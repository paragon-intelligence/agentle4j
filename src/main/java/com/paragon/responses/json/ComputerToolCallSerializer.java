package com.paragon.responses.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.paragon.responses.spec.ComputerToolCall;
import java.io.IOException;

/** Serializer that enforces the correct wire format for {@link ComputerToolCall}. */
public class ComputerToolCallSerializer extends StdSerializer<ComputerToolCall> {

  public ComputerToolCallSerializer() {
    super(ComputerToolCall.class);
  }

  @Override
  public void serializeWithType(
      ComputerToolCall value,
      JsonGenerator gen,
      SerializerProvider provider,
      TypeSerializer typeSer)
      throws IOException {
    serialize(value, gen, provider);
  }

  @Override
  public void serialize(ComputerToolCall value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartObject();
    gen.writeStringField("type", "computer_call");
    gen.writeStringField("id", value.id());
    gen.writeStringField("call_id", value.callId());
    gen.writeObjectField("action", value.action());
    gen.writeObjectField("pending_safety_checks", value.pendingSafetyChecks());
    gen.writeStringField("status", value.status().name().toLowerCase());
    gen.writeEndObject();
  }
}

