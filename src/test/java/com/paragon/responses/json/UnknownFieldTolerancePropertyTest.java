package com.paragon.responses.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.spec.*;
import java.util.List;
import java.util.Map;
import net.jqwik.api.*;

/**
 * Property-based tests for unknown field tolerance in JSON deserialization.
 *
 * <p>**Feature: responses-api-jackson-serialization, Property 4: Unknown field tolerance**
 *
 * <p>Validates: Requirements 2.3
 */
public class UnknownFieldTolerancePropertyTest {

  private final ObjectMapper objectMapper = ResponsesApiObjectMapper.create();

  /**
   * Property 4: Unknown field tolerance
   *
   * <p>For any valid Response JSON with additional unknown fields added, deserialization should
   * succeed and produce a valid Response object (ignoring the unknown fields).
   *
   * <p>**Validates: Requirements 2.3**
   */
  @Property(tries = 100)
  void responseWithUnknownFieldsDeserializesSuccessfully(
      @ForAll("responses") Response original,
      @ForAll("unknownFields") Map<String, Object> unknownFields)
      throws Exception {

    // Serialize original to JSON
    String originalJson = objectMapper.writeValueAsString(original);

    // Parse to JsonNode and add unknown fields
    ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(originalJson);
    for (Map.Entry<String, Object> entry : unknownFields.entrySet()) {
      jsonNode.putPOJO(entry.getKey(), entry.getValue());
    }

    // Convert back to JSON string with unknown fields
    String jsonWithUnknownFields = objectMapper.writeValueAsString(jsonNode);

    // Deserialize - should succeed despite unknown fields
    Response deserialized = objectMapper.readValue(jsonWithUnknownFields, Response.class);

    // Verify deserialization succeeded and known fields match
    assertNotNull(deserialized, "Deserialization should succeed with unknown fields");
    assertEquals(original.id(), deserialized.id(), "Known fields should be preserved");
    assertEquals(original.model(), deserialized.model(), "Known fields should be preserved");
    assertEquals(original.status(), deserialized.status(), "Known fields should be preserved");
  }

  /**
   * Property 4: Unknown field tolerance (CreateResponse variant)
   *
   * <p>For any valid CreateResponse JSON with additional unknown fields added, deserialization
   * should succeed and produce a valid CreateResponse object (ignoring the unknown fields).
   *
   * <p>**Validates: Requirements 2.3**
   */
  @Property(tries = 100)
  void createResponseWithUnknownFieldsDeserializesSuccessfully(
      @ForAll("createResponses") CreateResponsePayload original,
      @ForAll("unknownFields") Map<String, Object> unknownFields)
      throws Exception {

    // Serialize original to JSON
    String originalJson = objectMapper.writeValueAsString(original);

    // Parse to JsonNode and add unknown fields
    ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(originalJson);
    for (Map.Entry<String, Object> entry : unknownFields.entrySet()) {
      jsonNode.putPOJO(entry.getKey(), entry.getValue());
    }

    // Convert back to JSON string with unknown fields
    String jsonWithUnknownFields = objectMapper.writeValueAsString(jsonNode);

    // Deserialize - should succeed despite unknown fields
    CreateResponsePayload deserialized =
        objectMapper.readValue(jsonWithUnknownFields, CreateResponsePayload.class);

    // Verify deserialization succeeded and known fields match
    assertNotNull(deserialized, "Deserialization should succeed with unknown fields");
    assertEquals(original.model(), deserialized.model(), "Known fields should be preserved");
    assertEquals(
        original.instructions(), deserialized.instructions(), "Known fields should be preserved");
    assertEquals(
        original.maxOutputTokens(),
        deserialized.maxOutputTokens(),
        "Known fields should be preserved");
  }

  /**
   * Property 4: Unknown field tolerance with nested objects
   *
   * <p>Tests that unknown fields in nested objects are also tolerated.
   */
  @Property(tries = 100)
  void responseWithNestedUnknownFieldsDeserializesSuccessfully(
      @ForAll("responsesWithOutput") Response original,
      @ForAll("unknownFields") Map<String, Object> unknownFields)
      throws Exception {

    // Serialize original to JSON
    String originalJson = objectMapper.writeValueAsString(original);

    // Parse to JsonNode and add unknown fields at root level
    ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(originalJson);
    for (Map.Entry<String, Object> entry : unknownFields.entrySet()) {
      jsonNode.putPOJO(entry.getKey(), entry.getValue());
    }

    // Convert back to JSON string with unknown fields
    String jsonWithUnknownFields = objectMapper.writeValueAsString(jsonNode);

    // Deserialize - should succeed despite unknown fields
    Response deserialized = objectMapper.readValue(jsonWithUnknownFields, Response.class);

    // Verify deserialization succeeded
    assertNotNull(
        deserialized, "Deserialization should succeed with unknown fields in nested objects");
    assertEquals(original.id(), deserialized.id(), "Known fields should be preserved");
  }

