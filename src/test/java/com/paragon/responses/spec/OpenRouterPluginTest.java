package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.ResponsesApiObjectMapper;
import org.junit.jupiter.api.Test;

/** Tests for OpenRouterPlugin serialization and deserialization */
class OpenRouterPluginTest {

  private final ObjectMapper objectMapper = ResponsesApiObjectMapper.create();

  @Test
  void deserialize_ModerationPlugin() throws Exception {
    String json =
        """
        { "id": "moderation" }
        """;

    OpenRouterPlugin result = objectMapper.readValue(json, OpenRouterPlugin.class);

    assertNotNull(result);
    assertInstanceOf(OpenRouterModerationPlugin.class, result);
  }

  @Test
  void deserialize_WebPlugin() throws Exception {
    String json =
        """
        { "id": "web" }
        """;

    OpenRouterPlugin result = objectMapper.readValue(json, OpenRouterPlugin.class);

    assertNotNull(result);
    assertInstanceOf(OpenRouterWebPlugin.class, result);
  }

  @Test
  void deserialize_FileParserPlugin() throws Exception {
    String json =
        """
        { "id": "file-parser" }
        """;

    OpenRouterPlugin result = objectMapper.readValue(json, OpenRouterPlugin.class);

    assertNotNull(result);
    assertInstanceOf(OpenRouterFileParserPlugin.class, result);
  }

  @Test
  void deserialize_ResponseHealingPlugin() throws Exception {
    String json =
        """
        { "id": "response-healing" }
        """;

    OpenRouterPlugin result = objectMapper.readValue(json, OpenRouterPlugin.class);

    assertNotNull(result);
    assertInstanceOf(OpenRouterResponseHealingPlugin.class, result);
  }

  @Test
  void serialize_ModerationPlugin() throws Exception {
    OpenRouterPlugin plugin = new OpenRouterModerationPlugin();

    String json = objectMapper.writeValueAsString(plugin);

    assertTrue(json.contains("\"id\""), "JSON should contain 'id' field");
    assertTrue(
        json.contains("\"moderation\"") || json.contains("moderation"),
        "JSON should contain 'moderation' value");
  }

  @Test
  void roundTrip_WebPlugin() throws Exception {
    OpenRouterPlugin original = new OpenRouterWebPlugin();

    String json = objectMapper.writeValueAsString(original);
    OpenRouterPlugin deserialized = objectMapper.readValue(json, OpenRouterPlugin.class);

    assertNotNull(deserialized);
    assertInstanceOf(OpenRouterWebPlugin.class, deserialized);
  }
}
