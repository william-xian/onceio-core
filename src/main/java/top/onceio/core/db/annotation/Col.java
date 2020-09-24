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
     * @return 字符串长度
     */
    int size() default 255;

    /**
     * @return 字段名称
     */
    String name() default "";
    /**
     * @return 字段类型名称
     */
    String type() default "";
    /**
     * @return 字段默认值
     */
    String defaultValue() default "";

    /**
     * @return 字段描述
     */
    String comment() default "";
    /**
     * @return 是否包含唯一约束
     */
    boolean unique() default false;
    /**
     * @return 索引使用类型，默认tree
     */
    String using() default "";
    /**
     * @return 是否可为null约束
     */
    boolean nullable() default false;
    /**
     * @return 字段符合模式
     */
    String pattern() default "";

    /**
     * (Optional) The precision for a decimal (exact numeric) column. (Applies
     * only if a decimal column is used.) Value must be set by developer if used
     * when generating the DDL for the column.
     * @return 精度数值
     */
    int precision() default 0;

    /**
     * (Optional) The scale for a decimal (exact numeric) column. (Applies only
     * if a decimal column is used.)
     * @return 规模
     */
    int scale() default 0;

    /**
     *
     * 如果该类标注@Model则说明表示。
     * @return 引用的类
     */
    Class<?> ref() default void.class;

    /**
     * depends on ref    /
     * @return 是否使用外键
     */
    boolean useFK() default false;
}
