package ua.jdeep.aom.advices;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import ua.jdeep.aom.AOMConstants;

import java.lang.annotation.Annotation;


public class AnnotationAccessAdvice {


    @RuntimeType
    @Advice.OnMethodEnter
    public static Object getAnnotation(@FieldValue(AOMConstants.TARGET_OBJECT_FIELD_NAME) Object object, @Argument(0) Class<? extends Annotation> annotationClass) {
        return object.getClass().getAnnotation(annotationClass);
    }
}