package com.paragon.responses.json;

import com.paragon.responses.spec.ComputerToolCall;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

/** Serializer that enforces the correct wire format for {@link ComputerToolCall}. */
public class ComputerToolCallSerializer extends StdSerializer<ComputerToolCall> {

  public ComputerToolCallSerializer() {
    super(ComputerToolCall.class);
  }

  @Override
  public void serializeWithType(
      ComputerToolCall value,
      JsonGenerator gen,
      SerializationContext provider,
      TypeSerializer typeSer)
      throws tools.jackson.core.JacksonException {
    serialize(value, gen, provider);
  }

  @Override
  public void serialize(ComputerToolCall value, JsonGenerator gen, SerializationContext provider)
      throws tools.jackson.core.JacksonException {
    gen.writeStartObject();
    gen.writeStringProperty("type", "computer_call");
    gen.writeStringProperty("id", value.id());
    gen.writeStringProperty("call_id", value.callId());
    gen.writePOJOProperty("action", value.action());
    gen.writePOJOProperty("pending_safety_checks", value.pendingSafetyChecks());
    gen.writeStringProperty("status", value.status().name().toLowerCase());
    gen.writeEndObject();
  }
}
