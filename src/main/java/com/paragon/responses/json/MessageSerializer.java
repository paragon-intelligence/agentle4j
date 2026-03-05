package com.paragon.responses.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.MessageContent;
import com.paragon.responses.spec.Text;
import java.io.IOException;
import java.util.List;

/**
 * Custom Jackson serializer for {@link Message} and its subclasses.
 *
 * <p>The OpenAI Responses API (and OpenRouter) requires {@code content} to be a plain string
 * rather than an array of content objects. Without this serializer, Jackson would serialize
 * {@code content} as {@code [{"type":"input_text","text":"..."}]} which is rejected with
 * {@code "Invalid input: expected string, received array"}.
 *
 * <p>This serializer writes {@code content} as a plain string when the message has exactly one
 * {@link Text} content item (the common case for conversation history), and falls back to the
 * array format otherwise (for multi-content messages with images, files, etc.).
 */
public class MessageSerializer extends StdSerializer<Message> {

  public MessageSerializer() {
    super(Message.class);
  }

  /**
   * Called when serializing within a polymorphic context (e.g., {@code List<ResponseInputItem>}
   * with {@code @JsonTypeInfo}). Since we embed {@code "type"} directly in the JSON object,
   * we skip the external type wrapper and delegate to {@link #serialize}.
   */
  @Override
  public void serializeWithType(
      Message value,
      JsonGenerator gen,
      SerializerProvider provider,
      TypeSerializer typeSer)
      throws IOException {
    serialize(value, gen, provider);
  }

  @Override
  public void serialize(Message value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartObject();
    gen.writeStringField("type", "message");
    gen.writeStringField("role", value.role().name().toLowerCase());
    // OutputMessage carries an 'id' field required by the Responses API schema.
    if (value instanceof com.paragon.responses.spec.OutputMessage<?> om && om.id() != null) {
      gen.writeStringField("id", om.id());
    }
    writeContent(value.content(), gen, provider);
    if (value.status() != null) {
      gen.writeStringField("status", value.status().name().toLowerCase());
    }
    gen.writeEndObject();
  }

  private void writeContent(
      List<MessageContent> content, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    // Write as a plain string when content is a single Text item — required by OpenRouter/OpenAI
    if (content.size() == 1 && content.get(0) instanceof Text t) {
      gen.writeStringField("content", t.text());
    } else {
      gen.writeFieldName("content");
      provider.defaultSerializeValue(content, gen);
    }
  }
}
