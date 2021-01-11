package top.onceio.core.mvc.annocations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Param {
    /**
     * name
     */
    String value() default "";
    
    String name() default "";

    boolean nullable() default true;

    String pattern() default "";
}
