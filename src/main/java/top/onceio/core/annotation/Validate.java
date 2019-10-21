package top.onceio.core.annotation;

public @interface Validate {
    boolean nullable() default true;

    String pattern() default "";

    Class<?> valRef() default void.class;
}
