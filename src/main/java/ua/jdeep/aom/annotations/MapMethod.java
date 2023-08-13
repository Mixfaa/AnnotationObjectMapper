package ua.jdeep.aom.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface MapMethod {
    Class<? extends Annotation> annotatedWith();
}
