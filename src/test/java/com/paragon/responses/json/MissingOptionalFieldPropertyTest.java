package com.paragon.responses.json;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.spec.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.jqwik.api.*;

/**
 * Property-based tests for missing optional field handling in JSON deserialization.
 *
 * <p>**Feature: responses-api-jackson-serialization, Property 5: Missing optional field handling**
 *
 * <p>Validates: Requirements 2.4
 */
public class MissingOptionalFieldPropertyTest {

  private final ObjectMapper objectMapper = ResponsesApiObjectMapper.create();

  /**
   * Property 5: Missing optional field handling
   *
   * <p>For any Response JSON with optional fields omitted, deserialization should succeed and set
   * those fields to null in the resulting object.
   *
   * <p>**Validates: Requirements 2.4**
   */
  @Property(tries = 100)
  void responseWithMissingOptionalFieldsDeserializesSuccessfully(
      @ForAll("responses") Response original,
      @ForAll("fieldNamesToRemove") List<String> fieldsToRemove)
      throws Exception {

    // Serialize original to JSON
    String originalJson = objectMapper.writeValueAsString(original);

    // Parse to JsonNode and remove specified optional fields
    ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(originalJson);
    for (String fieldName : fieldsToRemove) {
      jsonNode.remove(fieldName);
    }

    // Convert back to JSON string with missing fields
    String jsonWithMissingFields = objectMapper.writeValueAsString(jsonNode);

    // Deserialize - should succeed despite missing optional fields
    Response deserialized = objectMapper.readValue(jsonWithMissingFields, Response.class);

    // Verify deserialization succeeded
    assertNotNull(deserialized, "Deserialization should succeed with missing optional fields");

    // Verify that fields that were removed are now null in deserialized object
    for (String fieldName : fieldsToRemove) {
      assertFieldIsNull(deserialized, fieldName);
    }
  }

  /**
   * Property 5: Missing optional field handling (CreateResponse variant)
   *
   * <p>For any CreateResponse JSON with optional fields omitted, deserialization should succeed and
   * set those fields to null in the resulting object.
   *
   * <p>**Validates: Requirements 2.4**
   */
  @Property(tries = 100)
  void createResponseWithMissingOptionalFieldsDeserializesSuccessfully(
      @ForAll("createResponses") CreateResponsePayload original,
      @ForAll("fieldNamesToRemove") List<String> fieldsToRemove)
      throws Exception {

    // Serialize original to JSON
    String originalJson = objectMapper.writeValueAsString(original);

    // Parse to JsonNode and remove specified optional fields
    ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(originalJson);
    for (String fieldName : fieldsToRemove) {
      jsonNode.remove(fieldName);
    }

    // Convert back to JSON string with missing fields
    String jsonWithMissingFields = objectMapper.writeValueAsString(jsonNode);

    // Deserialize - should succeed despite missing optional fields
    CreateResponsePayload deserialized =
        objectMapper.readValue(jsonWithMissingFields, CreateResponsePayload.class);

    // Verify deserialization succeeded
    assertNotNull(deserialized, "Deserialization should succeed with missing optional fields");

    // Verify that fields that were removed are now null in deserialized object
    for (String fieldName : fieldsToRemove) {
      assertFieldIsNull(deserialized, fieldName);
    }
  }

  /**
   * Property 5: Minimal JSON deserialization
   *
   * <p>Tests that a minimal JSON object (with only required fields or even empty) can be
   * deserialized successfully.
   */
  @Property(tries = 100)
  void minimalResponseJsonDeserializesSuccessfully() throws Exception {
    // Create minimal JSON - empty object
    String minimalJson = "{}";

    // Deserialize - should succeed
    Response deserialized = objectMapper.readValue(minimalJson, Response.class);

    // Verify deserialization succeeded with all fields null
    assertNotNull(deserialized, "Deserialization should succeed with minimal JSON");
    assertNull(deserialized.id(), "Optional field should be null");
    assertNull(deserialized.model(), "Optional field should be null");
    assertNull(deserialized.status(), "Optional field should be null");
  }

