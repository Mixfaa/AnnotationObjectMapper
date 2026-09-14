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

## Contributing

Contributions are welcome! Please fork the repository, create a feature branch, and submit a pull request. Follow the existing code style and include unit tests for new functionality.

## License

This project is licensed under the MIT License – see the `LICENSE` file for details.
