package top.onceio.core.db.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Target;

/**
 * 默认显示的字段
 */
@Target({ METHOD, FIELD })
public @interface Name {

}
