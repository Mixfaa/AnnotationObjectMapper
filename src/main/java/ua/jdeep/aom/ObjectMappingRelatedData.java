package ua.jdeep.aom;

public class ObjectMappingRelatedData<T> {
    Object source;
    Object target;
    Class<?> sourceClass;
    Class<T> targetClass;
    int targetClassModifiers;

    public ObjectMappingRelatedData(Object source, Class<T> targetClass) {
        this.source = source;
        this.sourceClass = source.getClass();
        this.targetClass = targetClass;
        this.targetClassModifiers = targetClass.getModifiers();
    }
}
