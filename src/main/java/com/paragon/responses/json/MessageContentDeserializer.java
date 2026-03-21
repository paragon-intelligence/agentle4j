package com.paragon.responses.json;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.paragon.responses.spec.File;
import com.paragon.responses.spec.Image;
import com.paragon.responses.spec.MessageContent;
import com.paragon.responses.spec.Text;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

/**
 * Custom deserializer for {@link MessageContent} that is tolerant of plain string values.
 *
 * <p>The Responses API primarily uses discriminated union objects with a {@code type} field. Some
 * providers and internal helpers, however, may represent simple text content as a bare JSON string.
 * This deserializer accepts both formats:
 *
 * <ul>
 *   <li>If the node is a string, it is wrapped as a {@link Text} instance.
 *   <li>If the node is an object with a {@code type} field, normal polymorphic resolution is used.
 * </ul>
 */
public final class MessageContentDeserializer extends ValueDeserializer<MessageContent> {

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = Text.class, name = "input_text"),
    @JsonSubTypes.Type(value = Image.class, name = "input_image"),
    @JsonSubTypes.Type(value = File.class, name = "input_file"),
    @JsonSubTypes.Type(value = Text.class, name = "output_text"),
    @JsonSubTypes.Type(value = Image.class, name = "output_image"),
    @JsonSubTypes.Type(value = File.class, name = "output_file")
  })
  interface Delegate {}

  @Override
  public MessageContent deserialize(JsonParser p, DeserializationContext ctxt)
      throws tools.jackson.core.JacksonException {
    JsonNode node = p.readValueAsTree();

    // Plain string → treat as simple text content
    if (node.isTextual()) {
      return new Text(node.asText());
    }

    // If this is an object without an explicit "type", fall back to a tolerant
    // text representation. This handles edge cases in tests and unknown shapes
    // while keeping real API payloads (which always include "type") strict.
    if (!node.has("type")) {
      if (node.has("text") && node.get("text").isTextual()) {
        return new Text(node.get("text").asText());
      }
      return new Text(node.toString());
    }

    // Delegate to standard polymorphic handling when 'type' is present
    return (MessageContent) (Object) ctxt.readTreeAsValue(node, Delegate.class);
  }
}
