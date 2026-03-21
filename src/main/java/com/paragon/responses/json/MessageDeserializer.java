package com.paragon.responses.json;

import com.paragon.responses.spec.AssistantMessage;
import com.paragon.responses.spec.DeveloperMessage;
import com.paragon.responses.spec.InputMessageStatus;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.MessageContent;
import com.paragon.responses.spec.OutputMessage;
import com.paragon.responses.spec.Text;
import com.paragon.responses.spec.UserMessage;
import java.util.List;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

/**
 * Custom deserializer for Message that uses the 'role' field to determine which concrete subclass
 * to instantiate. Also handles OutputMessage which has an 'id' field.
 */
public class MessageDeserializer extends ValueDeserializer<Message> {

  @Override
  public Message deserialize(JsonParser p, DeserializationContext ctxt)
      throws tools.jackson.core.JacksonException {
    JsonNode node = ctxt.readTree(p);

    String role = node.has("role") ? node.get("role").asText() : null;

    if (role == null) {
      return ctxt.reportInputMismatch(Message.class, "Missing 'role' field in Message JSON");
    }

    // Parse content - support both string (single text) and array/object formats.
    // Use explicit per-element deserialization so that the custom
    // MessageContentDeserializer is always applied.
    List<MessageContent> content;
    JsonNode contentNode = node.get("content");
    if (contentNode == null || contentNode.isNull()) {
      return ctxt.reportInputMismatch(Message.class, "Missing 'content' field in Message JSON");
    }
    if (contentNode.isTextual()) {
      // Backwards/forwards compatible: accept plain string and wrap as single Text item
      content = List.of(new Text(contentNode.asText()));
    } else if (contentNode.isArray()) {
      List<MessageContent> tmp = new java.util.ArrayList<>();
      for (JsonNode item : contentNode) {
        tmp.add(ctxt.readTreeAsValue(item, MessageContent.class));
      }
      content = java.util.List.copyOf(tmp);
    } else {
      // Single object treated as one content item
      content = List.of(ctxt.readTreeAsValue(contentNode, MessageContent.class));
    }

    // Parse status
    InputMessageStatus status = null;
    if (node.has("status") && !node.get("status").isNull()) {
      String statusStr = node.get("status").asText();
      status = InputMessageStatus.valueOf(statusStr.toUpperCase());
    }

    // Check if this is an OutputMessage (has 'id' field and role is 'assistant')
    if ("assistant".equals(role) && node.has("id")) {
      String id = node.get("id").asText();
      // Parse optional 'parsed' field - we'll leave it as null for now
      // since we can't know the generic type
      return new OutputMessage<>(content, id, status, null);
    }

    return switch (role) {
      case "developer", "system" -> new DeveloperMessage(content, status);
      case "user" -> new UserMessage(content, status);
      case "assistant" -> new AssistantMessage(content, status);
      default -> ctxt.reportInputMismatch(Message.class, "Unknown role: %s", role);
    };
  }
}
