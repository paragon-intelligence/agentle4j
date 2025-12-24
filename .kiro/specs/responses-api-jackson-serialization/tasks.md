# Implementation Plan

- [x] 1. Set up Jackson dependencies and configuration infrastructure




  - Add Jackson dependencies to pom.xml (jackson-databind, jackson-datatype-jdk8)
  - Create ResponsesApiObjectMapper factory class with centralized configuration
  - Configure snake_case naming strategy, null exclusion, and unknown property handling
  - _Requirements: 1.1, 1.2, 2.3, 7.1_

- [x] 2. Implement custom enum serialization





  - Create LowercaseEnumSerializer to convert enum names to lowercase
  - Create LowercaseEnumDeserializer to parse lowercase strings to enums
  - Handle multi-word enums with underscores correctly
  - _Requirements: 3.1, 3.2, 3.3_

- [x] 2.1 Write property test for enum serialization

  - **Property 6: Enum lowercase serialization**
  - **Validates: Requirements 3.1, 3.3**

- [x] 2.2 Write unit test for invalid enum deserialization

  - Test that invalid enum values throw descriptive errors
  - _Requirements: 3.4_

- [x] 3. Implement custom Coordinate serialization



  - Create CoordinateSerializer to output separate x and y fields
  - Create CoordinateDeserializer to construct Coordinate from x/y fields
  - Handle null coordinates appropriately
  - _Requirements: 4.1, 4.2, 4.3_




- [x] 3.1 Write property test for coordinate serialization


  - **Property 7: Coordinate field separation**

  - **Validates: Requirements 4.1**

- [x] 3.2 Write unit test for incomplete coordinate deserialization




  - Test that JSON with only x or only y field throws descriptive error
  - _Requirements: 4.4_

- [x] 4. Create ResponsesApiModule to register custom serializers




  - Create Jackson SimpleModule subclass
  - Register LowercaseEnumSerializer and LowercaseEnumDeserializer
  - Register CoordinateSerializer and CoordinateDeserializer
  - Integrate module into ResponsesApiObjectMapper configuration
  - _Requirements: 7.2_

- [x] 5. Annotate ResponseInputItem and subtypes for discriminated union





  - Add @JsonTypeInfo annotation to ResponseInputItem interface with type discriminator
  - Add @JsonSubTypes annotation listing all subtypes (UserMessage, DeveloperMessage, AssistantMessage)
  - Verify each subtype has correct type name mapping
  - _Requirements: 5.1, 5.2, 6.1_

- [x] 6. Annotate ResponseOutput and subtypes for discriminated union




  - Add @JsonTypeInfo annotation to ResponseOutput interface
  - Add @JsonSubTypes annotation listing all output subtypes (OutputMessage, ToolCall, ReasoningContent, etc.)
  - Verify each subtype has correct type name mapping
  - _Requirements: 5.1, 5.2, 6.2_

- [x] 7. Annotate Tool and subtypes for discriminated union



  - Add @JsonTypeInfo annotation to Tool interface
  - Add @JsonSubTypes annotation listing all tool types (FunctionTool, WebSearchTool, FileSearchTool, CodeInterpreterTool, ComputerUseTool, McpTool, CustomTool)
  - Verify each subtype has correct type name mapping
  - _Requirements: 5.1, 5.2, 6.3_

- [x] 8. Annotate ToolCall and subtypes for discriminated union




  - Add @JsonTypeInfo annotation to ToolCall interface
  - Add @JsonSubTypes annotation listing all tool call types
  - Verify each subtype has correct type name mapping
  - _Requirements: 5.1, 5.2, 6.4_

- [x] 9. Annotate MessageContent and ComputerUseAction for discriminated unions



  - Add @JsonTypeInfo and @JsonSubTypes to MessageContent (Text, Image, File)
  - Add @JsonTypeInfo and @JsonSubTypes to ComputerUseAction (ClickAction, TypeAction, KeyPressAction, etc.)
  - Verify all subtypes have correct type name mappings
  - _Requirements: 5.1, 5.2_


- [x] 9.1 Write property test for discriminated union serialization


  - **Property 8: Discriminated union type preservation**
  - **Validates: Requirements 5.1, 5.2, 5.5, 6.1, 6.2, 6.3, 6.4, 6.5**



- [x] 9.2 Write unit tests for discriminated union error cases

  - Test missing discriminator field throws descriptive error
  - Test unknown discriminator value throws descriptive error
  - _Requirements: 5.3, 5.4_



- [x] 10. Add necessary Jackson annotations to CreateResponse and Response




  - Add @JsonInclude(NON_NULL) if needed (should be handled by ObjectMapper config)

  - Add @JsonProperty annotations for any fields that don't follow standard naming
  - Ensure all nested types are properly annotated
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 10.1 Write property test for snake_case field naming

  - **Property 1: Snake case field naming**
  - **Validates: Requirements 1.1, 1.3**

- [x] 10.2 Write property test for null field exclusion

  - **Property 2: Null field exclusion**
  - **Validates: Requirements 1.2**

- [x] 11. Checkpoint - Ensure all tests pass




  - Ensure all tests pass, ask the user if questions arise.

- [x] 12. Write comprehensive round-trip property test











  - **Property 3: Serialization round-trip preservation**
  - **Validates: Requirements 2.1, 2.2, 3.2, 4.2, 5.2**

- [x] 13. Write property test for unknown field tolerance



  - **Property 4: Unknown field tolerance**
  - **Validates: Requirements 2.3**

- [x] 14. Write property test for missing optional fields




  - **Property 5: Missing optional field handling**
  - **Validates: Requirements 2.4**

- [x] 15. Create test data generators (jqwik Arbitraries)




  - Create Arbitrary for CreateResponse with random valid values
  - Create Arbitrary for Response with random valid values
  - Create Arbitraries for all discriminated union subtypes
  - Create Arbitrary for Coordinate objects
  - Create generators for valid JSON strings with various field combinations
  - _Requirements: All testing requirements_


- [x] 16. Final Checkpoint - Ensure all tests pass


  - Ensure all tests pass, ask the user if questions arise.
