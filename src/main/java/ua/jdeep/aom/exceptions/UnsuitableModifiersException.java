package ua.jdeep.aom.exceptions;

import ua.jdeep.aom.annotations.MapAnnotationPropertyForClass;

/**
 * Tells that field/method/class have wrong modifiers.
 * Every target class must be abstract class.
 * Every field annotated with {@link MapAnnotationPropertyForClass} must be static.
 * Every method annotated with {@link ua.jdeep.aom.annotations.MapField} or {@link ua.jdeep.aom.annotations.MapMethod} must be abstract.
 */
public class UnsuitableModifiersException extends Exception{
    public UnsuitableModifiersException(String message) {
        super(message);
    }
}
