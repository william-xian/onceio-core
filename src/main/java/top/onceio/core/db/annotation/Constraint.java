package top.onceio.core.db.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({})
@Retention(RUNTIME)
public @interface Constraint {
    String using() default "BTREE";

    ConstraintType type() default ConstraintType.UNIQUE;

    String[] colNames() default {};
}
