package com.paragon.responses.json;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.spec.*;
import java.util.Iterator;
import java.util.Map;
import net.jqwik.api.*;

/**
 * Property-based tests for null field exclusion in JSON serialization.
 *
 * <p>**Feature: responses-api-jackson-serialization, Property 2: Null field exclusion**
 *
 * <p>Validates: Requirements 1.2
 */
public class NullFieldExclusionPropertyTest {

  private final ObjectMapper objectMapper = ResponsesApiObjectMapper.create();

  /**
   * Property 2: Null field exclusion
   *
   * <p>For any CreateResponse object with null fields, when serialized to JSON, those null fields
   * should not appear in the resulting JSON string.
   *
   * <p>**Validates: Requirements 1.2**
   */
  @Property(tries = 100)
  void createResponseNullFieldsAreExcluded(
      @ForAll("createResponsesWithNulls") CreateResponsePayload createResponsePayload)
      throws Exception {
    // Serialize to JSON
    String json = objectMapper.writeValueAsString(createResponsePayload);

    // Parse back to JsonNode to inspect fields
    JsonNode jsonNode = objectMapper.readTree(json);

    // Verify no null values in JSON
    assertNoNullValues(jsonNode);
  }

  /**
   * Property 2: Null field exclusion (Response variant)
   *
   * <p>For any Response object with null fields, when serialized to JSON, those null fields should
   * not appear in the resulting JSON string.
   *
   * <p>**Validates: Requirements 1.2**
   */
  @Property(tries = 100)
  void responseNullFieldsAreExcluded(@ForAll("responsesWithNulls") Response response)
      throws Exception {
    // Serialize to JSON
    String json = objectMapper.writeValueAsString(response);

    // Parse back to JsonNode to inspect fields
    JsonNode jsonNode = objectMapper.readTree(json);

    // Verify no null values in JSON
    assertNoNullValues(jsonNode);
  }

  /** Recursively checks that no field in a JsonNode has a null value. */
  private void assertNoNullValues(JsonNode node) {
    if (node.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        JsonNode value = field.getValue();

        // Check that value is not null
        assertFalse(
            value.isNull(), "Field '" + field.getKey() + "' should not have null value in JSON");

        // Recursively check nested objects
        assertNoNullValues(value);
      }
    } else if (node.isArray()) {
      for (JsonNode element : node) {
        assertNoNullValues(element);
      }
    }
  }

  // ===== Generators =====

  @Provide
  Arbitrary<CreateResponsePayload> createResponsesWithNulls() {
    return Arbitraries.of(
        // CreateResponse with all null fields
        new CreateResponsePayload(
            null, // background
            null, // conversation
            null, // include
            null, // input
            null, // instructions
            null, // maxOutputTokens
            null, // maxToolCalls
            null, // metadata
            null, // model
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
        // CreateResponse with some null and some non-null fields
        new CreateResponsePayload(
            true, // background
            null, // conversation
            null, // include
            null, // input
            "Test instructions", // instructions
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
            0.7, // temperature
            null, // text
            null, // toolChoice
            null, // tools
            null, // topLogprobs
            null, // topP
            null, // truncation
            null // openRouterCustomPayload
            ),
        // CreateResponse with mostly non-null fields
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
  Arbitrary<Response> responsesWithNulls() {
    return Arbitraries.of(
        // Response with all null fields
        new Response(
            null, // background
            null, // conversation
            null, // createdAt
            null, // error
            null, // id
            null, // incompleteDetails
            null, // instructions
            null, // maxOutputTokens
            null, // maxToolCalls
            null, // metadata
            null, // model
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
        // Response with some null and some non-null fields
        new Response(
            true, // background
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
            ResponseGenerationStatus.COMPLETED, // status
            null, // temperature
            null, // text
            null, // toolChoice
            null, // tools
            null, // topLogprobs
            null, // topP
            null // truncation
            ),
        // Response with mostly non-null fields
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
            null, // output
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
