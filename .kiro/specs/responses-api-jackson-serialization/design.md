# Design Document

## Overview

This design implements Jackson-based JSON serialization and deserialization for the Responses API specification classes. The solution uses Jackson annotations, custom serializers/deserializers, and a centralized ObjectMapper configuration to handle the complex requirements including discriminated unions, snake_case naming, lowercase enums, and special coordinate handling.

## Architecture

The implementation follows a layered approach:

1. **Annotation Layer**: Jackson annotations applied directly to domain classes
2. **Custom Serialization Layer**: Custom serializers/deserializers for special cases (enums, coordinates, discriminated unions)
3. **Configuration Layer**: Centralized ObjectMapper configuration that wires everything together
4. **Utility Layer**: Helper classes for common serialization patterns

### Key Design Decisions

- **Non-invasive approach**: Minimize changes to existing domain classes by using Jackson's annotation-based configuration
- **Convention over configuration**: Use Jackson's `PropertyNamingStrategies.SNAKE_CASE` for automatic field name conversion
- **Type safety**: Leverage Jackson's polymorphic type handling for discriminated unions
- **Reusability**: Create a single configured ObjectMapper that can be reused throughout the application

## Components and Interfaces

### 1. ObjectMapper Configuration

**Class**: `ResponsesApiObjectMapper`

A factory class that creates and configures a Jackson ObjectMapper with all necessary settings:

```java
public class ResponsesApiObjectMapper {
    public static ObjectMapper create() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Snake case naming
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        
        // Ignore unknown properties
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // Exclude null values
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        // Register custom modules
        mapper.registerModule(new ResponsesApiModule());
        
        return mapper;
    }
}
```

### 2. Custom Jackson Module

**Class**: `ResponsesApiModule`

A Jackson module that registers all custom serializers and deserializers:

```java
public class ResponsesApiModule extends SimpleModule {
    public ResponsesApiModule() {
        // Register enum serializer/deserializer
        addSerializer(Enum.class, new LowercaseEnumSerializer());
        addDeserializer(Enum.class, new LowercaseEnumDeserializer());
        
        // Register coordinate serializer/deserializer
        addSerializer(Coordinate.class, new CoordinateSerializer());
        addDeserializer(Coordinate.class, new CoordinateDeserializer());
    }
}
```

### 3. Enum Serialization

**Classes**: `LowercaseEnumSerializer`, `LowercaseEnumDeserializer`

Custom serializers that convert enum values to/from lowercase strings:

```java
public class LowercaseEnumSerializer extends JsonSerializer<Enum<?>> {
    @Override
    public void serialize(Enum<?> value, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        gen.writeString(value.name().toLowerCase());
    }
}
```

### 4. Coordinate Serialization

**Classes**: `CoordinateSerializer`, `CoordinateDeserializer`

Custom serializers that flatten Coordinate objects into separate x/y fields:

```java
public class CoordinateSerializer extends JsonSerializer<Coordinate> {
    @Override
    public void serialize(Coordinate value, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        gen.writeNumber(value.x());
        gen.writeNumber(value.y());
    }
}
```

### 5. Discriminated Union Handling

Use Jackson's `@JsonTypeInfo` and `@JsonSubTypes` annotations on base types:

**Example for ResponseInputItem**:
```java
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = UserMessage.class, name = "message"),
    @JsonSubTypes.Type(value = DeveloperMessage.class, name = "developer_message"),
    // ... other subtypes
})
public interface ResponseInputItem {
    // ...
}
```

## Data Models

### Core Domain Classes

The existing domain classes (`CreateResponse`, `Response`, and all nested types) will be annotated with:

- `@JsonProperty`: For explicit field name mapping (when needed)
- `@JsonInclude(JsonInclude.Include.NON_NULL)`: To exclude null fields
- `@JsonTypeInfo` / `@JsonSubTypes`: For discriminated unions

### Discriminated Union Types

The following types require polymorphic handling:

1. **ResponseInputItem** - Base type for input messages
   - Subtypes: `UserMessage`, `DeveloperMessage`, `AssistantMessage`
   
2. **ResponseOutput** - Base type for output items
   - Subtypes: `OutputMessage`, `ToolCall`, `ReasoningContent`, etc.
   
3. **Tool** - Base type for tools
   - Subtypes: `FunctionTool`, `WebSearchTool`, `FileSearchTool`, `CodeInterpreterTool`, `ComputerUseTool`, `McpTool`, `CustomTool`
   
4. **ToolCall** - Base type for tool calls
   - Subtypes: `FunctionToolCall`, `WebSearchToolCall`, `FileSearchToolCall`, etc.

5. **MessageContent** - Base type for message content
   - Subtypes: `Text`, `Image`, `File`

6. **ComputerUseAction** - Base type for computer actions
   - Subtypes: `ClickAction`, `TypeAction`, `KeyPressAction`, `MoveAction`, `ScreenshotAction`, etc.

### Special Types

- **Coordinate**: Requires custom serialization to split into x/y fields
- **All Enums**: Require lowercase serialization

## Co
rrectness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*


### Property Reflection

After reviewing the prework, several properties can be consolidated:

- Properties 6.1-6.5 are specific instances of the general discriminated union properties (5.1-5.2), so they are redundant
- Property 1.4 (OpenAPI validation) is valuable but may be complex to implement - we'll keep it as a comprehensive validation
- Properties 2.1 and the various round-trip properties (3.2, 4.2, 5.2) can be unified into a single comprehensive round-trip property

