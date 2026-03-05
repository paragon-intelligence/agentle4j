package com.paragon.responses.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.paragon.responses.spec.CustomToolCall;
import java.io.IOException;

/** Serializer that enforces the correct wire format for {@link CustomToolCall}. */
public class CustomToolCallSerializer extends StdSerializer<CustomToolCall> {

  public CustomToolCallSerializer() {
    super(CustomToolCall.class);
  }

  @Override
  public void serializeWithType(
      CustomToolCall value,
      JsonGenerator gen,
      SerializerProvider provider,
      TypeSerializer typeSer)
      throws IOException {
    serialize(value, gen, provider);
  }

  @Override
  public void serialize(CustomToolCall value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartObject();
    gen.writeStringField("type", "custom_tool_call");
    if (value.id() != null) {
      gen.writeStringField("id", value.id());
    }
    gen.writeStringField("call_id", value.callId());
    gen.writeStringField("input", value.input());
    gen.writeStringField("name", value.name());
    gen.writeEndObject();
  }
}

