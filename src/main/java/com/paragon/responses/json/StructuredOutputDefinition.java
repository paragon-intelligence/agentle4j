package com.paragon.responses.json;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class StructuredOutputDefinition<T> {
  public static final String ROOT_WRAPPER_PROPERTY = "value";
  private static final ObjectMapper TYPE_MAPPER = new ObjectMapper();

  private final @NonNull JavaType responseJavaType;
  private final @NonNull Class<T> responseType;
  private final boolean wrapsRoot;
  private final @Nullable Map<String, Object> schema;

  private StructuredOutputDefinition(
      @NonNull JavaType responseJavaType, boolean wrapsRoot, @Nullable Map<String, Object> schema) {
    this.responseJavaType =
        Objects.requireNonNull(responseJavaType, "responseJavaType cannot be null");
    Class<?> rawClass = responseJavaType.getRawClass();
    if (rawClass == null) {
      throw new IllegalArgumentException("Structured output JavaType must have a raw class");
    }
    @SuppressWarnings("unchecked")
    Class<T> typedRawClass = (Class<T>) rawClass;
    this.responseType = typedRawClass;
    this.wrapsRoot = wrapsRoot;
    this.schema = schema != null ? Map.copyOf(schema) : null;
  }

  public static <T> @NonNull StructuredOutputDefinition<T> create(@NonNull Class<T> responseType) {
    return create(TYPE_MAPPER.constructType(responseType));
  }

  public static <T> @NonNull StructuredOutputDefinition<T> create(
      @NonNull TypeReference<T> responseType) {
    return create(TYPE_MAPPER.constructType(responseType.getType()));
  }

  public static <T> @NonNull StructuredOutputDefinition<T> create(@NonNull JavaType responseType) {
    boolean wrapsRoot = requiresRootWrapping(responseType);
    return new StructuredOutputDefinition<>(responseType, wrapsRoot, null);
  }

  public static <T> @NonNull StructuredOutputDefinition<T> create(
      @NonNull Class<T> responseType, @NonNull JsonSchemaProducer schemaProducer) {
    return create(TYPE_MAPPER.constructType(responseType), schemaProducer);
  }

  public static <T> @NonNull StructuredOutputDefinition<T> create(
      @NonNull TypeReference<T> responseType, @NonNull JsonSchemaProducer schemaProducer) {
    return create(TYPE_MAPPER.constructType(responseType.getType()), schemaProducer);
  }

  public static <T> @NonNull StructuredOutputDefinition<T> create(
      @NonNull JavaType responseType, @NonNull JsonSchemaProducer schemaProducer) {
    Map<String, Object> schema = schemaProducer.produce(responseType);
    boolean wrapsRoot = requiresRootWrapping(responseType);
    if (wrapsRoot && !isWrappedRootSchema(schema)) {
      schema = wrapRootSchema(schema);
    }
    return new StructuredOutputDefinition<>(responseType, wrapsRoot, schema);
  }

  public @NonNull Class<T> responseType() {
    return responseType;
  }

  public @NonNull JavaType responseJavaType() {
    return responseJavaType;
  }

  public boolean wrapsRoot() {
    return wrapsRoot;
  }

  public @NonNull Map<String, Object> schema() {
    if (schema == null) {
      throw new IllegalStateException("Structured output schema was not captured");
    }
    return schema;
  }

  public @NonNull T parse(@NonNull String json, @NonNull ObjectMapper objectMapper)
      throws JacksonException {
    if (!wrapsRoot) {
      return objectMapper.readValue(json, responseJavaType);
    }
    JsonNode root = objectMapper.readTree(json);
    JsonNode valueNode = unwrapNode(root);
    return objectMapper.readValue(objectMapper.writeValueAsString(valueNode), responseJavaType);
  }

  public @NonNull T parse(@NonNull JsonNode root, @NonNull ObjectMapper objectMapper)
      throws JacksonException {
    if (!wrapsRoot) {
      return objectMapper.readValue(objectMapper.writeValueAsString(root), responseJavaType);
    }
    return objectMapper.readValue(
        objectMapper.writeValueAsString(unwrapNode(root)), responseJavaType);
  }

  public @Nullable Map<String, Object> unwrapPartialMap(@Nullable Map<String, Object> partialMap) {
    if (partialMap == null || !wrapsRoot) {
      return partialMap;
    }
    Object wrapped = partialMap.get(ROOT_WRAPPER_PROPERTY);
    if (!(wrapped instanceof Map<?, ?> raw)) {
      return partialMap;
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> cast = (Map<String, Object>) raw;
    return cast;
  }

  private static boolean requiresRootWrapping(JavaType responseType) {
    JavaType unwrapped = unwrapReferenceType(responseType);
    Class<?> rawClass = unwrapped.getRawClass();
    if (rawClass == null) {
      return false;
    }

    if (PolymorphicTypeMetadata.resolve(rawClass).isPresent()) {
      return true;
    }

    if (unwrapped.isContainerType()) {
      return !unwrapped.isMapLikeType();
    }

    return !isObjectLikeRawClass(rawClass);
  }

  private static boolean isWrappedRootSchema(Map<String, Object> schema) {
    if (!"object".equals(schema.get("type"))) {
      return false;
    }

    Object propertiesObj = schema.get("properties");
    if (!(propertiesObj instanceof Map<?, ?> rawProperties)) {
      return false;
    }

    return rawProperties.containsKey(ROOT_WRAPPER_PROPERTY);
  }

  private static Map<String, Object> wrapRootSchema(Map<String, Object> innerSchema) {
    return Map.of(
        "type",
        "object",
        "properties",
        Map.of(ROOT_WRAPPER_PROPERTY, innerSchema),
        "required",
        List.of(ROOT_WRAPPER_PROPERTY),
        "additionalProperties",
        false);
  }

  private static JavaType unwrapReferenceType(JavaType responseType) {
    if (responseType.isReferenceType() && responseType.getReferencedType() != null) {
      return unwrapReferenceType(responseType.getReferencedType());
    }
    return responseType;
  }

  private static boolean isObjectLikeRawClass(Class<?> rawClass) {
    return !rawClass.isPrimitive()
        && !rawClass.isEnum()
        && rawClass != Object.class
        && rawClass != String.class
        && !Number.class.isAssignableFrom(rawClass)
        && rawClass != Boolean.class
        && rawClass != Character.class;
  }

  private JsonNode unwrapNode(JsonNode root) {
    JsonNode valueNode = root.get(ROOT_WRAPPER_PROPERTY);
    if (valueNode == null || valueNode.isNull()) {
      throw new IllegalStateException(
          "Structured output for "
              + responseJavaType.toCanonical()
              + " must be wrapped in a root '"
              + ROOT_WRAPPER_PROPERTY
              + "' object");
    }
    return valueNode;
  }
}
