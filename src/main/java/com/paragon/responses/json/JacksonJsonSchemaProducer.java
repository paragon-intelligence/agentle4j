package com.paragon.responses.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.module.jsonSchema.JsonSchema;
import tools.jackson.module.jsonSchema.JsonSchemaGenerator;

/**
 * Produces JSON schemas compatible with OpenAI's function calling API.
 *
 * <p>When using strict mode, OpenAI requires:
 *
 * <ul>
 *   <li>{@code type: "object"}
 *   <li>{@code properties: {...}}
 *   <li>{@code required: [...]} - array of all property names
 *   <li>{@code additionalProperties: false}
 * </ul>
 */
public record JacksonJsonSchemaProducer(@NonNull ObjectMapper mapper)
    implements JsonSchemaProducer {

  @Override
  public Map<String, Object> produce(JavaType javaType) {
    JavaType rootType = unwrapReferenceType(javaType);
    Class<?> rawClass = rootType.getRawClass();
    Map<String, Object> schema;
    if (rawClass != null && PolymorphicTypeMetadata.resolve(rawClass).isPresent()) {
      schema = buildRootWrapperSchema(rawClass);
    } else {
      schema = generateResolvedSchema(rootType);
      transformSchemaForType(rootType, schema, new HashSet<>());
    }

    finalizeStrictSchema(schema);
    return schema;
  }

  private Map<String, Object> generateResolvedSchema(JavaType javaType) {
    var jsonSchemaGenerator = new JsonSchemaGenerator(mapper);

    JsonSchema jsonSchema;
    try {
      jsonSchema = jsonSchemaGenerator.generateSchema(javaType);
    } catch (JacksonException e) {
      throw new RuntimeException(e);
    }

    Map<String, Object> schema =
        mapper.convertValue(jsonSchema, new TypeReference<HashMap<String, Object>>() {});

    Map<String, Map<String, Object>> idToSchema = new HashMap<>();
    collectIds(schema, idToSchema);
    resolveRefs(schema, idToSchema, new HashSet<>());
    return schema;
  }

  private JavaType unwrapReferenceType(JavaType javaType) {
    if (javaType.isReferenceType() && javaType.getReferencedType() != null) {
      return unwrapReferenceType(javaType.getReferencedType());
    }
    return javaType;
  }

  private Map<String, Object> buildRootWrapperSchema(Class<?> clazz) {
    PolymorphicTypeMetadata metadata =
        PolymorphicTypeMetadata.resolve(clazz)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Polymorphic structured output metadata not found for " + clazz.getName()));

    Map<String, Object> schema = new HashMap<>();
    schema.put("type", "object");
    Map<String, Object> properties = new HashMap<>();
    properties.put(
        StructuredOutputDefinition.ROOT_WRAPPER_PROPERTY,
        buildPolymorphicUnionSchema(metadata, new HashSet<>()));
    schema.put("properties", properties);
    return schema;
  }

  @SuppressWarnings("unchecked")
  private void transformSchemaForType(
      JavaType javaType, Map<String, Object> schema, Set<Class<?>> resolvingTypes) {
    if (javaType == null || schema == null) {
      return;
    }

    if (javaType.isReferenceType() && javaType.getReferencedType() != null) {
      transformSchemaForType(javaType.getReferencedType(), schema, resolvingTypes);
      return;
    }

    if (javaType.isContainerType()) {
      Object itemsObj = schema.get("items");
      if (itemsObj instanceof Map<?, ?> rawItems && javaType.getContentType() != null) {
        Map<String, Object> items = (Map<String, Object>) rawItems;
        schema.put(
            "items", transformNestedSchema(javaType.getContentType(), items, resolvingTypes));
      }
      return;
    }

    Class<?> rawClass = javaType.getRawClass();
    if (rawClass == null || !shouldTraverseObjectProperties(rawClass) || !isObjectSchema(schema)) {
      return;
    }

    if (!resolvingTypes.add(rawClass)) {
      return;
    }

    try {
      Object propertiesObj = schema.get("properties");
      if (!(propertiesObj instanceof Map<?, ?> rawProperties)) {
        return;
      }

      Map<String, Object> properties = (Map<String, Object>) rawProperties;
      for (Map.Entry<String, JavaType> property : resolvePropertyTypes(javaType).entrySet()) {
        Object existingSchema = properties.get(property.getKey());
        if (!(existingSchema instanceof Map<?, ?> rawPropertySchema)) {
          continue;
        }

        Map<String, Object> propertySchema = (Map<String, Object>) rawPropertySchema;
        properties.put(
            property.getKey(),
            transformNestedSchema(property.getValue(), propertySchema, resolvingTypes));
      }
    } finally {
      resolvingTypes.remove(rawClass);
    }
  }

  private Map<String, Object> transformNestedSchema(
      JavaType javaType, Map<String, Object> existingSchema, Set<Class<?>> resolvingTypes) {
    if (javaType == null) {
      return existingSchema;
    }

    if (javaType.isReferenceType() && javaType.getReferencedType() != null) {
      return transformNestedSchema(javaType.getReferencedType(), existingSchema, resolvingTypes);
    }

    Class<?> rawClass = javaType.getRawClass();
    if (rawClass == null) {
      return existingSchema;
    }

    if (PolymorphicTypeMetadata.resolve(rawClass).isPresent()) {
      return buildPolymorphicUnionSchema(
          PolymorphicTypeMetadata.resolve(rawClass).orElseThrow(), new HashSet<>(resolvingTypes));
    }

    if (javaType.isContainerType()) {
      transformSchemaForType(javaType, existingSchema, resolvingTypes);
      return existingSchema;
    }

    if (!shouldTraverseObjectProperties(rawClass) || !isObjectSchema(existingSchema)) {
      return existingSchema;
    }

    transformSchemaForType(javaType, existingSchema, resolvingTypes);
    return existingSchema;
  }

  private Map<String, Object> buildPolymorphicUnionSchema(
      PolymorphicTypeMetadata metadata, Set<Class<?>> resolvingTypes) {
    Map<String, Object> unionSchema = new HashMap<>();
    List<Map<String, Object>> branches = new ArrayList<>();

    for (PolymorphicTypeMetadata.Branch branch : metadata.branches()) {
      Map<String, Object> branchSchema =
          generateResolvedSchema(mapper.constructType(branch.subtypeClass()));
      transformSchemaForType(
          mapper.constructType(branch.subtypeClass()), branchSchema, new HashSet<>(resolvingTypes));

      if (!isObjectSchema(branchSchema)) {
        throw new IllegalArgumentException(
            "Polymorphic structured output only supports object subtypes, but "
                + branch.subtypeClass().getName()
                + " produced "
                + branchSchema.get("type"));
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> properties =
          (Map<String, Object>)
              branchSchema.computeIfAbsent("properties", ignored -> new HashMap<>());
      properties.put(metadata.propertyName(), discriminatorSchema(branch.typeId()));
      branches.add(branchSchema);
    }

    unionSchema.put("anyOf", branches);
    return unionSchema;
  }

  private Map<String, Object> discriminatorSchema(String typeId) {
    Map<String, Object> schema = new HashMap<>();
    schema.put("type", "string");
    schema.put("enum", List.of(typeId));
    return schema;
  }

  private boolean shouldTraverseObjectProperties(Class<?> rawClass) {
    return !rawClass.isPrimitive()
        && !rawClass.isEnum()
        && rawClass != Object.class
        && rawClass != String.class
        && !Number.class.isAssignableFrom(rawClass)
        && rawClass != Boolean.class
        && rawClass != Character.class
        && !Map.class.isAssignableFrom(rawClass);
  }

  private boolean isObjectSchema(Map<String, Object> schema) {
    return "object".equals(schema.get("type")) || schema.containsKey("properties");
  }

  private Map<String, JavaType> resolvePropertyTypes(JavaType javaType) {
    Map<String, JavaType> propertyTypes = new LinkedHashMap<>();
    Class<?> rawClass = javaType.getRawClass();
    if (rawClass == null) {
      return propertyTypes;
    }

    if (rawClass.isRecord()) {
      RecordComponent[] components = rawClass.getRecordComponents();
      if (components != null) {
        for (RecordComponent component : components) {
          propertyTypes.putIfAbsent(
              resolvePropertyName(component.getName(), component.getAnnotation(JsonProperty.class)),
              mapper.constructType(component.getGenericType()));
        }
      }
    }

    for (Class<?> current = rawClass;
        current != null && current != Object.class;
        current = current.getSuperclass()) {
      for (Field field : current.getDeclaredFields()) {
        if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
          continue;
        }
        propertyTypes.putIfAbsent(
            resolvePropertyName(field.getName(), field.getAnnotation(JsonProperty.class)),
            mapper.constructType(field.getGenericType()));
      }
    }

    for (Method method : rawClass.getMethods()) {
      if (Modifier.isStatic(method.getModifiers())
          || method.isSynthetic()
          || method.getParameterCount() != 0
          || method.getReturnType() == Void.TYPE
          || method.getDeclaringClass() == Object.class) {
        continue;
      }

      String propertyName = resolveGetterPropertyName(method);
      if (propertyName == null || propertyName.equals("class")) {
        continue;
      }

      propertyTypes.putIfAbsent(propertyName, mapper.constructType(method.getGenericReturnType()));
    }

    return propertyTypes;
  }

  private String resolveGetterPropertyName(Method method) {
    JsonProperty jsonProperty = method.getAnnotation(JsonProperty.class);
    if (jsonProperty != null && jsonProperty.value() != null && !jsonProperty.value().isBlank()) {
      return jsonProperty.value();
    }

    String name = method.getName();
    if (name.startsWith("get") && name.length() > 3) {
      return decapitalize(name.substring(3));
    }
    if (name.startsWith("is") && name.length() > 2) {
      return decapitalize(name.substring(2));
    }
    return null;
  }

  private String resolvePropertyName(String fallback, JsonProperty jsonProperty) {
    if (jsonProperty != null && jsonProperty.value() != null && !jsonProperty.value().isBlank()) {
      return jsonProperty.value();
    }
    return fallback;
  }

  private String decapitalize(String value) {
    if (value.isEmpty()) {
      return value;
    }
    if (value.length() > 1 && Character.isUpperCase(value.charAt(1))) {
      return value;
    }
    return Character.toLowerCase(value.charAt(0)) + value.substring(1);
  }

  /**
   * Pass 1: walks the entire schema tree and registers every sub-schema that carries an {@code id}
   * field (jackson-module-jsonSchema URN style, e.g. {@code "urn:jsonschema:com:example:Foo"}).
   */
  @SuppressWarnings("unchecked")
  private void collectIds(Object node, Map<String, Map<String, Object>> idToSchema) {
    if (!(node instanceof Map<?, ?> raw)) return;
    Map<String, Object> map = (Map<String, Object>) raw;
    if (map.get("id") instanceof String id) {
      idToSchema.put(id, map);
    }
    for (Object value : map.values()) {
      collectIds(value, idToSchema);
    }
  }

  /**
   * Pass 2: replaces every {@code {"$ref": "urn:..."}} node with a deep copy of the referenced
   * schema, and strips {@code id} and {@code $schema} from every node (unsupported by OpenAI).
   *
   * <p>{@code resolving} tracks URNs currently on the DFS stack. When a {@code $ref} points to a
   * URN that is already being resolved (circular reference), we replace it with an empty-object
   * fallback instead of recursing, breaking the cycle. OpenAI strict mode cannot represent
   * recursive schemas, so the fallback is the only safe option.
   */
  @SuppressWarnings("unchecked")
  private void resolveRefs(
      Map<String, Object> node,
      Map<String, Map<String, Object>> idToSchema,
      Set<String> resolving) {
    List<String> keysToReplace = new ArrayList<>();
    for (Map.Entry<String, Object> entry : node.entrySet()) {
      if (entry.getValue() instanceof Map<?, ?> child) {
        Map<String, Object> childMap = (Map<String, Object>) child;
        if (childMap.containsKey("$ref")) {
          keysToReplace.add(entry.getKey());
        } else {
          resolveRefs(childMap, idToSchema, resolving);
        }
      }
    }

    for (String key : keysToReplace) {
      @SuppressWarnings("unchecked")
      Map<String, Object> refNode = (Map<String, Object>) node.get(key);
      String ref = (String) refNode.get("$ref");

      if (resolving.contains(ref)) {
        // Circular reference detected — replace with an empty object to break the cycle.
        // OpenAI strict mode does not support recursive schemas.
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("type", "object");
        fallback.put("additionalProperties", false);
        fallback.put("properties", new HashMap<>());
        fallback.put("required", new ArrayList<>());
        node.put(key, fallback);
      } else {
        Map<String, Object> resolved = idToSchema.get(ref);
        if (resolved != null) {
          resolving.add(ref);
          Map<String, Object> copy = deepCopy(resolved);
          resolveRefs(copy, idToSchema, resolving);
          resolving.remove(ref);
          node.put(key, copy);
        }
      }
    }

    // Only strip "id" when it is the Jackson URN metadata (e.g. "urn:jsonschema:com:example:Foo"),
    // not when it is a legitimate schema field named "id" inside a properties map.
    if (node.get("id") instanceof String idVal && idVal.startsWith("urn:")) {
      node.remove("id");
    }
    node.remove("$schema");

    // Jackson's draft-3 schema puts "required": true on individual property schemas to mark them
    // as required. OpenAI strict mode (draft-7 style) expects "required" to be an array on the
    // parent object — never a boolean. Remove any boolean "required" from every node; the correct
    // array form is added by addRequiredProperties() after this pass.
    if (node.get("required") instanceof Boolean) {
      node.remove("required");
    }
  }

  /** Shallow-enough deep copy: new HashMap at every level, preserving leaf values. */
  @SuppressWarnings("unchecked")
  private Map<String, Object> deepCopy(Map<String, Object> source) {
    Map<String, Object> copy = new HashMap<>();
    for (Map.Entry<String, Object> e : source.entrySet()) {
      if (e.getValue() instanceof Map<?, ?> m) {
        copy.put(e.getKey(), deepCopy((Map<String, Object>) m));
      } else if (e.getValue() instanceof List<?> l) {
        copy.put(e.getKey(), new ArrayList<>(l));
      } else {
        copy.put(e.getKey(), e.getValue());
      }
    }
    return copy;
  }

  /**
   * Recursively adds {@code required} arrays to object schemas and sets {@code
   * additionalProperties: false}. OpenAI strict mode requires all properties to be listed in the
   * {@code required} array.
   */
  @SuppressWarnings("unchecked")
  private void finalizeStrictSchema(Object node) {
    if (node instanceof Map<?, ?> rawMap) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) rawMap;

      if ("object".equals(map.get("type"))) {
        @SuppressWarnings("unchecked")
        Map<String, Object> properties =
            (Map<String, Object>) map.computeIfAbsent("properties", ignored -> new HashMap<>());
        map.put("required", new ArrayList<>(properties.keySet()));
        map.put("additionalProperties", false);
      }

      for (Object value : map.values()) {
        finalizeStrictSchema(value);
      }
    } else if (node instanceof List<?> list) {
      for (Object value : list) {
        finalizeStrictSchema(value);
      }
    }
  }

  public static class Builder {
    private @Nullable ObjectMapper mapper = null;

    public Builder mapper(@Nullable ObjectMapper mapper) {
      this.mapper = mapper;
      return this;
    }

    public JsonSchemaProducer build() {
      return new JacksonJsonSchemaProducer(Objects.requireNonNullElse(mapper, new ObjectMapper()));
    }
  }
}
