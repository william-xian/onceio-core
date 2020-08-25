package top.onceio.core.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import top.onceio.core.exception.Failed;

public class FieldPathPicker {

    List<Tuple3<Field, String, Integer>> fields = new ArrayList<>();

    /**
     * 根据路径将clazz类型的内置方法需要的反射域保存下来。
     *
     * @param clazz 类型
     * @param path  如果是数组用方括号包起来
     *              如 [0].cars[1].name
     */

    public FieldPathPicker(Class<?> clazz, String path) {
        int first = path.indexOf('.');
        String[] p = null;
        if (first >= 0) {
            p = path.split("\\.");
        } else {
            p = new String[]{path};
        }
        try {
            for (int i = 0; i < p.length; i++) {
                String fieldName = p[i];
                int leftIndex = fieldName.indexOf('[');
                int rightIndex = fieldName.lastIndexOf(']');
                if (leftIndex >= 0 && rightIndex > leftIndex) {
                    String fn = fieldName.substring(0, leftIndex);
                    String idx = fieldName.substring(leftIndex + 1, rightIndex);
                    Field field = null;
                    if (!fn.isEmpty()) {
                        field = getClassField(clazz, fn);
                        field.setAccessible(true);
                        clazz = field.getType();
                    }
                    fields.add(new Tuple3<Field, String, Integer>(field, null, Integer.parseInt(idx)));
                } else if (leftIndex < 0 && rightIndex < 0) {
                    if (Map.class.isAssignableFrom(clazz)) {
                        fields.add(new Tuple3<Field, String, Integer>(null, fieldName, null));
                    } else {
                        Field field = getClassField(clazz, fieldName);
                        field.setAccessible(true);
                        clazz = field.getType();
                        fields.add(new Tuple3<Field, String, Integer>(field, null, null));
                    }
                } else {
                    Failed.throwError("%s 不合法", path);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Failed.throwError(e.getMessage());
        }
    }

    private static Field getClassField(Class<?> clazz, String name) {
        do {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException | SecurityException e) {
                e.printStackTrace();
            }
            clazz = clazz.getSuperclass();
        } while (!clazz.equals(Object.class));
        return null;
    }

    @SuppressWarnings("unchecked")
    public Object getField(Object obj) {
        boolean found = true;
        for (Tuple3<Field, String, Integer> fieldPath : fields) {
            Field field = fieldPath.a;
            String fieldName = fieldPath.b;
            Integer idx = fieldPath.c;
            try {
                if (obj != null) {
                    if (field != null) {
                        obj = field.get(obj);
                    } else if (fieldName != null) {
                        obj = ((Map<String, Object>) obj).get(fieldName);
                    }
                    if (idx != null) {
                        if (idx >= 0) {
                            if (obj instanceof Object[]) {
                                obj = ((Object[]) obj)[idx];
                            } else if (obj instanceof List) {
                                obj = ((List<Object>) obj).get(idx);
                            } else if (obj instanceof int[]) {
                                obj = ((int[]) obj)[idx];
                            } else if (obj instanceof short[]) {
                                obj = ((short[]) obj)[idx];
                            } else if (obj instanceof char[]) {
                                obj = ((char[]) obj)[idx];
                            } else if (obj instanceof byte[]) {
                                obj = ((byte[]) obj)[idx];
                            } else if (obj instanceof float[]) {
                                obj = ((float[]) obj)[idx];
                            } else if (obj instanceof boolean[]) {
                                obj = ((boolean[]) obj)[idx];
                            } else if (obj instanceof double[]) {
                                obj = ((double[]) obj)[idx];
                            } else if (obj instanceof long[]) {
                                obj = ((long[]) obj)[idx];
                            }
                        }
                    }
                } else {
                    found = false;
                    break;
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                found = false;
            }
        }
        if (found) {
            return obj;
        } else {
            return null;
        }
    }

}