# AnnotationObjectMapper

## Overview

`AnnotationObjectMapper` is a lightweight Java library that helps map annotated source objects to target domain objects. It provides a set of utilities to read, transform, and apply custom annotation‑based mapping rules, making it easier to decouple data‑transfer objects (DTOs) from business models.

## Key Features

- **Annotation‑driven mapping** – Define mapping behavior directly on source classes using custom annotations.
- **Flexible conversion utilities** – Helper methods for type conversion, null‑handling, and collection mapping.
- **Extensible advice system** – Plug‑in advice interfaces (`Advice`) to customize mapping logic (e.g., logging, validation).
- **Centralised constants** – `AOMConstants` holds reusable keys and default values.
- **Exception hierarchy** – Domain‑specific exceptions (`MappingException`, `AnnotationProcessingException`) for clear error handling.
- **Testable design** – Interfaces are designed for easy unit testing and mocking.

## Technologies Used

- **Java 8+** – Core language for implementation.
- **Maven** – Build tool (`pom.xml` defines dependencies and packaging).
- **JUnit 5** (assumed for tests in `src/test`) – Unit testing framework.
- **SLF4J** – Logging abstraction used by advice implementations.

## Project Structure

```
src/main/java/ua/jdeep/aom/
├── AOMConstants.java            # Constant definitions
├── AnnotationObjectMapper.java   # Core mapper implementation
├── AnnotationUtils.java          # Utility methods for annotation handling
├── AnnotationsImplementer.java   # Service that applies mapping rules
├── ObjectMappingRelatedData.java # Data holder for mapping context
├── advices/                      # Advice interfaces & default implementations
├── annotations/                  # Custom annotation definitions
└── exceptions/                   # Custom exception classes
```

## Getting Started

1. Clone the repository.
2. Run `mvn clean install` to build the library.
3. Add the generated JAR as a dependency in your project.
4. Annotate your source DTOs and invoke `AnnotationObjectMapper.map(source, Target.class)`.

## Usage Example

```java
@MapTo(TargetClass.class)
public class SourceDto {
    @MapField("name")
    private String fullName;
    // getters & setters
}

SourceDto src = new SourceDto();
src.setFullName("Jane Doe");
TargetClass target = AnnotationObjectMapper.map(src, TargetClass.class);
```

Example:

<p>Main testing method:</p>

```java
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
    }
}
```

<p>Output</p>

```java
Mapped annotation properties:
intValue: 8302
stringValue: default string value
Mapped functions:
Sum:4
getHelloWorld:Hello world
Mapped field:
23.72
```

<p>ExampleSourceClass code</p>

```java
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
```

<p>Target class code</p>

```java
@MappingTarget
public abstract class TargetClass {

    @MapAnnotationProperty(annotationPropName = "stringValue", sourceClass = ExampleSourceClass.class, targetAnnotation = TestClassAnnotation.class)
    public static String stringValueProp;

    @MapAnnotationProperty(annotationPropName = "intValue", sourceClass = ExampleSourceClass.class, targetAnnotation = TestClassAnnotation2.class)
    public static int intValueProp;

    @MapMethod(annotatedWith = TestSumMethod.class)
    public abstract int sumMethod(int a, int b);

    @MapMethod(annotatedWith = TestGetHelloWorld.class)
    public abstract String getHelloWorldMethod();

    @MapField(annotatedWith = TestField.class)
    public abstract Double getField();
}

```

## Contributing

Contributions are welcome! Please fork the repository, create a feature branch, and submit a pull request. Follow the existing code style and include unit tests for new functionality.

## License

This project is licensed under the MIT License – see the `LICENSE` file for details.
