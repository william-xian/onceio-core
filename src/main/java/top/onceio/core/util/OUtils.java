package top.onceio.core.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class OUtils {

	public final static Gson gson = new GsonBuilder().create();
	private final static Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
	
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
					String key = f.substring(0,sp);
					String val = f.substring(sp+1);
					List<String> vals = map.get(key);
					if(vals == null) {
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

}
