package com.paragon.responses.json;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;
import java.io.IOException;

/**
 * Custom Jackson serializer that converts enum values to lowercase strings. Multi-word enums with
 * underscores preserve the underscores in the output.
 *
 * <p>For example: - MessageRole.USER -> "user" - MessageRole.ASSISTANT -> "assistant" -
 * Status.IN_PROGRESS -> "in_progress"
 */
public class LowercaseEnumSerializer extends ValueSerializer<Enum<?>> {

  @Override
  public void serialize(Enum<?> value, JsonGenerator gen, SerializationContext serializers)
      throws tools.jackson.core.JacksonException {
    if (value == null) {
      gen.writeNull();
    } else {
      // Convert enum name to lowercase, preserving underscores
      gen.writeString(value.name().toLowerCase());
    }
  }
}
