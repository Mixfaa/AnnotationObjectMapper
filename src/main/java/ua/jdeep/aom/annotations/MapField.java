package ua.jdeep.aom.annotations;

import java.lang.annotation.*;

/**
 * {@link #annotatedWith()}, sets annotation class, with which field in source object should be annotated
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MapField {
    Class<? extends Annotation> annotatedWith();
}
