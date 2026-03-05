package com.paragon.responses.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.paragon.responses.spec.ApplyPatchToolCall;
import java.io.IOException;

/**
 * Custom serializer for {@link ApplyPatchToolCall} that ensures the {@code type} discriminator is
 * always present when used as a {@code ResponseInputItem}.
 */
public class ApplyPatchToolCallSerializer extends StdSerializer<ApplyPatchToolCall> {

  public ApplyPatchToolCallSerializer() {
    super(ApplyPatchToolCall.class);
  }

  @Override
  public void serializeWithType(
      ApplyPatchToolCall value,
      JsonGenerator gen,
      SerializerProvider provider,
      TypeSerializer typeSer)
      throws IOException {
    serialize(value, gen, provider);
  }

  @Override
  public void serialize(ApplyPatchToolCall value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartObject();
    gen.writeStringField("type", "apply_patch_call");
    gen.writeStringField("id", value.id());
    gen.writeStringField("call_id", value.callId());
    gen.writeObjectField("operation", value.operation());
    gen.writeStringField("status", value.status());
    gen.writeEndObject();
  }
}

