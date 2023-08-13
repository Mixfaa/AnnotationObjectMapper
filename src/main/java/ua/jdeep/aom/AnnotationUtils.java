package ua.jdeep.aom;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;


final class AnnotationUtils {

    private AnnotationUtils() {}

    public static List<Method> getAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotation) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(annotation))
                .toList();
    }

    public static List<Field> getAnnotatedFields(Class<?> clazz, Class<? extends Annotation> annotation) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(method -> method.isAnnotationPresent(annotation))
                .toList();
    }
}
