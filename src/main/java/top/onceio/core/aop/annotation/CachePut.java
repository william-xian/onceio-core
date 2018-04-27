package top.onceio.core.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface CachePut {
	
	String[] cacheNames() default {};

	/**
	 * 例如："${1.name}:${2}"
	 * 表示：第一个参数的name值和第二个参数的值最为主键，中间以冒号连接
	 */
	String key() default "";
}
