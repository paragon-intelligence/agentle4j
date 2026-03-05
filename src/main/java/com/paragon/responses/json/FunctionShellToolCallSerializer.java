package com.paragon.responses.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
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
      SerializerProvider provider,
      TypeSerializer typeSer)
      throws IOException {
    serialize(value, gen, provider);
  }

  @Override
  public void serialize(
      FunctionShellToolCall value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartObject();
    gen.writeStringField("type", "function_shell_call");
    // id may be null when the model first emits the call
    if (value.id() != null) {
      gen.writeStringField("id", value.id());
    }
    gen.writeStringField("call_id", value.callId());
    gen.writeObjectField("action", value.action());
    if (value.status() != null) {
      gen.writeStringField("status", value.status().name().toLowerCase());
    }
    gen.writeEndObject();
  }
}

