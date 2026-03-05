package com.paragon.responses.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.paragon.responses.spec.LocalShellCall;
import java.io.IOException;

/** Serializer that enforces the correct wire format for {@link LocalShellCall}. */
public class LocalShellCallSerializer extends StdSerializer<LocalShellCall> {

  public LocalShellCallSerializer() {
    super(LocalShellCall.class);
  }

  @Override
  public void serializeWithType(
      LocalShellCall value,
      JsonGenerator gen,
      SerializerProvider provider,
      TypeSerializer typeSer)
      throws IOException {
    serialize(value, gen, provider);
  }

  @Override
  public void serialize(LocalShellCall value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartObject();
    gen.writeStringField("type", "local_shell_call");
    gen.writeStringField("id", value.id());
    gen.writeStringField("call_id", value.callId());
    gen.writeObjectField("action", value.action());
    gen.writeStringField("status", value.status());
    gen.writeEndObject();
  }
}

