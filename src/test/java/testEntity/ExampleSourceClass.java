package testEntity;

import testAnnotation.*;

@TestClassAnnotation
@TestClassAnnotation2(intValue = 8302)
public class ExampleSourceClass {
    @TestField
    private final Double doubleValue;

    public ExampleSourceClass(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    @TestSumMethod
    public int sum(int a, int b) {
        return a + b;
    }

    @TestGetHelloWorld
    public String getHelloWorld() {
        return "Hello world";
    }
}
