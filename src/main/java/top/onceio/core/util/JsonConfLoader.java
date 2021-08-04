package top.onceio.core.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.onceio.OnceIO;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JsonConfLoader {
    private final static Logger LOGGER = LoggerFactory.getLogger(JsonConfLoader.class);

    private JsonObject conf = new JsonObject();
    private JsonObject beans = new JsonObject();

    public JsonObject getConf() {
        return conf;
    }

    public JsonObject getBeans() {
        return beans;
    }

    public void loadConf(String... dirs) {
        for (String dir : dirs) {
            load(dir);
        }
    }

    private void loadJar(URL url) {
        LOGGER.debug("loading: .jar!"+ url);
        String path = url.getFile();
        int sp = path.indexOf(".jar!");
        String jarpath = path.substring(0, sp) + ".jar";
        jarpath = OnceIO.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String dir = path.substring(sp + 6);
        JarFile localJarFile = null;
        try {
            localJarFile = new JarFile(jarpath);
            Enumeration<JarEntry> entries = localJarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                String innerPath = jarEntry.getName();
                if (innerPath.startsWith(dir) && innerPath.endsWith(".json")) {
                    InputStream inputStream = OnceIO.getClassLoader().getResourceAsStream(innerPath);
                    LOGGER.debug("loading innerPath:"+ innerPath);
                    loadJson(inputStream);
                    inputStream.close();
                }
            }
        } catch (IOException e) {
            LOGGER.warn(e.getMessage());
        } finally {
            if (localJarFile != null) {
                try {
                    localJarFile.close();
                } catch (IOException e) {
                    LOGGER.warn(e.getMessage());
                }
            }
        }
    }

    public void loadJson(InputStream inputStream) {
        try {
            JsonReader reader = OUtils.gson.newJsonReader(new InputStreamReader(inputStream));
            JsonObject jn = OUtils.gson.fromJson(reader, JsonObject.class);
            jn.entrySet().forEach((arg) -> {
                        if ("beans".equals(arg.getKey())) {
                            arg.getValue().getAsJsonObject().entrySet().forEach((bean) -> {
                                        beans.add(bean.getKey(), bean.getValue());
                                    }
                            );
                        } else {
                            conf.add(arg.getKey(), arg.getValue());
                        }
                    }
            );
            reader.close();
        } catch (IOException e) {
            LOGGER.warn(e.getMessage());
        }
    }

    public void load(String dir) {
        LOGGER.debug("loading: "+ dir);
        URL url = OnceIO.getClassLoader().getResource(dir);
        if (url != null) {
            if (url.getPath().contains(".jar!")) {
                loadJar(url);
            } else {
                File file = new File(url.getFile());
                if (file.exists() && file.isDirectory()) {
                    loadDir(file);
                }else  {
                    loadJSONFile(file);
                }
            }
        } else {
            File file = new File(dir);
            if (file.exists() && file.isDirectory()) {
                loadDir(file);
            } else  {
                loadJSONFile(file);
            }
        }

        LOGGER.debug("loaded: "+ dir);
    }

    private void loadDir(File dir) {
        LOGGER.debug("loading: dir:"+ dir.getName());
        try {
            Files.walk(dir.toPath(), FileVisitOption.FOLLOW_LINKS).forEach(path -> {
                File cnf = path.toFile();
                loadJSONFile(cnf);
            });
        } catch (IOException e) {
            LOGGER.warn(e.getMessage());
        }
    }

    private void loadJSONFile(File cnf) {
        LOGGER.debug("loadingJSONFile: "+ cnf.getName());
        if (cnf.getName().endsWith(".json")) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(cnf);
                loadJson(fis);
            } catch (FileNotFoundException e) {
                LOGGER.warn(e.getMessage());
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        LOGGER.warn(e.getMessage());
                    }
                }
            }
        }
    }

    public Map<String, Object> resolveBeans() {
        Map<String, Object> name2Bean = new HashMap<>();

        /** 初始分配内存 */
        beans.entrySet().forEach((t) -> {
                    JsonObject clsFields = t.getValue().getAsJsonObject();
                    JsonElement type = clsFields.get("@TYPE");
                    String clsName = (type != null) ? type.getAsString() : t.getKey();
                    try {
                        Class<?> cls = OnceIO.getClassLoader().loadClass(clsName);
                        Object bean = cls.newInstance();
                        name2Bean.put(t.getKey(), bean);
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                        LOGGER.error(e.getMessage());
                        e.printStackTrace();
                    }
                }
        );
        beans.entrySet().forEach((Entry<String, JsonElement> t) -> {
                    JsonObject clsFields = t.getValue().getAsJsonObject();
                    Object bean = name2Bean.get(t.getKey());
                    if (bean != null) {
                        Class<?> cls = bean.getClass();
                        clsFields.entrySet().forEach((Entry<String, JsonElement> fieldType) -> {
                                    if (fieldType.getKey().equals("@TYPE")) {
                                        return;
                                    }
                                    try {
                                        Method method = OReflectUtil.getSetMethod(cls, fieldType.getKey());
                                        if (method != null) {
                                            String strV = fieldType.getValue().getAsString();
                                            if (strV != null) {
                                                if (strV.startsWith("@")) {
                                                    method.invoke(bean, name2Bean.get(strV.substring(1)));
                                                } else {
                                                    method.invoke(bean,
                                                            OReflectUtil.strToBaseType(method.getParameterTypes()[0], strV));
                                                }
                                            } else {
                                                method.invoke(bean,
                                                        OReflectUtil.strToBaseType(method.getParameterTypes()[0], strV));
                                            }
                                        } else {
                                            LOGGER.warn("{} has no field: {}", cls.getName(), fieldType.getKey());
                                        }
                                    } catch (IllegalArgumentException | IllegalAccessException | SecurityException
                                            | InvocationTargetException e) {
                                        e.printStackTrace();
                                        LOGGER.error(e.getMessage());
                                    }
                                }

                        );
                    } else {
                        LOGGER.error("创建Bean: {}失败。", t.getKey());
                    }

                }
        );
        return name2Bean;
    }

}
