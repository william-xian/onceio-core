package top.onceio.core.db.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({TYPE, METHOD})
@Retention(RUNTIME)
public @interface DefSQL {
    /**
     * 启动执行一次的SQL；
     *
     * @return 执行sql语句
     */
    String[] value() default {};
}
