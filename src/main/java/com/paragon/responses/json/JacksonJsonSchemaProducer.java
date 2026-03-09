package com.paragon.responses.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
  public Map<String, Object> produce(Class<?> clazz) {
    var jsonSchemaGenerator = new JsonSchemaGenerator(mapper);

    JsonSchema jsonSchema;
    try {
      jsonSchema = jsonSchemaGenerator.generateSchema(clazz);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    Map<String, Object> schema =
        mapper.convertValue(jsonSchema, new TypeReference<HashMap<String, Object>>() {});

    // Pass 1: collect every sub-schema that carries an "id" URN
    Map<String, Map<String, Object>> idToSchema = new HashMap<>();
    collectIds(schema, idToSchema);

    // Pass 2: replace $ref nodes with deep-copied inlined schemas, strip unsupported keywords
    resolveRefs(schema, idToSchema);

    // Add 'required' array with all property names for OpenAI strict mode
    addRequiredProperties(schema);

    // Add 'additionalProperties: false' for strict mode
    schema.put("additionalProperties", false);

    return schema;
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
   */
  @SuppressWarnings("unchecked")
  private void resolveRefs(Map<String, Object> node, Map<String, Map<String, Object>> idToSchema) {
    List<String> keysToReplace = new ArrayList<>();
    for (Map.Entry<String, Object> entry : node.entrySet()) {
      if (entry.getValue() instanceof Map<?, ?> child) {
        Map<String, Object> childMap = (Map<String, Object>) child;
        if (childMap.containsKey("$ref")) {
          keysToReplace.add(entry.getKey());
        } else {
          resolveRefs(childMap, idToSchema);
        }
      }
    }

    for (String key : keysToReplace) {
      @SuppressWarnings("unchecked")
      Map<String, Object> refNode = (Map<String, Object>) node.get(key);
      String ref = (String) refNode.get("$ref");
      Map<String, Object> resolved = idToSchema.get(ref);
      if (resolved != null) {
        Map<String, Object> copy = deepCopy(resolved);
        resolveRefs(copy, idToSchema);
        node.put(key, copy);
      }
    }

    // Only strip "id" when it is the Jackson URN metadata (e.g. "urn:jsonschema:com:example:Foo"),
    // not when it is a legitimate schema field named "id" inside a properties map.
    if (node.get("id") instanceof String idVal && idVal.startsWith("urn:")) {
      node.remove("id");
    }
    node.remove("$schema");
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
  private void addRequiredProperties(Map<String, Object> schema) {
    Object propertiesObj = schema.get("properties");
    if (propertiesObj instanceof Map) {
      Map<String, Object> properties = (Map<String, Object>) propertiesObj;

      // Add all property names to 'required' array
      if (!properties.isEmpty()) {
        List<String> required = new ArrayList<>(properties.keySet());
        schema.put("required", required);
      }

      // Recursively process nested objects
      for (Object propertyValue : properties.values()) {
        if (propertyValue instanceof Map) {
          Map<String, Object> nestedSchema = (Map<String, Object>) propertyValue;
          if ("object".equals(nestedSchema.get("type"))) {
            addRequiredProperties(nestedSchema);
            nestedSchema.put("additionalProperties", false);
          }
          // Handle array items that are objects
          Object itemsObj = nestedSchema.get("items");
          if (itemsObj instanceof Map) {
            Map<String, Object> itemsSchema = (Map<String, Object>) itemsObj;
            if ("object".equals(itemsSchema.get("type"))) {
              addRequiredProperties(itemsSchema);
              itemsSchema.put("additionalProperties", false);
            }
          }
        }
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
