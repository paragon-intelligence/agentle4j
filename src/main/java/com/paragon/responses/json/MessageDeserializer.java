package com.paragon.responses.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.spec.AssistantMessage;
import com.paragon.responses.spec.DeveloperMessage;
import com.paragon.responses.spec.InputMessageStatus;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.MessageContent;
import com.paragon.responses.spec.OutputMessage;
import com.paragon.responses.spec.UserMessage;
import java.io.IOException;
import java.util.List;

/**
 * Custom deserializer for Message that uses the 'role' field to determine which concrete subclass
 * to instantiate. Also handles OutputMessage which has an 'id' field.
 */
public class MessageDeserializer extends JsonDeserializer<Message> {

  private static final TypeReference<List<MessageContent>> CONTENT_TYPE = new TypeReference<>() {};

  @Override
  public Message deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    ObjectMapper mapper = (ObjectMapper) p.getCodec();
    JsonNode node = mapper.readTree(p);

    String role = node.has("role") ? node.get("role").asText() : null;

    if (role == null) {
      throw new IOException("Missing 'role' field in Message JSON");
    }

    // Parse content
    List<MessageContent> content = null;
    if (node.has("content")) {
      content = mapper.convertValue(node.get("content"), CONTENT_TYPE);
    }
    if (content == null) {
      throw new IOException("Missing 'content' field in Message JSON");
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
      default -> throw new IOException("Unknown role: " + role);
    };
  }
}
