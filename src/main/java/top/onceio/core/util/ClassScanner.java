package top.onceio.core.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import top.onceio.OnceIO;

public class ClassScanner {
    private static final ClassLoader classLoader = OnceIO.getClassLoader();// 默认使用的类加载器

    public static void findBy(Consumer<Class<?>> consumer, String... packages) {
        for (String pkg : packages) {
            URL url = classLoader.getResource(pkg.replace(".", "/"));
            String protocol = url.getProtocol();
            if ("file".equals(protocol)) {
                // 本地自己可见的代码
                findClassLocal(pkg, consumer);
            } else if ("jar".equals(protocol)) {
                // 引用jar包的代码
                findClassJar(pkg, consumer);
            }
        }
    }

    /**
     * 本地查找
     *
     * @param packName
     */
    private static void findClassLocal(final String packName, Consumer<Class<?>> consumer) {
        URI url = null;
        try {
            url = classLoader.getResource(packName.replace(".", "/")).toURI();
        } catch (URISyntaxException e1) {
            throw new RuntimeException("未找到策略资源");
        }
        File file = new File(url);
        file.listFiles(new FileFilter() {
            public boolean accept(File chiFile) {
                if (chiFile.isDirectory()) {
                    findClassLocal(packName + "." + chiFile.getName(), consumer);
                }
                if (chiFile.getName().endsWith(".class")) {
                    Class<?> clazz = null;
                    try {
                        clazz = classLoader.loadClass(packName + "." + chiFile.getName().replace(".class", ""));
                        consumer.accept(clazz);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                return false;
            }
        });

    }

    /**
     * jar包查找
     *
     * @param packName
     */
    private static void findClassJar(final String packName, Consumer<Class<?>> consumer) {
        String pathName = packName.replace(".", "/");
        JarFile jarFile = null;
        try {
            URL url = classLoader.getResource(pathName);
            JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
            jarFile = jarURLConnection.getJarFile();
        } catch (IOException e) {
            throw new RuntimeException("未找到策略资源");
        }

        Enumeration<JarEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = jarEntries.nextElement();
            String jarEntryName = jarEntry.getName();

            if (jarEntryName.contains(pathName) && !jarEntryName.equals(pathName + "/")) {
                // 递归遍历子目录
                if (jarEntry.isDirectory()) {
                    String clazzName = jarEntry.getName().replace("/", ".");
                    int endIndex = clazzName.lastIndexOf(".");
                    String prefix = null;
                    if (endIndex > 0) {
                        prefix = clazzName.substring(0, endIndex);
                    }
                    findClassJar(prefix, consumer);
                }
                if (jarEntry.getName().endsWith(".class")) {
                    Class<?> clazz = null;
                    try {
                        clazz = classLoader.loadClass(jarEntry.getName().replace("/", ".").replace(".class", ""));
                        consumer.accept(clazz);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }

        }

    }

}