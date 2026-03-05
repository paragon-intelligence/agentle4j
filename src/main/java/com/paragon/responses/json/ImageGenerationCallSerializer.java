package com.paragon.responses.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.paragon.responses.spec.ImageGenerationCall;
import java.io.IOException;

/** Serializer that enforces the correct wire format for {@link ImageGenerationCall}. */
public class ImageGenerationCallSerializer extends StdSerializer<ImageGenerationCall> {

  public ImageGenerationCallSerializer() {
    super(ImageGenerationCall.class);
  }

  @Override
  public void serializeWithType(
      ImageGenerationCall value,
      JsonGenerator gen,
      SerializerProvider provider,
      TypeSerializer typeSer)
      throws IOException {
    serialize(value, gen, provider);
  }

  @Override
  public void serialize(ImageGenerationCall value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartObject();
    gen.writeStringField("type", "image_generation_call");
    gen.writeStringField("id", value.id());
    gen.writeStringField("result", value.result());
    gen.writeStringField("status", value.status());
    gen.writeEndObject();
  }
}

