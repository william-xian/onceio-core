package top.onceio.core.db.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface Tbl {
	String schema() default "";

	Constraint[] constraints() default {};

	Class<?> extend() default void.class;

	/** 根据关联表自动创建 */
	boolean autoCreate() default false;
}
