package com.paragon.responses.json;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import com.paragon.responses.spec.FunctionShellToolCall;
import java.io.IOException;

/** Serializer that enforces the correct wire format for {@link FunctionShellToolCall}. */
public class FunctionShellToolCallSerializer extends StdSerializer<FunctionShellToolCall> {

  public FunctionShellToolCallSerializer() {
    super(FunctionShellToolCall.class);
  }

  @Override
  public void serializeWithType(
      FunctionShellToolCall value,
      JsonGenerator gen,
      SerializationContext provider,
      TypeSerializer typeSer)
      throws tools.jackson.core.JacksonException {
    serialize(value, gen, provider);
  }

  @Override
  public void serialize(
      FunctionShellToolCall value, JsonGenerator gen, SerializationContext provider)
      throws tools.jackson.core.JacksonException {
    gen.writeStartObject();
    gen.writeStringProperty("type", "function_shell_call");
    // id may be null when the model first emits the call
    if (value.id() != null) {
      gen.writeStringProperty("id", value.id());
    }
    gen.writeStringProperty("call_id", value.callId());
    gen.writePOJOProperty("action", value.action());
    if (value.status() != null) {
      gen.writeStringProperty("status", value.status().name().toLowerCase());
    }
    gen.writeEndObject();
  }
}

