package com.paragon.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
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
public class ResponsesApiObjectMapper extends ObjectMapper {
  private ResponsesApiObjectMapper() {
    // Configure snake_case naming strategy for JSON field names
    // This converts camelCase Java field names to snake_case JSON field names
    super.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    // Ignore unknown properties during deserialization
    // This allows the system to handle API responses with additional fields
    super.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // Accept case-insensitive enum values (e.g., "in_progress" → IN_PROGRESS)
    // The API returns lowercase snake_case enum values
    super.enable(com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

    // Exclude null values from JSON output
    // This ensures that null fields are not included in serialized JSON
    super.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);

    // Register JDK8 module for Optional, LocalDate, etc.
    super.findAndRegisterModules();

    // Register custom ResponsesApiModule with enum and coordinate serializers
    super.registerModule(new ResponsesApiModule());
  }

  /**
   * Creates and configures a new ObjectMapper instance with all necessary settings for Responses
   * API serialization and deserialization.
   *
   * @return A fully configured ObjectMapper instance
   */
  public static ObjectMapper create() {
    return new ResponsesApiObjectMapper();
  }
}
