package com.paragon.responses.spec;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.ResponsesApiObjectMapper;
import java.io.IOException;
import java.util.List;

/**
 * Custom deserializer for {@link ToolChoice} to handle polymorphic deserialization.
 *
 * <p>The OpenAI API returns tool_choice as either:
 *
 * <ul>
 *   <li>A string: "none", "auto", or "required" (mapped to {@link ToolChoiceMode})
 *   <li>An object with "mode" and "tools" fields (mapped to {@link AllowedTools})
 * </ul>
 */
public class ToolChoiceDeserializer extends JsonDeserializer<ToolChoice> {

  private static final ObjectMapper MAPPER = ResponsesApiObjectMapper.create();

  @Override
  public ToolChoice deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    JsonNode node = parser.getCodec().readTree(parser);

    // If it's a string value, parse as ToolChoiceMode
    if (node.isTextual()) {
      String value = node.asText().toUpperCase();
      try {
        return ToolChoiceMode.valueOf(value);
      } catch (IllegalArgumentException e) {
        throw new IOException(
            "Invalid tool_choice string value: "
                + node.asText()
                + ". Expected one of: none, auto, required",
            e);
      }
    }

    // If it's an object, manually parse as AllowedTools to avoid recursion
    if (node.isObject()) {
      // Parse the mode field
      JsonNode modeNode = node.get("mode");
      if (modeNode == null) {
        throw new IOException("Missing 'mode' field in tool_choice object");
      }
      String modeString = modeNode.asText().toUpperCase();
      AllowedToolsMode mode = AllowedToolsMode.valueOf(modeString);

      // Parse the tools field using ResponsesApiObjectMapper for proper polymorphic
      // deserialization
      JsonNode toolsNode = node.get("tools");
      if (toolsNode == null) {
        throw new IOException("Missing 'tools' field in tool_choice object");
      }
      List<Tool> tools = MAPPER.treeToValue(toolsNode, new TypeReference<List<Tool>>() {});

      return new AllowedTools(mode, tools);
    }

    throw new IOException(
        "Cannot deserialize tool_choice: expected a string or object, but got: "
            + node.getNodeType());
  }
}