  /**
   * Property 5: Minimal CreateResponse JSON deserialization
   *
   * <p>Tests that a minimal CreateResponse JSON object can be deserialized successfully.
   */
  @Property(tries = 100)
  void minimalCreateResponseJsonDeserializesSuccessfully() throws Exception {
    // Create minimal JSON - empty object
    String minimalJson = "{}";

    // Deserialize - should succeed
    CreateResponsePayload deserialized =
        objectMapper.readValue(minimalJson, CreateResponsePayload.class);

    // Verify deserialization succeeded with all fields null
    assertNotNull(deserialized, "Deserialization should succeed with minimal JSON");
    assertNull(deserialized.model(), "Optional field should be null");
    assertNull(deserialized.instructions(), "Optional field should be null");
    assertNull(deserialized.maxOutputTokens(), "Optional field should be null");
  }

  /** Verifies that a specific field in a Response object is null. */
  private void assertFieldIsNull(Response response, String fieldName) {
    Object fieldValue = getFieldValue(response, fieldName);
    assertNull(
        fieldValue, "Field '" + fieldName + "' should be null after being removed from JSON");
  }

  /** Verifies that a specific field in a CreateResponse object is null. */
  private void assertFieldIsNull(CreateResponsePayload createResponsePayload, String fieldName) {
    Object fieldValue = getFieldValue(createResponsePayload, fieldName);
    assertNull(
        fieldValue, "Field '" + fieldName + "' should be null after being removed from JSON");
  }

  /** Gets the value of a field from a Response object using reflection-like access. */
  private Object getFieldValue(Response response, String fieldName) {
    return switch (fieldName) {
      case "background" -> response.background();
      case "conversation" -> response.conversation();
      case "created_at" -> response.createdAt();
      case "error" -> response.error();
      case "id" -> response.id();
      case "incomplete_details" -> response.incompleteDetails();
      case "instructions" -> response.instructions();
      case "max_output_tokens" -> response.maxOutputTokens();
      case "max_tool_calls" -> response.maxToolCalls();
      case "metadata" -> response.metadata();
      case "model" -> response.model();
      case "object" -> response.object();
      case "output" -> response.output();
      case "parallel_tool_calls" -> response.parallelToolCalls();
      case "prompt" -> response.prompt();
      case "prompt_cache_key" -> response.promptCacheKey();
      case "prompt_cache_retention" -> response.promptCacheRetention();
      case "reasoning" -> response.reasoning();
      case "safety_identifier" -> response.safetyIdentifier();
      case "service_tier" -> response.serviceTier();
      case "status" -> response.status();
      case "temperature" -> response.temperature();
      case "text" -> response.text();
      case "tool_choice" -> response.toolChoice();
      case "tools" -> response.tools();
      case "top_logprobs" -> response.topLogprobs();
      case "top_p" -> response.topP();
      case "truncation" -> response.truncation();
      default -> null;
    };
  }

  /** Gets the value of a field from a CreateResponse object using reflection-like access. */
  private Object getFieldValue(CreateResponsePayload createResponsePayload, String fieldName) {
    return switch (fieldName) {
      case "background" -> createResponsePayload.background();
      case "conversation" -> createResponsePayload.conversation();
      case "include" -> createResponsePayload.include();
      case "input" -> createResponsePayload.input();
      case "instructions" -> createResponsePayload.instructions();
      case "max_output_tokens" -> createResponsePayload.maxOutputTokens();
      case "max_tool_calls" -> createResponsePayload.maxToolCalls();
      case "metadata" -> createResponsePayload.metadata();
      case "model" -> createResponsePayload.model();
      case "parallel_tool_calls" -> createResponsePayload.parallelToolCalls();
      case "prompt" -> createResponsePayload.prompt();
      case "prompt_cache_key" -> createResponsePayload.promptCacheKey();
      case "prompt_cache_retention" -> createResponsePayload.promptCacheRetention();
      case "reasoning" -> createResponsePayload.reasoning();
      case "safety_identifier" -> createResponsePayload.safetyIdentifier();
      case "service_tier" -> createResponsePayload.serviceTier();
      case "store" -> createResponsePayload.store();
      case "stream" -> createResponsePayload.stream();
      case "stream_options" -> createResponsePayload.streamOptions();
      case "temperature" -> createResponsePayload.temperature();
      case "text" -> createResponsePayload.text();
      case "tool_choice" -> createResponsePayload.toolChoice();
      case "tools" -> createResponsePayload.tools();
      case "top_logprobs" -> createResponsePayload.topLogprobs();
      case "top_p" -> createResponsePayload.topP();
      case "truncation" -> createResponsePayload.truncation();
      default -> null;
    };
  }

  // ===== Generators =====

