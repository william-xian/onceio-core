package top.onceio.core.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ByteArraySerializer;

public final class OUtils {

	  public static ObjectMapper mapper = new ObjectMapper();
	  public static ObjectMapper prettyMapper = new ObjectMapper();

	  static {
	    // Non-standard JSON but we allow C style comments in our JSON
	    mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
	    prettyMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
	    prettyMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

	    SimpleModule module = new SimpleModule();
	    // custom types
	    module.addSerializer(byte[].class, new ByteArraySerializer());

	    mapper.registerModule(module);
	    prettyMapper.registerModule(module);
	  }

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

	public static String toJson(Object obj) throws RuntimeException{
		 try {
		      return mapper.writeValueAsString(obj);
	    } catch (Exception e) {
	      throw new RuntimeException("Failed to encode as JSON: " + e.getMessage());
	    }
	}

	public static String toPrettyJson(Object obj) throws RuntimeException {
		try {
			return prettyMapper.writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException("Failed to encode as JSON: " + e.getMessage());
		}
	}

	public static <T> T createFromJson(String json, Class<T> clazz) throws RuntimeException {
		try {
			return mapper.readValue(json, clazz);
		} catch (Exception e) {
			throw new RuntimeException("Failed to decode: " + e.getMessage());
		}
	}

}
