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

    // Add 'required' array with all property names for OpenAI strict mode
    addRequiredProperties(schema);

    // Add 'additionalProperties: false' for strict mode
    schema.put("additionalProperties", false);

    return schema;
  }

  /**
   * Recursively adds 'required' arrays to object schemas. OpenAI strict mode requires all
   * properties to be listed in the 'required' array.
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
