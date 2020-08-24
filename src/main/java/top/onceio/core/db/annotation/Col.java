package top.onceio.core.db.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.Callable;

@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface Col {

    /**
     * (Optional) The string-valued column length. (Applies only if a
     * string-valued column is used.) The decimal column scale for a decimal
     * (exact numeric) column
     */
    int size() default 255;

    String name() default "";

    String type() default "";

    String defaultValue() default "";

    String comment() default "";

    boolean unique() default false;

    String using() default "";

    boolean nullable() default true;

    String pattern() default "";

    /**
     * (Optional) The precision for a decimal (exact numeric) column. (Applies
     * only if a decimal column is used.) Value must be set by developer if used
     * when generating the DDL for the column.
     */
    int precision() default 0;

    /**
     * (Optional) The scale for a decimal (exact numeric) column. (Applies only
     * if a decimal column is used.)
     */
    int scale() default 0;

    /**
     * 该字段的值引用自某类字段
     *
     * @return 值引用类
     */
    Class<?> valRef() default void.class;

    Class<?> ref() default void.class;

    /**
     * depends on ref
     */
    boolean useFK() default true;

    /**
     * 引用的ID可能来源某个表。 注意枚举中的表都会被连接
     */
    Class<?>[] refEnum() default {};

    /**
     * 引用路径
     */
    String refBy() default "";
}
