package top.onceio.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 被标注的字段会被添加到i18n表中，
 * i18n.id=c/类名_字段名,
 * i18n.val = 字段的值
 * i18n.name = I18nCfgBrief.value() | I18nMsg.value()
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface I18nCfg {
    String value() default ""; //language
}
