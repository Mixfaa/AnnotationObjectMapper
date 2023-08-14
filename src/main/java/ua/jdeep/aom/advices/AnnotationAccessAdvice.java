package ua.jdeep.aom.advices;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import ua.jdeep.aom.AOMConstants;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class AnnotationAccessAdvice {

    public static class ClassAnnotation {
        @RuntimeType
        @Advice.OnMethodEnter
        public static Annotation getAnnotation(@FieldValue(AOMConstants.TARGET_OBJECT_FIELD_NAME) Object object, @Argument(0) Class<? extends Annotation> annotationClass) {
            return object.getClass().getAnnotation(annotationClass);
        }
    }

    public static class MethodAnnotation {
        @RuntimeType
        @Advice.OnMethodEnter
        public static List<Annotation> getMethodAnnotations(@FieldValue(AOMConstants.TARGET_OBJECT_FIELD_NAME) Object object, @Argument(0) Class<? extends Annotation> annotationClass) {
            return Arrays.stream(object.getClass().getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(annotationClass))
                    .map(method -> method.getAnnotation(annotationClass))
                    .collect(Collectors.toList());
        }
    }

    public static class FieldAnnotation {
        @RuntimeType
        @Advice.OnMethodEnter
        public static List<Annotation> getFieldAnnotation(@FieldValue(AOMConstants.TARGET_OBJECT_FIELD_NAME) Object object, @Argument(0) Class<? extends Annotation> annotationClass) {
            return Arrays.stream(object.getClass().getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(annotationClass))
                    .map(field -> field.getAnnotation(annotationClass))
                    .collect(Collectors.toList());
        }
    }
}