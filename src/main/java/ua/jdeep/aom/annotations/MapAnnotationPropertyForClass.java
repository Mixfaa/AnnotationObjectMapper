package ua.jdeep.aom.annotations;

import java.lang.annotation.*;

/**
 * Maps annotation property to static field.
 * {@link #targetAnnotation()}, annotation which will be scanned for method with name given by {@link #annotationPropName()}.
 * {@link #sourceClass()}, class which will be scanned for annotation given by {@link #targetAnnotation()}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MapAnnotationPropertyForClass {
    Class<? extends Annotation> targetAnnotation();

    Class<?> sourceClass();

    String annotationPropName();
}
