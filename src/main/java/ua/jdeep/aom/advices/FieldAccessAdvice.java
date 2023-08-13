package ua.jdeep.aom.advices;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import ua.jdeep.aom.AOMConstants;

import java.lang.reflect.Field;


public abstract class FieldAccessAdvice {
    protected final Field field;

    public FieldAccessAdvice(Field field) {
        this.field = field;
    }

    public static class Getter extends FieldAccessAdvice {
        public Getter(Field field) {
            super(field);
        }
 
        @RuntimeType
        @Advice.OnMethodEnter
        public Object getField(@FieldValue(AOMConstants.TARGET_OBJECT_FIELD_NAME) Object object) {
            try {
                return field.get(object);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class Setter extends FieldAccessAdvice {
        public Setter(Field field) {
            super(field);
        }

        @RuntimeType
        @Advice.OnMethodEnter
        public void setField(@FieldValue(AOMConstants.TARGET_OBJECT_FIELD_NAME) Object object, @Argument(0) Object arg) {
            try {
                field.set(object, arg);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}