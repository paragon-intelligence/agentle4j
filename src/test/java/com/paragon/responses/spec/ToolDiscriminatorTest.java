package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.ResponsesApiObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Basic verification test for Tool discriminated union annotations. Tests that the Jackson
 * annotations correctly handle polymorphic serialization/deserialization.
 */
class ToolDiscriminatorTest {

  private final ObjectMapper mapper = ResponsesApiObjectMapper.create();

  @Test
  void functionToolSerializesWithTypeField() throws Exception {
    // Note: FunctionTool is abstract, so we can't test it directly
    // This test verifies the annotation is present on the interface
    assertTrue(
        Tool.class.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonTypeInfo.class),
        "Tool interface should have @JsonTypeInfo annotation");
    assertTrue(
        Tool.class.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonSubTypes.class),
        "Tool interface should have @JsonSubTypes annotation");
  }

  @Test
  void fileSearchToolSerializesWithTypeField() throws Exception {
    // Create a FileSearchTool
    FileSearchTool tool = new FileSearchTool(List.of("vector_store_1"), null, null, null);

    // Serialize
    String json = mapper.writeValueAsString(tool);

    // Verify type field is present
    assertTrue(
        json.contains("\"type\":\"file_search\"") || json.contains("\"type\": \"file_search\""),
        "Serialized JSON should contain type field with value 'file_search'");
  }

  @Test
  void codeInterpreterToolSerializesWithTypeField() throws Exception {
    // Create a CodeInterpreterTool
    CodeInterpreterTool tool = new CodeInterpreterTool("container_123");

    // Serialize
    String json = mapper.writeValueAsString(tool);

    // Verify type field is present
    assertTrue(
        json.contains("\"type\":\"code_interpreter\"")
            || json.contains("\"type\": \"code_interpreter\""),
        "Serialized JSON should contain type field with value 'code_interpreter'");
  }

  @Test
  void webSearchToolSerializesWithTypeField() throws Exception {
    // Create a WebSearchTool
    WebSearchTool tool = new WebSearchTool(null, null, null);

    // Serialize
    String json = mapper.writeValueAsString(tool);

    // Verify type field is present
    assertTrue(
        json.contains("\"type\":\"web_search\"") || json.contains("\"type\": \"web_search\""),
        "Serialized JSON should contain type field with value 'web_search'");
  }

  @Test
  void customToolSerializesWithTypeField() throws Exception {
    // Create a CustomTool
    CustomTool tool = new CustomTool("my_custom_tool", "A custom tool", null);

    // Serialize
    String json = mapper.writeValueAsString(tool);

    // Verify type field is present
    assertTrue(
        json.contains("\"type\":\"custom\"") || json.contains("\"type\": \"custom\""),
        "Serialized JSON should contain type field with value 'custom'");
  }

  @Test
  void deserializesFileSearchToolFromJson() throws Exception {
    // JSON with type discriminator
    String json =
        """
        {
            "type": "file_search",
            "vector_store_ids": ["vector_store_1"]
        }
        """;

    // Deserialize as Tool
    Tool tool = mapper.readValue(json, Tool.class);

    // Verify correct type
    assertInstanceOf(
        FileSearchTool.class, tool, "Should deserialize to FileSearchTool based on type field");
  }

  @Test
  void deserializesCodeInterpreterToolFromJson() throws Exception {
    // JSON with type discriminator
    String json =
        """
        {
            "type": "code_interpreter",
            "container": "container_123"
        }
        """;

    // Deserialize as Tool
    Tool tool = mapper.readValue(json, Tool.class);

    // Verify correct type
    assertInstanceOf(
        CodeInterpreterTool.class,
        tool,
        "Should deserialize to CodeInterpreterTool based on type field");
  }

  @Test
  void deserializesWebSearchToolFromJson() throws Exception {
    // JSON with type discriminator
    String json =
        """
        {
            "type": "web_search"
        }
        """;

    // Deserialize as Tool
    Tool tool = mapper.readValue(json, Tool.class);

    // Verify correct type
    assertInstanceOf(
        WebSearchTool.class, tool, "Should deserialize to WebSearchTool based on type field");
  }

  @Test
  void deserializesCustomToolFromJson() throws Exception {
    // JSON with type discriminator
    String json =
        """
        {
            "type": "custom",
            "name": "my_custom_tool"
        }
        """;

    // Deserialize as Tool
    Tool tool = mapper.readValue(json, Tool.class);

    // Verify correct type
    assertInstanceOf(
        CustomTool.class, tool, "Should deserialize to CustomTool based on type field");
  }
}
