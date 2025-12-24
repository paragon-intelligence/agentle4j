package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.ResponsesApiObjectMapper;
import org.junit.jupiter.api.Test;

/** Tests for the ToolChoiceDeserializer to ensure proper polymorphic deserialization. */
class ToolChoiceDeserializerTest {

  private final ObjectMapper objectMapper = ResponsesApiObjectMapper.create();

  @Test
  void deserialize_stringValueNone_returnsToolChoiceMode() throws Exception {
    String json = "\"none\"";

    ToolChoice result = objectMapper.readValue(json, ToolChoice.class);

    assertNotNull(result);
    assertTrue(result instanceof ToolChoiceMode);
    assertEquals(ToolChoiceMode.NONE, result);
  }

  @Test
  void deserialize_stringValueAuto_returnsToolChoiceMode() throws Exception {
    String json = "\"auto\"";

    ToolChoice result = objectMapper.readValue(json, ToolChoice.class);

    assertNotNull(result);
    assertTrue(result instanceof ToolChoiceMode);
    assertEquals(ToolChoiceMode.AUTO, result);
  }

  @Test
  void deserialize_stringValueRequired_returnsToolChoiceMode() throws Exception {
    String json = "\"required\"";

    ToolChoice result = objectMapper.readValue(json, ToolChoice.class);

    assertNotNull(result);
    assertTrue(result instanceof ToolChoiceMode);
    assertEquals(ToolChoiceMode.REQUIRED, result);
  }

  @Test
  void deserialize_invalidStringValue_throwsException() {
    String json = "\"invalid_value\"";

    assertThrows(Exception.class, () -> objectMapper.readValue(json, ToolChoice.class));
  }

  @Test
  void deserialize_objectValue_returnsAllowedTools() throws Exception {
    // Simplified test - just verify the mode is deserialized
    String json =
        """
        {
          "mode": "auto",
          "tools": []
        }
        """;

    ToolChoice result = objectMapper.readValue(json, ToolChoice.class);

    assertNotNull(result);
    assertTrue(result instanceof AllowedTools);

    AllowedTools allowedTools = (AllowedTools) result;
    assertEquals(AllowedToolsMode.AUTO, allowedTools.mode());
    assertNotNull(allowedTools.tools());
    assertTrue(allowedTools.tools().isEmpty());
  }

  @Test
  void deserialize_objectWithRequiredMode_returnsAllowedTools() throws Exception {
    String json =
        """
        {
          "mode": "required",
          "tools": []
        }
        """;

    ToolChoice result = objectMapper.readValue(json, ToolChoice.class);

    assertNotNull(result);
    assertTrue(result instanceof AllowedTools);

    AllowedTools allowedTools = (AllowedTools) result;
    assertEquals(AllowedToolsMode.REQUIRED, allowedTools.mode());
    assertTrue(allowedTools.tools().isEmpty());
  }

  @Test
  void deserialize_inResponseObject_deserializesCorrectly() throws Exception {
    // Test that ToolChoice deserializes correctly when nested in a Response object
    String json =
        """
        {
          "id": "resp-123",
          "model": "gpt-4o",
          "tool_choice": "auto",
          "status": "completed"
        }
        """;

    Response response = objectMapper.readValue(json, Response.class);

    assertNotNull(response);
    assertEquals("resp-123", response.id());
    assertEquals("gpt-4o", response.model());
    assertNotNull(response.toolChoice());
    assertTrue(response.toolChoice() instanceof ToolChoiceMode);
    assertEquals(ToolChoiceMode.AUTO, response.toolChoice());
  }

  @Test
  void deserialize_objectToolChoiceInResponse_deserializesCorrectly() throws Exception {
    String json =
        """
        {
          "id": "resp-456",
          "model": "gpt-4o",
          "tool_choice": {
            "mode": "required",
            "tools": []
          },
          "status": "completed"
        }
        """;

    Response response = objectMapper.readValue(json, Response.class);

    assertNotNull(response);
    assertEquals("resp-456", response.id());
    assertNotNull(response.toolChoice());
    assertTrue(response.toolChoice() instanceof AllowedTools);

    AllowedTools allowedTools = (AllowedTools) response.toolChoice();
    assertEquals(AllowedToolsMode.REQUIRED, allowedTools.mode());
  }
}
