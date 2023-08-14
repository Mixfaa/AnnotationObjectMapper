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
import ua.jdeep.aom.advices.AnnotationAccessAdvice;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


public final class AnnotationObjectMapper {
    private static final Map<Class<?>, Class<?>> sourceTargetClassMap = new HashMap<>();
    private static final AnnotationObjectMapper instance = new AnnotationObjectMapper();
    private static final Logger logger = Logger.getLogger(AnnotationObjectMapper.class.getName());

    static {
        try {
            substituteAnnotationsPropsToStaticFields();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AnnotationObjectMapper() {
    }


    private static void substituteAnnotationsPropsToStaticFields() {
        Iterable<Class<?>> targetClasses = ClassIndex.getAnnotated(MappingTarget.class);

        for (Class<?> targetClass : targetClasses) {

            List<Field> mappingToPropFields = AOMUtil.findField(targetClass,
                    ElementMatchers.isAnnotatedWith(MapAnnotationProperty.class)
                            .and(ElementMatchers.isStatic()));

            for (Field mappingToPropField : mappingToPropFields) {

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

    private static <T> DynamicType.Builder<T> implementMethodMappingMethods(ObjectMappingRelatedData<T> data, DynamicType.Builder<T> classBuilder) throws AmbiguousDefinitionException {

        List<Method> methodMappings = AOMUtil.getAnnotatedMethods(data.targetClass, MapMethod.class);

        for (Method targetClassAnnotatedMethod : methodMappings) {

            MapMethod mapMethod = targetClassAnnotatedMethod.getAnnotation(MapMethod.class);

            List<Method> annotatedMethods = AOMUtil.findMethod(data.sourceClass,
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

    private static <T> DynamicType.Builder<T> implementFieldMappingMethods(ObjectMappingRelatedData<T> data, DynamicType.Builder<T> classBuilder) throws AmbiguousDefinitionException {
        List<Method> fieldAccessMethods = AOMUtil.getAnnotatedMethods(data.targetClass, MapField.class);

        for (Method fieldAccessMethod : fieldAccessMethods) {

            MapField mapField = fieldAccessMethod.getAnnotation(MapField.class);

            List<Field> sourceClassAnnotatedFields = AOMUtil.findField(data.sourceClass,
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

    private static <T> DynamicType.Builder<T> implementGetAnnotationMethod(ObjectMappingRelatedData<T> data, DynamicType.Builder<T> classBuilder) {
        List<Method> getAnnotationMethods = AOMUtil.findMethod(data.targetClass,
                ElementMatchers.named("getAnnotation")
                        .and(ElementMatchers.returns(Annotation.class))
                        .and(ElementMatchers.takesArguments(Class.class))
                        .and(ElementMatchers.not(ElementMatchers.isAnnotatedWith(ElementMatchers.any()))));

        if (getAnnotationMethods.size() != 1) {
            logger.warning("More or less than 1 methods match for getAnnotationMethod in " + data.sourceClass);
            return classBuilder;
        }

        Method getAnnotationMethod = getAnnotationMethods.get(0);

        return classBuilder.method(ElementMatchers.is(getAnnotationMethod))
                .intercept(MethodDelegation.to(AnnotationAccessAdvice.class));
    }

    /**
     * Call it at application startup to load {@link AnnotationObjectMapper} class to memory, it will cause static block call
     */
    public static void initialize() {

    }

    public static AnnotationObjectMapper getInstance() {
        return instance;
    }

    /**
     * @param source      Source object that needs to be mapped (object, which class is annotated with your annotations)
     * @param targetClass Class to which source object needs to be mapped (abstract class, annotated with annotations from {@link ua.jdeep.aom.annotations}
     * @param <T>         Target class type, annotated with {@link MappingTarget}
     * @return Instance of targetClass, with methods, mapped to source object
     */
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

        classBuilder = implementGetAnnotationMethod(objectMappingRelatedData, classBuilder);

        try (DynamicType.Unloaded<T> unloadedClass = classBuilder.make()) {
            Class<? extends T> loadedClass = unloadedClass.load(targetClass.getClassLoader()).getLoaded();
            sourceTargetClassMap.put(sourceClass, loadedClass);

            return loadedClass.getConstructor(Object.class).newInstance(source);
        } catch (IOException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param beans       iterable object, with objects which should be mapped to targetClass
     * @param targetClass Class to which source object needs to be mapped (abstract class, annotated with annotations from {@link ua.jdeep.aom.annotations}
     * @param <T>         Target class type, annotated with {@link MappingTarget}
     * @return List, containing mapped objects
     * @see #mapObjectTo(Object, Class)
     */
    public <T> List<T> mapObjectsTo(Iterable<Object> beans, Class<T> targetClass) {

        return StreamSupport.stream(beans.spliterator(), false).map(bean -> {
            try {
                return mapObjectTo(bean, targetClass);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }
}
