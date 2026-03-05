package com.paragon.responses.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.paragon.responses.spec.CodeInterpreterToolCall;
import java.io.IOException;

/** Serializer that enforces the correct wire format for {@link CodeInterpreterToolCall}. */
public class CodeInterpreterToolCallSerializer extends StdSerializer<CodeInterpreterToolCall> {

  public CodeInterpreterToolCallSerializer() {
    super(CodeInterpreterToolCall.class);
  }

  @Override
  public void serializeWithType(
      CodeInterpreterToolCall value,
      JsonGenerator gen,
      SerializerProvider provider,
      TypeSerializer typeSer)
      throws IOException {
    serialize(value, gen, provider);
  }

  @Override
  public void serialize(
      CodeInterpreterToolCall value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartObject();
    gen.writeStringField("type", "code_interpreter_call");
    gen.writeStringField("id", value.id());
    gen.writeStringField("code", value.code());
    gen.writeStringField("container_id", value.containerId());
    gen.writeObjectField("outputs", value.outputs());
    gen.writeStringField("status", value.status().name().toLowerCase());
    gen.writeEndObject();
  }
}

