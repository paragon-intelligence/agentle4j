package com.paragon.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import com.paragon.json.ParagonJavaTimeModule;
import com.paragon.responses.json.ResponsesApiModule;

/**
 * Factory class for creating a centrally configured Jackson ObjectMapper for the Responses API
 * specification classes. This ObjectMapper is configured with:
 *
 * <ul>
 *   <li>Snake case naming strategy for JSON field names
 *   <li>Null value exclusion from JSON output
 *   <li>Unknown property tolerance during deserialization
 *   <li>Custom serializers and deserializers for special types
 * </ul>
 */
public final class ResponsesApiObjectMapper {
  private ResponsesApiObjectMapper() {}

  private static ObjectMapper configure(ObjectMapper mapper) {
    return mapper
        .rebuild()
        .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
        .changeDefaultPropertyInclusion(
            ignored -> JsonInclude.Value.empty().withValueInclusion(JsonInclude.Include.NON_NULL))
        .findAndAddModules()
        .addModule(new ParagonJavaTimeModule())
        .addModule(new ResponsesApiModule())
        .build();
  }

  /**
   * Creates and configures a new ObjectMapper instance with all necessary settings for Responses
   * API serialization and deserialization.
   *
   * @return A fully configured ObjectMapper instance
   */
  public static ObjectMapper create() {
    return configure(new ObjectMapper());
  }
}
