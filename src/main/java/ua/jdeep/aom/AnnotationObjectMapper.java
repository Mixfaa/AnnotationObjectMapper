package ua.jdeep.aom;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatchers;
import org.atteo.classindex.ClassIndex;
import ua.jdeep.aom.advices.FieldAccessAdvice;
import ua.jdeep.aom.annotations.MapAnnotationProperty;
import ua.jdeep.aom.annotations.MapField;
import ua.jdeep.aom.annotations.MapMethod;
import ua.jdeep.aom.annotations.MappingTarget;
import ua.jdeep.aom.exceptions.AmbiguousDefinitionException;
import ua.jdeep.aom.exceptions.TypesNotMatchException;
import ua.jdeep.aom.exceptions.UnsuitableModifiersException;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AnnotationObjectMapper {
    private static final Map<Class<?>, Class<?>> sourceTargetClassMap = new HashMap<>();
    private static final AnnotationObjectMapper instance = new AnnotationObjectMapper();

    static {
        try {
            substituteAnnotationsPropsToStaticFields();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AnnotationObjectMapper() {

    }

    private static void substituteAnnotationsPropsToStaticFields() throws UnsuitableModifiersException {
        Iterable<Class<?>> targetClasses = ClassIndex.getAnnotated(MappingTarget.class);

        for (Class<?> targetClass : targetClasses) {

            List<Field> mappingToPropFields = AnnotationUtils.getAnnotatedFields(targetClass, MapAnnotationProperty.class);

            for (Field mappingToPropField : mappingToPropFields) {

                if (!Modifier.isStatic(mappingToPropField.getModifiers()))
                    throw new UnsuitableModifiersException("Field mapped to annotation parameter must be static");

                MapAnnotationProperty mapAnnotationProperty = mappingToPropField.getAnnotation(MapAnnotationProperty.class);

                try {
                    Class<? extends Annotation> targetAnnotation = mapAnnotationProperty.targetAnnotation();
                    Method annotationPropMethod = targetAnnotation.getDeclaredMethod(mapAnnotationProperty.annotationPropName());

                    annotationPropMethod.trySetAccessible();

                    var propValue = annotationPropMethod.invoke(mapAnnotationProperty.sourceClass().getAnnotation(targetAnnotation));

                    mappingToPropField.set(null, propValue);
                } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }

    private static <T> DynamicType.Builder<T> implementMethodMappingMethods(ObjectMappingRelatedData<T> data, DynamicType.Builder<T> classBuilder) throws AmbiguousDefinitionException, TypesNotMatchException, UnsuitableModifiersException {

        List<Method> methodMappings = AnnotationUtils.getAnnotatedMethods(data.targetClass, MapMethod.class);

        for (Method targetClassAnnotatedMethod : methodMappings) {

            MapMethod mapMethod = targetClassAnnotatedMethod.getAnnotation(MapMethod.class);

            List<Method> annotatedMethods = AnnotationUtils.getAnnotatedMethods(data.sourceClass, mapMethod.annotatedWith());

            if (annotatedMethods.size() != 1)
                throw new AmbiguousDefinitionException("More or less than 1 method annotated with " + mapMethod.annotatedWith());

            Method sourceClassMethod = annotatedMethods.get(0);

            if (!Modifier.isPublic(sourceClassMethod.getModifiers()))
                throw new UnsuitableModifiersException("Source class method is not public, can't access it");

            if (sourceClassMethod.getReturnType() != targetClassAnnotatedMethod.getReturnType())
                throw new TypesNotMatchException("Target method return type is not matching with source class method return type, method annotated with  " + mapMethod.annotatedWith());

            if (!Arrays.equals(sourceClassMethod.getParameterTypes(), targetClassAnnotatedMethod.getParameterTypes()))
                throw new TypesNotMatchException("Target method parameter types is not matching with source class method parameter types, method annotated with  " + mapMethod.annotatedWith());

            classBuilder = classBuilder.method(
                    ElementMatchers.is(targetClassAnnotatedMethod)
                            .and(ElementMatchers.isDeclaredBy(data.targetClass))
                            .and(ElementMatchers.returns(targetClassAnnotatedMethod.getReturnType()))
            ).intercept(MethodCall.invoke(sourceClassMethod).onField(AOMConstants.TARGET_OBJECT_FIELD_NAME).withAllArguments().withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));
        }

        return classBuilder;
    }

    private static <T> DynamicType.Builder<T> implementFieldMappingMethods(ObjectMappingRelatedData<T> data, DynamicType.Builder<T> classBuilder) throws AmbiguousDefinitionException, TypesNotMatchException {
        List<Method> fieldAccessMethods = AnnotationUtils.getAnnotatedMethods(data.targetClass, MapField.class);

        for (Method fieldAccessMethod : fieldAccessMethods) {

            MapField mapField = fieldAccessMethod.getAnnotation(MapField.class);

            List<Field> sourceClassAnnotatedFields = AnnotationUtils.getAnnotatedFields(data.sourceClass, mapField.annotatedWith());

            if (sourceClassAnnotatedFields.size() != 1)
                throw new AmbiguousDefinitionException("More or less than 1 field annotated with " + mapField.annotatedWith());

            Field sourceClassField = sourceClassAnnotatedFields.get(0);

            sourceClassField.setAccessible(true);

            if (sourceClassField.getType() != fieldAccessMethod.getReturnType())
                throw new TypesNotMatchException("Source class field type is not matching with method return type in target class");

            classBuilder = classBuilder.method(
                    ElementMatchers.is(fieldAccessMethod)
                            .and(ElementMatchers.isDeclaredBy(data.targetClass))
                            .and(ElementMatchers.returns(sourceClassField.getType()))
            ).intercept(MethodDelegation.to(new FieldAccessAdvice.Getter(sourceClassField)));
        }
        return classBuilder;
    }

    // made only to load class to memory call and call static block
    public static void initialize() {

    }

    public static AnnotationObjectMapper getInstance() {
        return instance;
    }

    public <T> T mapObjectTo(Object source, Class<T> targetClass) throws AmbiguousDefinitionException, UnsuitableModifiersException, TypesNotMatchException {
        Class<?> sourceClass = source.getClass();
        Class<?> cachedTargetClass = sourceTargetClassMap.get(sourceClass);

        if (cachedTargetClass != null) {
            try {
                return (T) cachedTargetClass.getConstructor(Object.class).newInstance(source);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        DynamicType.Builder<T> classBuilder;

        int targetClassModifiers = targetClass.getModifiers();
        try {

            if (!Modifier.isAbstract(targetClassModifiers) || Modifier.isInterface(targetClassModifiers))
                throw new UnsuitableModifiersException("Target class must be abstract class");

            classBuilder = new ByteBuddy()
                    .subclass(targetClass)
                    .defineField(AOMConstants.TARGET_OBJECT_FIELD_NAME, Object.class, Visibility.PRIVATE, FieldManifestation.FINAL)
                    .defineConstructor(Visibility.PUBLIC)
                    .withParameters(Object.class)
                    .intercept(MethodCall.invoke(targetClass.getDeclaredConstructor())
                            .andThen(FieldAccessor.ofField(AOMConstants.TARGET_OBJECT_FIELD_NAME).setsArgumentAt(0)));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        ObjectMappingRelatedData<T> objectMappingRelatedData = new ObjectMappingRelatedData<>(source, targetClass);

        classBuilder = implementMethodMappingMethods(objectMappingRelatedData, classBuilder);

        classBuilder = implementFieldMappingMethods(objectMappingRelatedData, classBuilder);

        try (DynamicType.Unloaded<T> unloadedBuiltClass = classBuilder.make()) {
            Class<? extends T> loadedBuildClass = unloadedBuiltClass.load(targetClass.getClassLoader()).getLoaded();
            sourceTargetClassMap.put(sourceClass, loadedBuildClass);

            return loadedBuildClass.getConstructor(Object.class).newInstance(source);
        } catch (IOException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> List<T> mapObjectsTo(List<Object> beans, Class<T> targetClass) {

        return beans.stream().map(bean -> {
            try {
                return mapObjectTo(bean, targetClass);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }
}
