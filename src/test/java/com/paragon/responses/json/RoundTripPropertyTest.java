package com.paragon.responses.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.spec.*;
import java.util.Map;
import net.jqwik.api.*;

/**
 * Property-based tests for serialization round-trip preservation.
 *
 * <p>**Feature: responses-api-jackson-serialization, Property 3: Serialization round-trip
 * preservation**
 *
 * <p>Validates: Requirements 2.1, 2.2, 3.2, 4.2, 5.2
 */
public class RoundTripPropertyTest {

  private final ObjectMapper objectMapper = ResponsesApiObjectMapper.create();

  /**
   * Property 3: Serialization round-trip preservation
   *
   * <p>For any CreateResponse object, serializing to JSON and then deserializing back should
   * produce an object equivalent to the original.
   *
   * <p>**Validates: Requirements 2.1, 2.2, 3.2, 4.2, 5.2**
   */
  @Property(tries = 100)
  void createResponseRoundTripPreservesData(
      @ForAll("createResponses") CreateResponsePayload original) throws Exception {
    // Serialize to JSON
    String json = objectMapper.writeValueAsString(original);

    // Deserialize back to object
    CreateResponsePayload deserialized = objectMapper.readValue(json, CreateResponsePayload.class);

    // Verify equivalence
    assertEquals(original, deserialized, "Deserialized CreateResponse should equal original");
  }

  /**
   * Property 3: Serialization round-trip preservation (Response variant)
   *
   * <p>For any Response object, serializing to JSON and then deserializing back should produce an
   * object equivalent to the original.
   *
   * <p>**Validates: Requirements 2.1, 2.2, 3.2, 4.2, 5.2**
   */
  @Property(tries = 100)
  void responseRoundTripPreservesData(@ForAll("responses") Response original) throws Exception {
    // Serialize to JSON
    String json = objectMapper.writeValueAsString(original);

    // Deserialize back to object
    Response deserialized = objectMapper.readValue(json, Response.class);

    // Verify equivalence
    assertEquals(original, deserialized, "Deserialized Response should equal original");
  }

  /**
   * Property 3: Round-trip with complex nested structures
   *
   * <p>Tests round-trip preservation with CreateResponse containing nested discriminated unions,
   * enums, and coordinates.
   */
  @Property(tries = 100)
  void createResponseWithComplexDataRoundTrips(
      @ForAll("complexCreateResponses") CreateResponsePayload original) throws Exception {
    // Serialize to JSON
    String json = objectMapper.writeValueAsString(original);

    // Deserialize back to object
    CreateResponsePayload deserialized = objectMapper.readValue(json, CreateResponsePayload.class);

    // Verify equivalence
    assertEquals(
        original,
        deserialized,
        "Deserialized CreateResponse with complex data should equal original");
  }

  /**
   * Property 3: Round-trip with Response containing output items
   *
   * <p>Tests round-trip preservation with Response containing various output types including
   * discriminated unions.
   */
  @Property(tries = 100)
  void responseWithOutputRoundTrips(@ForAll("responsesWithOutput") Response original)
      throws Exception {
    // Serialize to JSON
    String json = objectMapper.writeValueAsString(original);

    // Deserialize back to object
    Response deserialized = objectMapper.readValue(json, Response.class);

    // Verify equivalence
    assertEquals(original, deserialized, "Deserialized Response with output should equal original");
  }

  // ===== Generators =====

  @Provide
  Arbitrary<CreateResponsePayload> createResponses() {
    return Arbitraries.of(
        // Minimal CreateResponse
        new CreateResponsePayload(
            null, null, null, null, null, null, null, null, "gpt-4o", null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null),
        // CreateResponse with various field types
        new CreateResponsePayload(
            true, // background
            "conv-123", // conversation
            null, // include
            null, // input
            "Test instructions", // instructions
            1000, // maxOutputTokens
            5, // maxToolCalls
            Map.of("key1", "value1", "key2", "value2"), // metadata
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
            ),
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
  Arbitrary<CreateResponsePayload> complexCreateResponses() {
    return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20).injectNull(0.3),
            Arbitraries.of(ServiceTierType.values()).injectNull(0.2),
            Arbitraries.of(Truncation.values()).injectNull(0.2),
            Arbitraries.integers().between(100, 5000).injectNull(0.3),
            Arbitraries.doubles().between(0.0, 2.0).injectNull(0.3),
            inputItems().list().ofMaxSize(3).injectNull(0.3))
        .as(
            (model, serviceTier, truncation, maxTokens, temp, input) ->
                new CreateResponsePayload(
                    null,
                    null,
                    null,
                    input,
                    "Complex test",
                    maxTokens,
                    null,
                    null,
                    model,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    serviceTier,
                    null,
                    null,
                    null,
                    temp,
                    null,
                    null,
                    null,
                    null,
                    null,
                    truncation,
                    null));
  }

  @Provide
  Arbitrary<Response> responses() {
    return Arbitraries.of(
        // Minimal Response - 28 parameters
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
        // Response with various fields - 28 parameters
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
            ),
        // Response with different status - 28 parameters
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
  Arbitrary<Response> responsesWithOutput() {
    return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20),
            Arbitraries.of(ResponseGenerationStatus.values()),
            outputItems().list().ofMinSize(1).ofMaxSize(3))
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
  Arbitrary<ResponseInputItem> inputItems() {
    return Arbitraries.oneOf(userMessages(), developerMessages());
  }

  @Provide
  Arbitrary<UserMessage> userMessages() {
    return messageContents().list().ofMinSize(1).ofMaxSize(2).map(UserMessage::new);
  }

  @Provide
  Arbitrary<DeveloperMessage> developerMessages() {
    return messageContents()
        .list()
        .ofMinSize(1)
        .ofMaxSize(2)
        .map(content -> new DeveloperMessage(content, null));
  }

  @Provide
  Arbitrary<MessageContent> messageContents() {
    return Arbitraries.oneOf(texts(), images());
  }

  @Provide
  Arbitrary<Text> texts() {
    return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50).map(Text::new);
  }

  @Provide
  Arbitrary<Image> images() {
    return Combinators.combine(
            Arbitraries.of(ImageDetail.values()),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20).injectNull(0.3),
            Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(50).injectNull(0.3))
        .as(Image::new);
  }

  @Provide
  Arbitrary<ResponseOutput> outputItems() {
    return Arbitraries.oneOf(outputMessages());
  }

  @Provide
  Arbitrary<OutputMessage<Void>> outputMessages() {
    return Combinators.combine(
            messageContents().list().ofMinSize(1).ofMaxSize(2),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20),
            Arbitraries.of(InputMessageStatus.values()))
        .as((content, id, status) -> new OutputMessage<>(content, id, status, null));
  }
}
