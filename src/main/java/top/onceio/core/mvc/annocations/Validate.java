package top.onceio.core.mvc.annocations;

/**
 * 该注解功能与@Col, @Param类似，填补自定义类的空白
 */
public @interface Validate {
    boolean nullable() default true;

    String pattern() default "";

    Class<?> ref() default void.class;
}
