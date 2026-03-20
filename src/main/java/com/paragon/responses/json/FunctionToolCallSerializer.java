package com.paragon.responses.json;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import com.paragon.responses.spec.FunctionToolCall;
import java.io.IOException;

/**
 * Custom Jackson serializer for {@link FunctionToolCall}.
 *
 * <p>When {@code FunctionToolCall} appears in a {@code List<ResponseInputItem>} (which uses
 * {@code @JsonTypeInfo(EXISTING_PROPERTY)}), the outer type resolver does NOT inject the
 * {@code "type"} field. Without this serializer, the serialized JSON lacks {@code "type":
 * "function_call"}, causing OpenRouter to reject the payload.
 *
 * <p>This serializer explicitly writes {@code "type": "function_call"} along with all other fields.
 */
public class FunctionToolCallSerializer extends StdSerializer<FunctionToolCall> {

  public FunctionToolCallSerializer() {
    super(FunctionToolCall.class);
  }

  /**
   * Called when serializing within a polymorphic context (e.g., {@code List<ResponseInputItem>}
   * with {@code @JsonTypeInfo}). We embed {@code "type"} directly in the JSON object, so skip the
   * external type wrapper and delegate to {@link #serialize}.
   */
  @Override
  public void serializeWithType(
      FunctionToolCall value,
      JsonGenerator gen,
      SerializationContext provider,
      TypeSerializer typeSer)
      throws tools.jackson.core.JacksonException {
    serialize(value, gen, provider);
  }

  @Override
  public void serialize(FunctionToolCall value, JsonGenerator gen, SerializationContext provider)
      throws tools.jackson.core.JacksonException {
    gen.writeStartObject();
    gen.writeStringProperty("type", "function_call");
    gen.writeStringProperty("call_id", value.callId());
    gen.writeStringProperty("name", value.name());
    gen.writeStringProperty("arguments", value.arguments());
    if (value.id() != null) {
      gen.writeStringProperty("id", value.id());
    }
    if (value.status() != null) {
      gen.writeStringProperty("status", value.status().name().toLowerCase());
    }
    gen.writeEndObject();
  }
}
