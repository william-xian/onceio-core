package top.onceio.core.db.tbl;

import top.onceio.core.annotation.I18nCfg;
import top.onceio.core.db.annotation.Col;
import top.onceio.core.db.annotation.Tbl;
import top.onceio.core.util.OUtils;

@Tbl
public class OI18n extends OEntity {
	@Col(size = 64, nullable = false)
	private String key;
	@Col(size = 255, nullable = false)
	private String name;
	@Col(size = 32, nullable = true)
	private String val;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVal() {
		return val;
	}

	public void setVal(String val) {
		this.val = val;
	}

	public String toString() {
		return OUtils.toJson(this);
	}

	public static String msgId(String lang, String format) {
		return "msg/" + lang + "_" + OUtils.encodeMD5(format);
	}

	public static String constId(String lang, Class<?> clazz, String fieldName) {
		I18nCfg group = clazz.getAnnotation(I18nCfg.class);
		return "const/" + group.value() + "_" + clazz.getSimpleName() + "_" + fieldName;
	}
}
