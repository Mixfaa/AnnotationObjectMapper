package ua.jdeep.aom.exceptions;

/**
 * Tells, that return or parameter types are not matching in source class and target class
 */
public class TypesNotMatchException extends Exception{
    public TypesNotMatchException(String message) {
        super(message);
    }
}
