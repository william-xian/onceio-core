package top.onceio.core.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Aop {

    /**
     * @return 被拦截的方法pattern
     */
    String[] value() default {};

    String[] pattern() default {};

    String order() default "";

}
