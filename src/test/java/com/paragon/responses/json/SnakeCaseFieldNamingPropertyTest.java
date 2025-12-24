package com.paragon.responses.json;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.spec.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.jqwik.api.*;

/**
 * Property-based tests for snake_case field naming in JSON serialization.
 *
 * <p>**Feature: responses-api-jackson-serialization, Property 1: Snake case field naming**
 *
 * <p>Validates: Requirements 1.1, 1.3
 */
public class SnakeCaseFieldNamingPropertyTest {

  private final ObjectMapper objectMapper = ResponsesApiObjectMapper.create();

  /**
   * Property 1: Snake case field naming
   *
   * <p>For any CreateResponse object, when serialized to JSON, all field names at all nesting
   * levels should be in snake_case format.
   *
   * <p>**Validates: Requirements 1.1, 1.3**
   */
  @Property(tries = 100)
  void createResponseFieldsAreSnakeCase(
      @ForAll("createResponses") CreateResponsePayload createResponsePayload) throws Exception {
    // Serialize to JSON
    String json = objectMapper.writeValueAsString(createResponsePayload);

    // Parse back to JsonNode to inspect field names
    JsonNode jsonNode = objectMapper.readTree(json);

    // Verify all field names are snake_case
    assertAllFieldsAreSnakeCase(jsonNode);
  }

  /**
   * Property 1: Snake case field naming (Response variant)
   *
   * <p>For any Response object, when serialized to JSON, all field names at all nesting levels
   * should be in snake_case format.
   *
   * <p>**Validates: Requirements 1.1, 1.3**
   */
  @Property(tries = 100)
  void responseFieldsAreSnakeCase(@ForAll("responses") Response response) throws Exception {
    // Serialize to JSON
    String json = objectMapper.writeValueAsString(response);

    // Parse back to JsonNode to inspect field names
    JsonNode jsonNode = objectMapper.readTree(json);

    // Verify all field names are snake_case
    assertAllFieldsAreSnakeCase(jsonNode);
  }

  /** Recursively checks that all field names in a JsonNode are in snake_case format. */
  private void assertAllFieldsAreSnakeCase(JsonNode node) {
    if (node.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        String fieldName = field.getKey();

        // Check that field name is snake_case
        assertTrue(
            isSnakeCase(fieldName), "Field name '" + fieldName + "' is not in snake_case format");

        // Recursively check nested objects
        assertAllFieldsAreSnakeCase(field.getValue());
      }
    } else if (node.isArray()) {
      for (JsonNode element : node) {
        assertAllFieldsAreSnakeCase(element);
      }
    }
  }

  /**
   * Checks if a string is in snake_case format. Snake case: all lowercase, words separated by
   * underscores, no consecutive underscores.
   */
  private boolean isSnakeCase(String str) {
    if (str == null || str.isEmpty()) {
      return false;
    }

    // Snake case should be all lowercase with underscores separating words
    // No uppercase letters, no consecutive underscores, no leading/trailing underscores
    return str.matches("^[a-z][a-z0-9]*(_[a-z0-9]+)*$");
  }

  // ===== Generators =====

  @Provide
  Arbitrary<CreateResponsePayload> createResponses() {
    return Arbitraries.of(
        // Simple CreateResponse with minimal fields
        new CreateResponsePayload(
            null, // background
            null, // conversation
            null, // include
            null, // input
            null, // instructions
            null, // maxOutputTokens
            null, // maxToolCalls
            null, // metadata
            "gpt-4o", // model
            null, // parallelToolCalls
            null, // prompt
            null, // promptCacheKey
            null, // promptCacheRetention
            null, // reasoning
            null, // safetyIdentifier
            null, // serviceTier
            null, // store
            null, // stream
            null, // streamOptions
            null, // temperature
            null, // text
            null, // toolChoice
            null, // tools
            null, // topLogprobs
            null, // topP
            null, // truncation
            null // openRouterCustomPayload
            ),
        // CreateResponse with camelCase fields that should convert to snake_case
        new CreateResponsePayload(
            true, // background
            "conv-123", // conversation
            null, // include
            null, // input
            "Test instructions", // instructions
            1000, // maxOutputTokens
            5, // maxToolCalls
            Map.of("key1", "value1"), // metadata
            "gpt-4o", // model
            true, // parallelToolCalls
            null, // prompt
            "cache-key-123", // promptCacheKey
            "24h", // promptCacheRetention
            null, // reasoning
            "user-123", // safetyIdentifier
            ServiceTierType.AUTO, // serviceTier
            true, // store
            false, // stream
            null, // streamOptions
            0.7, // temperature
            null, // text
            null, // toolChoice
            null, // tools
            5, // topLogprobs
            0.9, // topP
            Truncation.AUTO, // truncation
            null // openRouterCustomPayload
            ));
  }

  @Provide
  Arbitrary<Response> responses() {
    return Arbitraries.of(
        // Simple Response with minimal fields
        new Response(
            null, // background
            null, // conversation
            null, // createdAt
            null, // error
            "resp-123", // id
            null, // incompleteDetails
            null, // instructions
            null, // maxOutputTokens
            null, // maxToolCalls
            null, // metadata
            "gpt-4o", // model
            null, // object
            null, // output
            null, // parallelToolCalls
            null, // prompt
            null, // promptCacheKey
            null, // promptCacheRetention
            null, // reasoning
            null, // safetyIdentifier
            null, // serviceTier
            null, // status
            null, // temperature
            null, // text
            null, // toolChoice
            null, // tools
            null, // topLogprobs
            null, // topP
            null // truncation
            ),
        // Response with camelCase fields that should convert to snake_case
        new Response(
            true, // background
            null, // conversation
            1638360000L, // createdAt
            null, // error
            "resp-456", // id
            null, // incompleteDetails
            null, // instructions
            1000, // maxOutputTokens
            5, // maxToolCalls
            Map.of("key1", "value1"), // metadata
            "gpt-4o", // model
            ResponseObject.RESPONSE, // object
            List.of(), // output
            true, // parallelToolCalls
            null, // prompt
            "cache-key-456", // promptCacheKey
            "24h", // promptCacheRetention
            null, // reasoning
            "user-456", // safetyIdentifier
            "default", // serviceTier
            ResponseGenerationStatus.COMPLETED, // status
            0.7, // temperature
            null, // text
            null, // toolChoice
            null, // tools
            5, // topLogprobs
            0.9, // topP
            Truncation.AUTO // truncation
            ));
  }
}
