package top.onceio.core.db.annotation;

import top.onceio.core.db.tbl.OEntity;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface TblView {

    Class<? extends OEntity>[] depends() default {};

    String def() default "";
}
