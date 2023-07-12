package mm4j.annotation;

import java.lang.annotation.*;

@Repeatable(Mappings.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Mapping {
    String mapFrom();

    String mapTo();

    boolean caseSensitive() default true;
}
