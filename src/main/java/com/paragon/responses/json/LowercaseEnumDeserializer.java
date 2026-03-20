package com.paragon.responses.json;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.JavaType;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Custom Jackson deserializer that converts lowercase strings to enum values. Multi-word enums with
 * underscores are handled correctly.
 *
 * <p>For example: - "user" -> MessageRole.USER - "assistant" -> MessageRole.ASSISTANT -
 * "in_progress" -> Status.IN_PROGRESS
 *
 * <p>Throws a descriptive DatabindException if the value doesn't match any enum constant.
 */
public class LowercaseEnumDeserializer extends ValueDeserializer<Enum<?>>
     {

  private Class<? extends Enum<?>> enumType;

  public LowercaseEnumDeserializer() {
    // Default constructor for Jackson
  }

  public LowercaseEnumDeserializer(Class<? extends Enum<?>> enumType) {
    this.enumType = enumType;
  }

  @Override
  public Enum<?> deserialize(JsonParser p, DeserializationContext ctxt) throws tools.jackson.core.JacksonException {
    if (enumType == null) {
      throw DatabindException.from(
          p, "LowercaseEnumDeserializer requires an enum type from contextual deserialization");
    }

    String value = p.getText();

    if (value == null || value.isEmpty()) {
      return null;
    }

    // Convert the lowercase string to uppercase to match enum constant names
    String upperValue = value.toUpperCase();

    try {
      // Find the matching enum constant
      @SuppressWarnings({"rawtypes", "unchecked"})
      Enum<?> result = Enum.valueOf((Class) enumType, upperValue);
      return result;
    } catch (IllegalArgumentException e) {
      // Provide a descriptive error message with valid values
      String validValues =
          Arrays.stream(enumType.getEnumConstants())
              .map(constant -> constant.name().toLowerCase())
              .collect(Collectors.joining(", "));

      throw DatabindException.from(
          p,
          String.format(
              "Invalid enum value '%s' for type %s. Valid values are: %s",
              value, enumType.getSimpleName(), validValues),
          e);
    }
  }

  @Override
  public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
      throws DatabindException {
    JavaType contextualType = ctxt.getContextualType();
    if (contextualType == null && property != null) {
      contextualType = property.getType();
    }
    if (contextualType == null) {
      return this;
    }

    Class<?> rawClass = contextualType.getRawClass();

    if (rawClass.isEnum()) {
      @SuppressWarnings("unchecked")
      Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) rawClass;
      return new LowercaseEnumDeserializer(enumClass);
    }

    return this;
  }
}
