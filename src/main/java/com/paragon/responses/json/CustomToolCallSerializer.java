package com.paragon.responses.json;

import com.paragon.responses.spec.CustomToolCall;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

/** Serializer that enforces the correct wire format for {@link CustomToolCall}. */
public class CustomToolCallSerializer extends StdSerializer<CustomToolCall> {

  public CustomToolCallSerializer() {
    super(CustomToolCall.class);
  }

  @Override
  public void serializeWithType(
      CustomToolCall value,
      JsonGenerator gen,
      SerializationContext provider,
      TypeSerializer typeSer)
      throws tools.jackson.core.JacksonException {
    serialize(value, gen, provider);
  }

  @Override
  public void serialize(CustomToolCall value, JsonGenerator gen, SerializationContext provider)
      throws tools.jackson.core.JacksonException {
    gen.writeStartObject();
    gen.writeStringProperty("type", "custom_tool_call");
    if (value.id() != null) {
      gen.writeStringProperty("id", value.id());
    }
    gen.writeStringProperty("call_id", value.callId());
    gen.writeStringProperty("input", value.input());
    gen.writeStringProperty("name", value.name());
    gen.writeEndObject();
  }
}
