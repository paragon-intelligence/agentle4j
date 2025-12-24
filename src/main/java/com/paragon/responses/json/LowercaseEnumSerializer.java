package com.paragon.responses.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

/**
 * Custom Jackson serializer that converts enum values to lowercase strings. Multi-word enums with
 * underscores preserve the underscores in the output.
 *
 * <p>For example: - MessageRole.USER -> "user" - MessageRole.ASSISTANT -> "assistant" -
 * Status.IN_PROGRESS -> "in_progress"
 */
public class LowercaseEnumSerializer extends JsonSerializer<Enum<?>> {

  @Override
  public void serialize(Enum<?> value, JsonGenerator gen, SerializerProvider serializers)
      throws IOException {
    if (value == null) {
      gen.writeNull();
    } else {
      // Convert enum name to lowercase, preserving underscores
      gen.writeString(value.name().toLowerCase());
    }
  }
}
