package top.onceio.core.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class OUtils {

	public static String encodeMD5(String str) {
		try {
			// 生成一个MD5加密计算摘要
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

	private static final Gson GSON = new GsonBuilder().serializeNulls().create();

	public static String toJSON(Object obj) {
		return GSON.toJson(obj);
	}

	public static <T> T createFromJson(String json, Class<T> entityClass) {
		return GSON.fromJson(json, entityClass);
	}

}
