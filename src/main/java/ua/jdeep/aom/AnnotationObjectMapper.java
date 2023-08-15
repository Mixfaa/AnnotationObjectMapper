package ua.jdeep.aom;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import ua.jdeep.aom.annotations.MappingTarget;
import ua.jdeep.aom.exceptions.AmbiguousDefinitionException;
import ua.jdeep.aom.exceptions.TypesNotMatchException;
import ua.jdeep.aom.exceptions.UnsuitableModifiersException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


public final class AnnotationObjectMapper {
    private static final AnnotationObjectMapper instance = new AnnotationObjectMapper();
    private static final Logger logger = Logger.getLogger(AnnotationObjectMapper.class.getName());

    static {
        try {
            AnnotationsImplementer.substituteAnnotationsPropsToStaticFields();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final Map<Class<?>, Class<?>> sourceTargetClassMap = new HashMap<>();

    private AnnotationObjectMapper() {
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
    public <T> T mapObjectTo(Object source, Class<T> targetClass) throws
            AmbiguousDefinitionException, UnsuitableModifiersException, TypesNotMatchException {
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

        classBuilder = AnnotationsImplementer.implementMethodMappingMethods(objectMappingRelatedData, classBuilder);

        classBuilder = AnnotationsImplementer.implementFieldMappingMethods(objectMappingRelatedData, classBuilder);

        classBuilder = AnnotationsImplementer.implementGetAnnotationMethod(objectMappingRelatedData, classBuilder);

        classBuilder = AnnotationsImplementer.implementGetAnnotationPropMethods(objectMappingRelatedData, classBuilder);

        try (DynamicType.Unloaded<T> unloadedClass = classBuilder.make()) {
            Class<? extends T> loadedClass = unloadedClass.load(targetClass.getClassLoader()).getLoaded();
            sourceTargetClassMap.put(sourceClass, loadedClass);

            var targetObject = loadedClass.getConstructor(Object.class).newInstance(source);

            objectMappingRelatedData.target = targetObject;

            AnnotationsImplementer.setupFieldsMappedToAnnotationsProps(objectMappingRelatedData);

            return targetObject;

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
