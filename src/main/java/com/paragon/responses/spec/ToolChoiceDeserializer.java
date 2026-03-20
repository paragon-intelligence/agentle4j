package com.paragon.responses.spec;

import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.paragon.responses.ResponsesApiObjectMapper;
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
public class ToolChoiceDeserializer extends ValueDeserializer<ToolChoice> {

  private static final ObjectMapper MAPPER = ResponsesApiObjectMapper.create();

  @Override
  public ToolChoice deserialize(JsonParser parser, DeserializationContext context)
      throws tools.jackson.core.JacksonException {
    JsonNode node = parser.readValueAsTree();

    // If it's a string value, parse as ToolChoiceMode
    if (node.isTextual()) {
      String value = node.asText().toUpperCase();
      try {
        return ToolChoiceMode.valueOf(value);
      } catch (IllegalArgumentException e) {
        return context.reportInputMismatch(
            ToolChoice.class,
            "Invalid tool_choice string value: %s. Expected one of: none, auto, required",
            node.asText());
      }
    }

    // If it's an object, manually parse as AllowedTools to avoid recursion
    if (node.isObject()) {
      // Parse the mode field
      JsonNode modeNode = node.get("mode");
      if (modeNode == null) {
        return context.reportInputMismatch(
            ToolChoice.class, "Missing 'mode' field in tool_choice object");
      }
      String modeString = modeNode.asText().toUpperCase();
      AllowedToolsMode mode = AllowedToolsMode.valueOf(modeString);

      // Parse the tools field using ResponsesApiObjectMapper for proper polymorphic
      // deserialization
      JsonNode toolsNode = node.get("tools");
      if (toolsNode == null) {
        return context.reportInputMismatch(
            ToolChoice.class, "Missing 'tools' field in tool_choice object");
      }
      List<Tool> tools = MAPPER.treeToValue(toolsNode, new TypeReference<List<Tool>>() {});

      return new AllowedTools(mode, tools);
    }

    return context.reportInputMismatch(
        ToolChoice.class,
        "Cannot deserialize tool_choice: expected a string or object, but got: %s",
        node.getNodeType());
  }
}
