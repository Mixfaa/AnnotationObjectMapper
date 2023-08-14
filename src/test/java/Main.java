import testAnnotation.TestClassAnnotation;
import testAnnotation.TestClassAnnotation2;
import testEntity.ExampleSourceClass;
import testEntity.TargetClass;
import ua.jdeep.aom.AnnotationObjectMapper;

public class Main {
    public static void main(String[] args) throws Exception {
        AnnotationObjectMapper.initialize();

        System.out.println("Mapped annotation properties:");
        System.out.println("intValue: " + TargetClass.intValueProp);
        System.out.println("stringValue: " + TargetClass.stringValueProp);

        ExampleSourceClass sourceObject = new ExampleSourceClass(23.72);

        TargetClass mappedObject = AnnotationObjectMapper.getInstance().mapObjectTo(sourceObject, TargetClass.class);

        System.out.println("Mapped functions:");
        System.out.println("Sum:" + mappedObject.sumMethod(2, 2));
        System.out.println("getHelloWorld:" + mappedObject.getHelloWorldMethod());

        System.out.println("Mapped field:");
        System.out.println(mappedObject.getField());

        System.out.println(mappedObject.getAnnotation(TestClassAnnotation.class).stringValue());
        System.out.println(mappedObject.getAnnotation(TestClassAnnotation2.class).intValue());
    }
}