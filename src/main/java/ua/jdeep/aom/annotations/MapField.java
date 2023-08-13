package ua.jdeep.aom.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MapField {
    Class<? extends Annotation> annotatedWith();
}
