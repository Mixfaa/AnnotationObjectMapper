package ua.jdeep.aom;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import ua.jdeep.aom.annotations.MapAnnotationProperty;
import ua.jdeep.aom.annotations.MapAnnotationPropertyForClass;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public final class AnnotationUtils {

    private AnnotationUtils() {
    }

    public static Object getAnnotationProp(MapAnnotationProperty mapAnnotationProperty, Class<?> sourceClass) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Class<? extends Annotation> targetAnnotation = mapAnnotationProperty.targetAnnotation();
        Method annotationPropMethod = targetAnnotation.getDeclaredMethod(mapAnnotationProperty.annotationPropName());

        annotationPropMethod.trySetAccessible();

        return annotationPropMethod.invoke(sourceClass.getAnnotation(targetAnnotation));

    }

    public static Object getAnnotationPropFromClass(MapAnnotationPropertyForClass mapAnnotationProperty) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Class<? extends Annotation> targetAnnotation = mapAnnotationProperty.targetAnnotation();
        Method annotationPropMethod = targetAnnotation.getDeclaredMethod(mapAnnotationProperty.annotationPropName());

        annotationPropMethod.trySetAccessible();

        return annotationPropMethod.invoke(mapAnnotationProperty.sourceClass().getAnnotation(targetAnnotation));

    }

    public static List<Field> findFields(Class<?> classToScan, ElementMatcher<? super FieldDescription> fieldDescription) {
        return Arrays.stream(classToScan.getDeclaredFields()).filter(field ->
                fieldDescription.matches(new FieldDescription.ForLoadedField(field))).collect(Collectors.toList());
    }

    public static List<Method> findMethods(Class<?> classToScan, ElementMatcher<? super MethodDescription> methodDescription) {

        return Arrays.stream(classToScan.getDeclaredMethods()).filter(method ->
                methodDescription.matches(new MethodDescription.ForLoadedMethod(method))).collect(Collectors.toList());
    }

    public static List<Method> getAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotation) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(annotation))
                .collect(Collectors.toList());
    }

    public static List<Field> getAnnotatedFields(Class<?> clazz, Class<? extends Annotation> annotation) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(method -> method.isAnnotationPresent(annotation))
                .collect(Collectors.toList());
    }

}
