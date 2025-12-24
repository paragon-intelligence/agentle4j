package com.paragon.responses.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Custom Jackson deserializer that converts lowercase strings to enum values. Multi-word enums with
 * underscores are handled correctly.
 *
 * <p>For example: - "user" -> MessageRole.USER - "assistant" -> MessageRole.ASSISTANT -
 * "in_progress" -> Status.IN_PROGRESS
 *
 * <p>Throws a descriptive JsonMappingException if the value doesn't match any enum constant.
 */
public class LowercaseEnumDeserializer extends JsonDeserializer<Enum<?>>
    implements ContextualDeserializer {

  private Class<? extends Enum> enumType;

  public LowercaseEnumDeserializer() {
    // Default constructor for Jackson
  }

  public LowercaseEnumDeserializer(Class<? extends Enum> enumType) {
    this.enumType = enumType;
  }

  @Override
  public Enum<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    String value = p.getText();

    if (value == null || value.isEmpty()) {
      return null;
    }

    // Convert the lowercase string to uppercase to match enum constant names
    String upperValue = value.toUpperCase();

    try {
      // Find the matching enum constant
      @SuppressWarnings("unchecked")
      Enum<?> result = Enum.valueOf(enumType, upperValue);
      return result;
    } catch (IllegalArgumentException e) {
      // Provide a descriptive error message with valid values
      String validValues =
          Arrays.stream(enumType.getEnumConstants())
              .map(constant -> constant.name().toLowerCase())
              .collect(Collectors.joining(", "));

      throw new JsonMappingException(
          p,
          String.format(
              "Invalid enum value '%s' for type %s. Valid values are: %s",
              value, enumType.getSimpleName(), validValues));
    }
  }

  @Override
  public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
      throws JsonMappingException {
    Class<?> rawClass = ctxt.getContextualType().getRawClass();

    if (rawClass.isEnum()) {
      @SuppressWarnings("unchecked")
      Class<? extends Enum> enumClass = (Class<? extends Enum>) rawClass;
      return new LowercaseEnumDeserializer(enumClass);
    }

    return this;
  }
}
