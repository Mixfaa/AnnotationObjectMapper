package ua.jdeep.aom;


import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatchers;
import org.atteo.classindex.ClassIndex;
import ua.jdeep.aom.advices.AnnotationAccessAdvice;
import ua.jdeep.aom.advices.FieldAccessAdvice;
import ua.jdeep.aom.annotations.*;
import ua.jdeep.aom.exceptions.AmbiguousDefinitionException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Logger;

public final class AnnotationsImplementer {
    private static final Logger logger = Logger.getLogger(AnnotationsImplementer.class.getName());

    private AnnotationsImplementer() {
    }

    public static void substituteAnnotationsPropsToStaticFields() {
        Iterable<Class<?>> targetClasses = ClassIndex.getAnnotated(MappingTarget.class);

        for (Class<?> targetClass : targetClasses) {

            List<Field> mappingToPropFields = AnnotationUtils.findFields(targetClass,
                    ElementMatchers.isAnnotatedWith(MapAnnotationPropertyForClass.class)
                            .and(ElementMatchers.isStatic()));

            for (Field mappingToPropField : mappingToPropFields) {

                MapAnnotationPropertyForClass mapAnnotationProperty = mappingToPropField.getAnnotation(MapAnnotationPropertyForClass.class);

                try {
                    var propValue = AnnotationUtils.getAnnotationPropFromClass(mapAnnotationProperty);

                    mappingToPropField.set(null, propValue);
                } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static <T> DynamicType.Builder<T> implementMethodMappingMethods(ObjectMappingRelatedData<T> data, DynamicType.Builder<T> classBuilder) throws AmbiguousDefinitionException {

        List<Method> methodMappings = AnnotationUtils.getAnnotatedMethods(data.targetClass, MapMethod.class);

        for (Method targetClassAnnotatedMethod : methodMappings) {

            MapMethod mapMethod = targetClassAnnotatedMethod.getAnnotation(MapMethod.class);

            List<Method> annotatedMethods = AnnotationUtils.findMethods(data.sourceClass,
                    ElementMatchers.isAnnotatedWith(mapMethod.annotatedWith())
                            .and(ElementMatchers.isPublic())
                            .and(ElementMatchers.returns(targetClassAnnotatedMethod.getReturnType()))
                            .and(ElementMatchers.takesArguments(targetClassAnnotatedMethod.getParameterTypes())));

            if (annotatedMethods.size() != 1)
                throw new AmbiguousDefinitionException("More or less than 1 method annotated with " + mapMethod.annotatedWith());

            Method sourceClassMethod = annotatedMethods.get(0);

            classBuilder = classBuilder.method(
                    ElementMatchers.is(targetClassAnnotatedMethod)
                            .and(ElementMatchers.isDeclaredBy(data.targetClass))
                            .and(ElementMatchers.returns(targetClassAnnotatedMethod.getReturnType()))
            ).intercept(MethodCall.invoke(sourceClassMethod).onField(AOMConstants.TARGET_OBJECT_FIELD_NAME).withAllArguments().withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));
        }

        return classBuilder;
    }

    public static <T> DynamicType.Builder<T> implementFieldMappingMethods(ObjectMappingRelatedData<T> data, DynamicType.Builder<T> classBuilder) throws AmbiguousDefinitionException {
        List<Method> fieldAccessMethods = AnnotationUtils.getAnnotatedMethods(data.targetClass, MapField.class);

        for (Method fieldAccessMethod : fieldAccessMethods) {

            MapField mapField = fieldAccessMethod.getAnnotation(MapField.class);

            List<Field> sourceClassAnnotatedFields = AnnotationUtils.findFields(data.sourceClass,
                    ElementMatchers.isAnnotatedWith(mapField.annotatedWith())
                            .and(ElementMatchers.fieldType(fieldAccessMethod.getReturnType())));

            if (sourceClassAnnotatedFields.size() != 1)
                throw new AmbiguousDefinitionException("More or less than 1 field annotated with " + mapField.annotatedWith());

            Field sourceClassField = sourceClassAnnotatedFields.get(0);

            sourceClassField.setAccessible(true);

            classBuilder = classBuilder.method(
                    ElementMatchers.is(fieldAccessMethod)
                            .and(ElementMatchers.isDeclaredBy(data.targetClass))
                            .and(ElementMatchers.returns(sourceClassField.getType()))
            ).intercept(MethodDelegation.to(new FieldAccessAdvice.Getter(sourceClassField)));
        }
        return classBuilder;
    }

    public static <T> DynamicType.Builder<T> implementGetAnnotationPropMethods(ObjectMappingRelatedData<T> data, DynamicType.Builder<T> classBuilder) {
        List<Method> annotationAccessMethods = AnnotationUtils.getAnnotatedMethods(data.targetClass, MapAnnotationProperty.class);

        for (Method annotationAccessMethod : annotationAccessMethods) {

            MapAnnotationProperty mapAnnotationProperty = annotationAccessMethod.getAnnotation(MapAnnotationProperty.class);

            try {
                var propValue = AnnotationUtils.getAnnotationProp(mapAnnotationProperty, data.sourceClass);

                classBuilder = classBuilder.method(ElementMatchers.is(annotationAccessMethod))
                        .intercept(FixedValue.value(propValue));

            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return classBuilder;
    }

    public static <T> DynamicType.Builder<T> implementGetAnnotationMethod(ObjectMappingRelatedData<T> data, DynamicType.Builder<T> classBuilder) {
        List<Method> getAnnotationMethods = AnnotationUtils.findMethods(data.targetClass,
                ElementMatchers.named("getClassAnnotation")
                        .and(ElementMatchers.returns(Annotation.class))
                        .and(ElementMatchers.takesArguments(Class.class))
                        .and(ElementMatchers.not(ElementMatchers.isAnnotatedWith(ElementMatchers.any()))));

        if (getAnnotationMethods.size() != 1) {
            logger.warning("More or less than 1 methods match for getClassAnnotation in " + data.sourceClass);
            return classBuilder;
        }

        Method getAnnotationMethod = getAnnotationMethods.get(0);

        return classBuilder.method(ElementMatchers.is(getAnnotationMethod))
                .intercept(MethodDelegation.to(AnnotationAccessAdvice.ClassAnnotation.class));
    }

    public static <T> void setupFieldsMappedToAnnotationsProps(ObjectMappingRelatedData<T> data) {
        if (data.target == null) throw new NullPointerException();

        List<Field> mappedFields = AnnotationUtils.findFields(data.targetClass,
                ElementMatchers.isAnnotatedWith(MapAnnotationProperty.class));

        for (Field mappedField : mappedFields) {

            MapAnnotationProperty mapAnnotationProperty = mappedField.getAnnotation(MapAnnotationProperty.class);

            try {
                var propValue = AnnotationUtils.getAnnotationProp(mapAnnotationProperty, data.sourceClass);

                mappedField.set(data.target, propValue);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

        }
    }
}
