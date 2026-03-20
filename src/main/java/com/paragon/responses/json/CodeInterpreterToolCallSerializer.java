package com.paragon.responses.json;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
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
      SerializationContext provider,
      TypeSerializer typeSer)
      throws tools.jackson.core.JacksonException {
    serialize(value, gen, provider);
  }

  @Override
  public void serialize(
      CodeInterpreterToolCall value, JsonGenerator gen, SerializationContext provider)
      throws tools.jackson.core.JacksonException {
    gen.writeStartObject();
    gen.writeStringProperty("type", "code_interpreter_call");
    gen.writeStringProperty("id", value.id());
    gen.writeStringProperty("code", value.code());
    gen.writeStringProperty("container_id", value.containerId());
    gen.writePOJOProperty("outputs", value.outputs());
    gen.writeStringProperty("status", value.status().name().toLowerCase());
    gen.writeEndObject();
  }
}

