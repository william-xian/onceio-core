package top.onceio.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 被标注的字段会被添加到i18n表中
 * oid : msg/lang_MD5（字段值）
 * name : 翻译的文字
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface I18nMsg {
	String value() default ""; /** lang */
}