  @Provide
  Arbitrary<Response> responses() {
    return Arbitraries.of(
        // Response with various fields populated
        new Response(
            true,
            new Conversation("conv-123"),
            1638360000L,
            null,
            "resp-456",
            null,
            null,
            1000,
            5,
            Map.of("key1", "value1"),
            "gpt-4o",
            ResponseObject.RESPONSE,
            null,
            true,
            null,
            "cache-key-456",
            "24h",
            null,
            "user-456",
            "default",
            ResponseGenerationStatus.COMPLETED,
            0.7,
            null,
            null,
            null,
            5,
            0.9,
            Truncation.AUTO),
        // Response with different field combinations
        new Response(
            false,
            null,
            1638360100L,
            null,
            "resp-789",
            null,
            null,
            2000,
            10,
            null,
            "gpt-4o-mini",
            ResponseObject.RESPONSE,
            null,
            false,
            null,
            null,
            null,
            null,
            null,
            "flex",
            ResponseGenerationStatus.IN_PROGRESS,
            1.2,
            null,
            null,
            null,
            null,
            null,
            Truncation.DISABLED),
        // Response with many fields populated
        new Response(
            true,
            new Conversation("conv-999"),
            1638360200L,
            null,
            "resp-111",
            null,
            null,
            1500,
            8,
            Map.of("key2", "value2", "key3", "value3"),
            "gpt-4o",
            ResponseObject.RESPONSE,
            null,
            true,
            null,
            "cache-key-789",
            "12h",
            null,
            "user-789",
            "default",
            ResponseGenerationStatus.COMPLETED,
            0.5,
            null,
            null,
            null,
            3,
            0.95,
            Truncation.AUTO));
  }

  @Provide
  Arbitrary<CreateResponsePayload> createResponses() {
    return Arbitraries.of(
        // CreateResponse with various fields populated
        new CreateResponsePayload(
            true,
            new Conversation("conv-123").id(),
            null,
            null,
            "Test instructions",
            1000,
            5,
            Map.of("key1", "value1"),
            "gpt-4o",
            true,
            null,
            "cache-key-123",
            "24h",
            null,
            "user-123",
            ServiceTierType.AUTO,
            true,
            false,
            null,
            0.7,
            null,
            null,
            null,
            5,
            0.9,
            Truncation.AUTO,
            null),
        // CreateResponse with different field combinations
        new CreateResponsePayload(
            false,
            new Conversation("conv-456").id(),
            null,
            null,
            "Different instructions",
            2000,
            10,
            null,
            "gpt-4o-mini",
            false,
            null,
            null,
            null,
            null,
            null,
            ServiceTierType.DEFAULT,
            false,
            true,
            null,
            1.5,
            null,
            null,
            null,
            null,
            1.0,
            Truncation.DISABLED,
            null),
        // CreateResponse with many fields populated
        new CreateResponsePayload(
            true,
            new Conversation("conv-789").id(),
            null,
            null,
            "More instructions",
            1500,
            8,
            Map.of("key2", "value2"),
            "gpt-4o",
            true,
            null,
            "cache-key-789",
            "12h",
            null,
            "user-789",
            ServiceTierType.AUTO,
            true,
            false,
            null,
            0.5,
            null,
            null,
            null,
            3,
            0.95,
            Truncation.AUTO,
            null));
  }

  /**
   * Generates lists of field names to remove from JSON. These represent optional fields that can be
   * omitted.
   */
  @Provide
  Arbitrary<List<String>> fieldNamesToRemove() {
    // List of optional fields in Response and CreateResponse (in snake_case as they appear in
    // JSON)
    List<String> optionalFields =
        List.of(
            "background",
            "conversation",
            "created_at",
            "error",
            "incomplete_details",
            "instructions",
            "max_output_tokens",
            "max_tool_calls",
            "metadata",
            "object",
            "output",
            "parallel_tool_calls",
            "prompt",
            "prompt_cache_key",
            "prompt_cache_retention",
            "reasoning",
            "safety_identifier",
            "service_tier",
            "temperature",
            "text",
            "tool_choice",
            "tools",
            "top_logprobs",
            "top_p",
            "truncation",
            "include",
            "input",
            "store",
            "stream",
            "stream_options");

    // Generate a subset of fields to remove (0 to 5 fields)
    return Arbitraries.of(optionalFields)
        .list()
        .ofMinSize(0)
        .ofMaxSize(5)
        .map(
            list -> {
              // Ensure unique field names
              List<String> uniqueList = new ArrayList<>(list.stream().distinct().toList());
              return uniqueList;
            });
  }
}