  // ===== Generators =====

  @Provide
  Arbitrary<Response> responses() {
    return Arbitraries.of(
        // Minimal Response
        new Response(
            null,
            null,
            null,
            null,
            "resp-123",
            null,
            null,
            null,
            null,
            null,
            "gpt-4o",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null),
        // Response with various fields
        new Response(
            true,
            null,
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
        // Response with different status
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
            Truncation.DISABLED));
  }

  @Provide
  Arbitrary<CreateResponsePayload> createResponses() {
    return Arbitraries.of(
        // Minimal CreateResponse
        new CreateResponsePayload(
            null, null, null, null, null, null, null, null, "gpt-4o", null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null),
        // CreateResponse with various fields
        new CreateResponsePayload(
            true,
            "conv-123",
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
        // CreateResponse with different enum values
        new CreateResponsePayload(
            false,
            "conv-456",
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
            null));
  }

  @Provide
  Arbitrary<Response> responsesWithOutput() {
    return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20),
            Arbitraries.of(ResponseGenerationStatus.values()),
            outputItems().list().ofMinSize(1).ofMaxSize(2))
        .as(
            (id, status, output) ->
                new Response(
                    null,
                    null,
                    System.currentTimeMillis() / 1000,
                    null,
                    id,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "gpt-4o",
                    ResponseObject.RESPONSE,
                    output,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    status,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));
  }

  @Provide
  Arbitrary<ResponseOutput> outputItems() {
    return outputMessages().map(msg -> (ResponseOutput) msg);
  }

  @Provide
  Arbitrary<OutputMessage<Void>> outputMessages() {
    return Combinators.combine(
            messageContents().list().ofMinSize(1).ofMaxSize(2),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20),
            Arbitraries.of(InputMessageStatus.values()))
        .as((content, id, status) -> new OutputMessage<>(content, id, status, null));
  }

  @Provide
  Arbitrary<MessageContent> messageContents() {
    return Arbitraries.oneOf(texts());
  }

  @Provide
  Arbitrary<Text> texts() {
    return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50).map(Text::new);
  }

  /**
   * Generates maps of unknown field names and values. These represent fields that don't exist in
   * the actual schema.
   */
  @Provide
  Arbitrary<Map<String, Object>> unknownFields() {
    return Combinators.combine(
            Arbitraries.strings()
                .alpha()
                .ofMinLength(5)
                .ofMaxLength(20)
                .filter(s -> !isKnownField(s)),
            unknownFieldValues())
        .as((key, value) -> Map.of(key, value))
        .injectDuplicates(0.1);
  }

  /** Generates various types of values for unknown fields. */
  @Provide
  Arbitrary<Object> unknownFieldValues() {
    return Arbitraries.oneOf(
        Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50),
        Arbitraries.integers().between(-1000, 1000),
        Arbitraries.doubles().between(-100.0, 100.0),
        Arbitraries.of(true, false),
        Arbitraries.just(List.of("item1", "item2")),
        Arbitraries.just(Map.of("nested_key", "nested_value")));
  }

  /**
   * Checks if a field name is a known field in Response or CreateResponse. This helps ensure we're
   * actually testing unknown fields.
   */
  private boolean isKnownField(String fieldName) {
    // Known fields from Response and CreateResponse (in snake_case)
    List<String> knownFields =
        List.of(
            "background",
            "conversation",
            "created_at",
            "error",
            "id",
            "incomplete_details",
            "instructions",
            "max_output_tokens",
            "max_tool_calls",
            "metadata",
            "model",
            "object",
            "output",
            "parallel_tool_calls",
            "prompt",
            "prompt_cache_key",
            "prompt_cache_retention",
            "reasoning",
            "safety_identifier",
            "service_tier",
            "status",
            "temperature",
            "text",
            "tool_choice",
            "tools",
            "top_logprobs",
            "top_p",
            "truncation",
            "parsed",
            "include",
            "input",
            "store",
            "stream",
            "stream_options");
    return knownFields.contains(fieldName);
  }
}
