package top.onceio.core.util;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotationScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationScanner.class);

    private final Set<Class<?>> filter = new HashSet<>();

    public AnnotationScanner(Class<?>... annotation) {
        filter.addAll(Arrays.asList(annotation));
    }

    public Set<Class<?>> getFilter() {
        return filter;
    }

    private final Map<Class<?>, Set<Class<?>>> classifiedAnns = new HashMap<>();

    public void scanPackages(String... packages) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("scanning :" + String.join(",", packages));
        }
        ClassScanner.findBy((clazz) -> {
            for (Annotation a : clazz.getAnnotations()) {
                if (filter.contains(a.annotationType())) {
                    putClass(a.annotationType(), clazz);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(String.format("%s:%s", a.annotationType().getName(), clazz.getName()));
                    }
                }
            }
        }, packages);
    }

    public void putClass(Class<?> annotation, Class<?> clazz) {
        Set<Class<?>> clazzList = classifiedAnns.get(annotation);
        if (clazzList == null) {
            clazzList = new HashSet<>();
            classifiedAnns.put(annotation, clazzList);
        }
        clazzList.add(clazz);
    }

    public Set<Class<?>> getClasses(Class<?>... annotation) {
        Set<Class<?>> result = new HashSet<>();
        for (Class<?> ann : annotation) {
            Set<Class<?>> r = classifiedAnns.get(ann);
            if (r != null) {
                result.addAll(r);
            }
        }
        return result;
    }
}
