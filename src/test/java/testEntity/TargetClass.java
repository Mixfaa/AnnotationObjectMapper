package testEntity;

import testAnnotation.*;
import ua.jdeep.aom.annotations.MapAnnotationPropertyForClass;
import ua.jdeep.aom.annotations.MapField;
import ua.jdeep.aom.annotations.MapMethod;
import ua.jdeep.aom.annotations.MappingTarget;

import java.lang.annotation.Annotation;

@MappingTarget
public abstract class TargetClass {

    @MapAnnotationPropertyForClass(annotationPropName = "stringValue", sourceClass = ExampleSourceClass.class, targetAnnotation = TestClassAnnotation.class)
    public static String stringValueProp;

    @MapAnnotationPropertyForClass(annotationPropName = "intValue", sourceClass = ExampleSourceClass.class, targetAnnotation = TestClassAnnotation2.class)
    public static int intValueProp;

    @MapMethod(annotatedWith = TestSumMethod.class)
    public abstract int sumMethod(int a, int b);

    @MapMethod(annotatedWith = TestGetHelloWorld.class)
    public abstract String getHelloWorldMethod();

    @MapField(annotatedWith = TestField.class)
    public abstract Double getField();

    public abstract <T extends Annotation> T getClassAnnotation(Class<T> annotationClass);
}
