package ua.jdeep.aom.annotations;

import java.lang.annotation.*;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MapAnnotationProperty {
    Class<? extends Annotation> targetAnnotation();
    Class<?> sourceClass();
    String annotationPropName();
}
