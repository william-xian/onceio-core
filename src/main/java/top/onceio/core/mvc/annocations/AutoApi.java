package top.onceio.core.mvc.annocations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import top.onceio.core.db.tbl.BaseEntity;

/**
 * 类的所有公共方法都会成为RESTful API
 *
 * @author william-xian
 */

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface AutoApi {
    Class<? extends BaseEntity> value();

    String brief() default "";
}
