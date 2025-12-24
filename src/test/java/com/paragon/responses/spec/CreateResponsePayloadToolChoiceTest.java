package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.ResponsesApiObjectMapper;
import org.junit.jupiter.api.Test;

/** Tests to verify ToolChoice serialization/deserialization in CreateResponsePayload */
class CreateResponsePayloadToolChoiceTest {

  private final ObjectMapper objectMapper = ResponsesApiObjectMapper.create();

  @Test
  void serializePayloadWithToolChoiceString() throws Exception {
    CreateResponsePayload payload =
        CreateResponsePayload.builder().model("gpt-4o").toolChoice(ToolChoiceMode.AUTO).build();

    String json = objectMapper.writeValueAsString(payload);

    assertTrue(json.contains("\"tool_choice\":\"auto\""));
  }

  @Test
  void deserializePayloadWithToolChoiceString() throws Exception {
    String json =
        """
        {
          "model": "gpt-4o",
          "tool_choice": "required"
        }
        """;

    CreateResponsePayload payload = objectMapper.readValue(json, CreateResponsePayload.class);

    assertNotNull(payload);
    assertNotNull(payload.toolChoice());
    assertTrue(payload.toolChoice() instanceof ToolChoiceMode);
    assertEquals(ToolChoiceMode.REQUIRED, payload.toolChoice());
  }

  @Test
  void deserializePayloadWithToolChoiceObject() throws Exception {
    String json =
        """
        {
          "model": "gpt-4o",
          "tool_choice": {
            "mode": "auto",
            "tools": []
          }
        }
        """;

    CreateResponsePayload payload = objectMapper.readValue(json, CreateResponsePayload.class);

    assertNotNull(payload);
    assertNotNull(payload.toolChoice());
    assertTrue(payload.toolChoice() instanceof AllowedTools);

    AllowedTools allowedTools = (AllowedTools) payload.toolChoice();
    assertEquals(AllowedToolsMode.AUTO, allowedTools.mode());
    assertTrue(allowedTools.tools().isEmpty());
  }

  @Test
  void roundTripPayloadWithToolChoice() throws Exception {
    CreateResponsePayload original =
        CreateResponsePayload.builder().model("gpt-4o").toolChoice(ToolChoiceMode.NONE).build();

    String json = objectMapper.writeValueAsString(original);
    CreateResponsePayload deserialized = objectMapper.readValue(json, CreateResponsePayload.class);

    assertNotNull(deserialized.toolChoice());
    assertEquals(ToolChoiceMode.NONE, deserialized.toolChoice());
  }
}
