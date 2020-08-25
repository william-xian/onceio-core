package top.onceio.core.aop.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.sql.Connection;

@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface Transactional {
    /**
     * <code>Connection.TRANSACTION_READ_UNCOMMITTED</code>,
     * <code>Connection.TRANSACTION_READ_COMMITTED</code>,
     * <code>Connection.TRANSACTION_REPEATABLE_READ</code>, or
     * <code>Connection.TRANSACTION_SERIALIZABLE</code>.
     * @return 隔离级别
     */
    int isolation() default Connection.TRANSACTION_READ_COMMITTED;

    boolean readOnly() default false;

    int timeout() default -1; // (in seconds granularity)
}
