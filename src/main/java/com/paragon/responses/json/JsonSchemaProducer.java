package com.paragon.responses.json;

import java.util.Map;
import org.jspecify.annotations.NonNull;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

public interface JsonSchemaProducer {
  ObjectMapper TYPE_MAPPER = new ObjectMapper();

  Map<String, Object> produce(JavaType javaType);

  default Map<String, Object> produce(Class<?> clazz) {
    return produce(TYPE_MAPPER.constructType(clazz));
  }

  default <T> @NonNull StructuredOutputDefinition<T> structuredOutputDefinition(
      @NonNull Class<T> clazz) {
    return structuredOutputDefinition(TYPE_MAPPER.constructType(clazz));
  }

  default <T> @NonNull StructuredOutputDefinition<T> structuredOutputDefinition(
      @NonNull TypeReference<T> typeReference) {
    return structuredOutputDefinition(TYPE_MAPPER.constructType(typeReference.getType()));
  }

  default <T> @NonNull StructuredOutputDefinition<T> structuredOutputDefinition(
      @NonNull JavaType javaType) {
    return StructuredOutputDefinition.create(javaType, this);
  }
}
