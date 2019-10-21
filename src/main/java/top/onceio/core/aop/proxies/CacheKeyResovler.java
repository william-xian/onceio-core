package top.onceio.core.aop.proxies;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import top.onceio.core.util.FieldPathPicker;
import top.onceio.core.util.Tuple2;


public class CacheKeyResovler {

    private static Map<Method, Tuple2<String, List<FieldPathPicker>>> methodToPicker = new HashMap<>();

    public static String extractKey(Method method, String key, Object[] args) {
        Tuple2<String, List<FieldPathPicker>> tuple = methodToPicker.get(method);
        if (tuple == null) {
            tuple = new Tuple2<String, List<FieldPathPicker>>();
            tuple.b = new ArrayList<>();
            methodToPicker.put(method, tuple);
            StringBuilder sb = new StringBuilder();
            List<String> marks = new ArrayList<>();
            int b = 0;
            int e = -1;
            for (int i = 0; i < key.length(); i++) {
                if (key.charAt(i) == '$') {
                    b = i;
                    sb.append(key.substring(e + 1, b));
                } else if (key.charAt(i) == '}') {
                    e = i;
                    sb.append("%s");
                    marks.add(key.substring(b, e + 1));
                    String path = key.substring(b, e + 1);
                    tuple.b.add(new FieldPathPicker(args.getClass(), path));
                }
            }
            tuple.a = sb.toString();
        }

        List<Object> objs = new ArrayList<>(tuple.b.size());

        for (FieldPathPicker fpp : tuple.b) {
            objs.add(fpp.getField(args));
        }
        return String.format(tuple.a, objs.toArray());
    }

}