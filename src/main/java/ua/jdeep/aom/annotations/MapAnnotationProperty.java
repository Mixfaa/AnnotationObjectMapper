package ua.jdeep.aom.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MapAnnotationProperty {
    Class<? extends Annotation> targetAnnotation();

    String annotationPropName();
}
