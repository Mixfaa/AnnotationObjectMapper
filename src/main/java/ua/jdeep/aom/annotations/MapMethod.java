package ua.jdeep.aom.annotations;

import java.lang.annotation.*;

/**
 * {@link #annotatedWith()}, sets annotation class, with which method in source object should be annotated
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface MapMethod {
    Class<? extends Annotation> annotatedWith();
}
