package top.onceio.core.annotation;

public @interface Validate {
    boolean nullable() default true;

    String pattern() default "";

    Class<?> ref() default void.class;
}
