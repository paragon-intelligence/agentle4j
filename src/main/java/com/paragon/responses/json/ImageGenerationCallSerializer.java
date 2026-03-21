package com.paragon.responses.json;

import com.paragon.responses.spec.ImageGenerationCall;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

/** Serializer that enforces the correct wire format for {@link ImageGenerationCall}. */
public class ImageGenerationCallSerializer extends StdSerializer<ImageGenerationCall> {

  public ImageGenerationCallSerializer() {
    super(ImageGenerationCall.class);
  }

  @Override
  public void serializeWithType(
      ImageGenerationCall value,
      JsonGenerator gen,
      SerializationContext provider,
      TypeSerializer typeSer)
      throws tools.jackson.core.JacksonException {
    serialize(value, gen, provider);
  }

  @Override
  public void serialize(ImageGenerationCall value, JsonGenerator gen, SerializationContext provider)
      throws tools.jackson.core.JacksonException {
    gen.writeStartObject();
    gen.writeStringProperty("type", "image_generation_call");
    gen.writeStringProperty("id", value.id());
    gen.writeStringProperty("result", value.result());
    gen.writeStringProperty("status", value.status());
    gen.writeEndObject();
  }
}
