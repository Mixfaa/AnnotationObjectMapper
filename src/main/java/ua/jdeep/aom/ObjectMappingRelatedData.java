package ua.jdeep.aom;

class ObjectMappingRelatedData<T> {
    Object source;
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
