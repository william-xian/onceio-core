package top.onceio.core.util;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.*;

public final class OUtils {

    public final static Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setLongSerializationPolicy(LongSerializationPolicy.STRING).registerTypeAdapter(Long.TYPE, new DateDeserializer()).disableHtmlEscaping().create();
    private final static Gson prettyGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setLongSerializationPolicy(LongSerializationPolicy.STRING).registerTypeAdapter(Long.TYPE, new DateDeserializer()).disableHtmlEscaping().setPrettyPrinting().create();

    public static String encodeMD5(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 计算md5函数
            md.update(str.getBytes());
            return new BigInteger(1, md.digest()).toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return str;
        }
    }

    public static String randomUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * getStub("?",",",5)
     *
     * @param e   元素
     * @param s   分割符号
     * @param cnt 个数
     * @return 打桩字符串
     */
    public static String genStub(String e, String s, int cnt) {
        StringBuffer sb = new StringBuffer((e.length() + s.length()) * cnt);
        for (int i = 0; i < cnt - 1; i++) {
            sb.append(e);
            sb.append(s);
        }
        if (cnt > 0) {
            sb.append(e);
        }
        return sb.toString();
    }

    public static String replaceWord(String str, Map<String, String> tokens) {
        String patternString = "(\\b" + String.join("\\b|\\b", tokens.keySet()) + "\\b)";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(str);
        // 两个方法：appendReplacement, appendTail
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, tokens.get(matcher.group(1)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static Map<String, List<String>> parseURLParam(String uri) {
        Map<String, List<String>> map = new HashMap<>();
        if (uri != null) {
            int sp = uri.indexOf('.');
            if (sp >= 0) {
                uri = uri.substring(sp + 1);
            }
            String[] fields = uri.split("&");
            for (String f : fields) {
                sp = f.indexOf('=');
                if (sp > 0) {
                    String key = f.substring(0, sp);
                    String val = f.substring(sp + 1);
                    List<String> vals = map.get(key);
                    if (vals == null) {
                        vals = new ArrayList<>();
                        map.put(key, vals);
                    }
                    vals.add(val);
                    map.put(key, vals);
                }
            }
        }
        return map;
    }

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    public static String toPrettyJson(Object obj) {
        return prettyGson.toJson(obj);
    }

    public static <T> T createFromJson(String json, Class<T> clazz) throws RuntimeException {
        return gson.fromJson(json, clazz);
    }

    public static Object trans(Object val, Class<?> type) {
        if (val == null) {
            return null;
        }
        if (type.equals(String.class)) {
            return val.toString();
        } else if (type.equals(int.class) || type.equals(Integer.class)) {
            return Integer.valueOf(val.toString());
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            return Long.valueOf(val.toString());
        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            return Boolean.valueOf(val.toString());
        } else if (type.equals(byte.class) || type.equals(Byte.class)) {
            return Byte.valueOf(val.toString());
        } else if (type.equals(short.class) || type.equals(Short.class)) {
            return Short.valueOf(val.toString());
        } else if (type.equals(double.class) || type.equals(Double.class)) {
            return Double.valueOf(val.toString());
        } else if (type.equals(float.class) || type.equals(Float.class)) {
            return Float.valueOf(val.toString());
        } else if (type.equals(BigDecimal.class)) {
            return new BigDecimal(val.toString());
        } else if (type.equals(Timestamp.class)) {
            return new Timestamp(Long.valueOf(val.toString()));
        } else if (type.equals(Date.class)) {
            return new Date(Long.valueOf(val.toString()));
        } else {
            return null;
        }
    }
}
class DateDeserializer implements JsonDeserializer<Long> {
    public Long deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return json.getAsJsonPrimitive().getAsLong();
    }
}