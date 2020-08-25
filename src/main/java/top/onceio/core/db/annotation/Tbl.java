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

    String name() default "";

    Index[] indexes() default {};

    /**
     * @return 扩展表
     */
    Class<?> extend() default void.class;
    /**
     * 根据扩展表【主键值相同】自动添加数值
     * @return 是否自动创建关联表
     */
    boolean autoCreate() default false;

    TblType type() default TblType.TABLE;

}