### Core Properties

**Property 1: Snake case field naming**
*For any* CreateResponse or Response object, when serialized to JSON, all field names at all nesting levels should be in snake_case format (words separated by underscores, all lowercase).
**Validates: Requirements 1.1, 1.3**

**Property 2: Null field exclusion**
*For any* CreateResponse or Response object with null fields, when serialized to JSON, those null fields should not appear in the resulting JSON string.
**Validates: Requirements 1.2**

**Property 3: Serialization round-trip preservation**
*For any* CreateResponse or Response object, serializing to JSON and then deserializing back should produce an object equivalent to the original.
**Validates: Requirements 2.1, 2.2, 3.2, 4.2, 5.2**

**Property 4: Unknown field tolerance**
*For any* valid Response JSON with additional unknown fields added, deserialization should succeed and produce a valid Response object (ignoring the unknown fields).
**Validates: Requirements 2.3**

**Property 5: Missing optional field handling**
*For any* Response JSON with optional fields omitted, deserialization should succeed and set those fields to null in the resulting object.
**Validates: Requirements 2.4**

**Property 6: Enum lowercase serialization**
*For any* object containing enum fields, when serialized to JSON, all enum values should appear as lowercase strings (with underscores preserved for multi-word enums).
**Validates: Requirements 3.1, 3.3**

**Property 7: Coordinate field separation**
*For any* object containing a Coordinate field, when serialized to JSON, the coordinate should appear as separate "x" and "y" numeric fields (not as a nested object).
**Validates: Requirements 4.1**

**Property 8: Discriminated union type preservation**
*For any* discriminated union type (ResponseInputItem, ResponseOutput, Tool, ToolCall, MessageContent, ComputerUseAction), when serialized to JSON, the discriminator field should be present with the correct type value, and when deserialized, the correct concrete subtype should be instantiated.
**Validates: Requirements 5.1, 5.2, 5.5, 6.1, 6.2, 6.3, 6.4, 6.5**

## Error Handling

### Deserialization Errors

The system will throw `JsonProcessingException` (or subclasses) for the following error conditions:

1. **Invalid enum value**: When JSON contains an enum value that doesn't match any enum constant
2. **Missing discriminator**: When deserializing a discriminated union without a type field
3. **Unknown discriminator**: When the discriminator value doesn't match any known subtype
4. **Incomplete coordinate**: When JSON has only x or only y field for a coordinate
5. **Malformed JSON**: When JSON syntax is invalid

All exceptions should include descriptive messages indicating:
- The field or type that caused the error
- The invalid value (if applicable)
- What was expected

### Serialization Errors

Serialization should generally not fail for valid domain objects. However, if it does:

1. Throw `JsonProcessingException` with a clear message
2. Include the object type and field that caused the issue
3. Preserve the original exception as the cause if applicable

## Testing Strategy

### Unit Tests

Unit tests will cover:

1. **Basic serialization**: Serialize simple CreateResponse/Response objects and verify JSON structure
2. **Basic deserialization**: Deserialize known JSON strings and verify object fields
3. **Enum handling**: Test specific enum values serialize to lowercase
4. **Coordinate handling**: Test Coordinate serialization produces x/y fields
5. **Discriminated unions**: Test each subtype serializes with correct discriminator
6. **Error cases**: Test invalid enum, missing discriminator, incomplete coordinate

### Property-Based Tests

Property-based tests will use a Java PBT library (we'll use **jqwik** for Java) to verify:

1. **Property 1**: Generate random CreateResponse/Response objects, serialize, verify all keys are snake_case
2. **Property 2**: Generate objects with random null fields, serialize, verify nulls are excluded
3. **Property 3**: Generate random objects, serialize then deserialize, verify equivalence
4. **Property 4**: Generate valid JSON, add random unknown fields, verify deserialization succeeds
5. **Property 5**: Generate JSON, randomly omit optional fields, verify deserialization succeeds with nulls
6. **Property 6**: Generate objects with enums, serialize, verify all enum values are lowercase
7. **Property 7**: Generate objects with coordinates, serialize, verify x/y field structure
8. **Property 8**: Generate discriminated union instances, serialize then deserialize, verify type preservation

Each property test should run at least 100 iterations with randomly generated data.

### Test Data Generators

We'll need custom generators (Arbitraries in jqwik) for:

- `CreateResponse` objects with random valid field values
- `Response` objects with random valid field values
- All discriminated union subtypes
- Valid JSON strings with various field combinations
- Coordinate objects with random x/y values
- Enum values from all enum types

## Implementation Notes

### Jackson Version

Use Jackson 2.15+ for best support of Java records and modern features.

### Maven Dependencies

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jdk8</artifactId>
    <version>2.15.2</version>
</dependency>
```

### Annotation Strategy

1. **Minimal annotations on domain classes**: Use Jackson's conventions where possible
2. **Explicit annotations for special cases**: Use `@JsonProperty`, `@JsonTypeInfo`, etc. only when needed
3. **Custom serializers for complex cases**: Use custom serializers for Coordinate and enums

### Performance Considerations

- ObjectMapper instances are thread-safe and expensive to create - reuse a single configured instance
- Consider caching serialized JSON for frequently used objects
- Use streaming API for very large responses if needed

### Backward Compatibility

This implementation adds Jackson support without modifying the existing domain model's public API. Existing code that doesn't use JSON serialization will be unaffected.
