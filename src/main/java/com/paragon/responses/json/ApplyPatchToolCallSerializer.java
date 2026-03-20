package com.paragon.responses.json;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
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
      SerializationContext provider,
      TypeSerializer typeSer)
      throws tools.jackson.core.JacksonException {
    serialize(value, gen, provider);
  }

  @Override
  public void serialize(ApplyPatchToolCall value, JsonGenerator gen, SerializationContext provider)
      throws tools.jackson.core.JacksonException {
    gen.writeStartObject();
    gen.writeStringProperty("type", "apply_patch_call");
    gen.writeStringProperty("id", value.id());
    gen.writeStringProperty("call_id", value.callId());
    gen.writePOJOProperty("operation", value.operation());
    gen.writeStringProperty("status", value.status());
    gen.writeEndObject();
  }
}

