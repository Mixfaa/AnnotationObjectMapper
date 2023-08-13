package ua.jdeep.aom.exceptions;

/**
 * Tells, that there is 0 or more than 1 definition of method/field.
 */
public class AmbiguousDefinitionException extends Exception {
    public AmbiguousDefinitionException(String message) {
        super(message);
    }
}
