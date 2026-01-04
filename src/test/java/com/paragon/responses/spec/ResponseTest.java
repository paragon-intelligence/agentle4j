package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for Response class.
 *
 * <p>Tests cover: - Accessors for all fields - outputText aggregation - parse method for structured
 * output - Edge cases - equals/hashCode - toString - utility methods
 */
@DisplayName("Response Tests")
class ResponseTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ACCESSORS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Accessors")
  class Accessors {

    @Test
    @DisplayName("id returns value")
    void idReturnsValue() {
      Response response = createMinimalResponse("resp_123");

      assertEquals("resp_123", response.id());
    }

    @Test
    @DisplayName("model returns value")
    void modelReturnsValue() {
      Response response = createMinimalResponse("resp_123");

      assertEquals("gpt-4o", response.model());
    }

    @Test
    @DisplayName("status returns value")
    void statusReturnsValue() {
      Response response = createMinimalResponse("resp_123");

      assertEquals(ResponseGenerationStatus.COMPLETED, response.status());
    }

    @Test
    @DisplayName("object returns response")
    void objectReturnsResponse() {
      Response response = createMinimalResponse("resp_123");

      assertEquals(ResponseObject.RESPONSE, response.object());
    }

    @Test
    @DisplayName("output returns list")
    void outputReturnsList() {
      Response response = createMinimalResponse("resp_123");

      assertNotNull(response.output());
      assertFalse(response.output().isEmpty());
    }

    @Test
    @DisplayName("background returns value")
    void backgroundReturnsValue() {
      Response response = createResponseWithAllFields();

      assertEquals(true, response.background());
    }

    @Test
    @DisplayName("createdAt returns value")
    void createdAtReturnsValue() {
      Response response = createMinimalResponse("resp_123");

      assertNotNull(response.createdAt());
    }

    @Test
    @DisplayName("maxOutputTokens returns value")
    void maxOutputTokensReturnsValue() {
      Response response = createResponseWithAllFields();

      assertEquals(1000, response.maxOutputTokens());
    }

    @Test
    @DisplayName("maxToolCalls returns value")
    void maxToolCallsReturnsValue() {
      Response response = createResponseWithAllFields();

      assertEquals(10, response.maxToolCalls());
    }

    @Test
    @DisplayName("parallelToolCalls returns value")
    void parallelToolCallsReturnsValue() {
      Response response = createResponseWithAllFields();

      assertEquals(true, response.parallelToolCalls());
    }

    @Test
    @DisplayName("metadata returns value")
    void metadataReturnsValue() {
      Response response = createResponseWithAllFields();

      assertNotNull(response.metadata());
      assertEquals("value1", response.metadata().get("key1"));
    }

    @Test
    @DisplayName("temperature returns value")
    void temperatureReturnsValue() {
      Response response = createResponseWithAllFields();

      assertNotNull(response.temperature());
    }

    @Test
    @DisplayName("topP returns value")
    void topPReturnsValue() {
      Response response = createResponseWithAllFields();

      assertNotNull(response.topP());
    }

    @Test
    @DisplayName("topLogprobs returns value")
    void topLogprobsReturnsValue() {
      Response response = createResponseWithAllFields();

      assertEquals(5, response.topLogprobs());
    }

    @Test
    @DisplayName("promptCacheKey returns value")
    void promptCacheKeyReturnsValue() {
      Response response = createResponseWithAllFields();

      assertEquals("cache-key", response.promptCacheKey());
    }

    @Test
    @DisplayName("promptCacheRetention returns value")
    void promptCacheRetentionReturnsValue() {
      Response response = createResponseWithAllFields();

      assertEquals("24h", response.promptCacheRetention());
    }

    @Test
    @DisplayName("safetyIdentifier returns value")
    void safetyIdentifierReturnsValue() {
      Response response = createResponseWithAllFields();

      assertEquals("safety-123", response.safetyIdentifier());
    }

    @Test
    @DisplayName("serviceTier returns value")
    void serviceTierReturnsValue() {
      Response response = createResponseWithAllFields();

      assertEquals("default", response.serviceTier());
    }

    @Test
    @DisplayName("instructions returns null when not set")
    void instructionsReturnsNullWhenNotSet() {
      Response response = createMinimalResponse("resp_123");

      assertNull(response.instructions());
    }

    @Test
    @DisplayName("error returns null when not set")
    void errorReturnsNullWhenNotSet() {
      Response response = createMinimalResponse("resp_123");

      assertNull(response.error());
    }

    @Test
    @DisplayName("conversation returns null when not set")
    void conversationReturnsNullWhenNotSet() {
      Response response = createMinimalResponse("resp_123");

      assertNull(response.conversation());
    }

    @Test
    @DisplayName("incompleteDetails returns null when not set")
    void incompleteDetailsReturnsNullWhenNotSet() {
      Response response = createMinimalResponse("resp_123");

      assertNull(response.incompleteDetails());
    }

    @Test
    @DisplayName("prompt returns null when not set")
    void promptReturnsNullWhenNotSet() {
      Response response = createMinimalResponse("resp_123");

      assertNull(response.prompt());
    }

    @Test
    @DisplayName("reasoning returns null when not set")
    void reasoningReturnsNullWhenNotSet() {
      Response response = createMinimalResponse("resp_123");

      assertNull(response.reasoning());
    }

    @Test
    @DisplayName("text returns null when not set")
    void textReturnsNullWhenNotSet() {
      Response response = createMinimalResponse("resp_123");

      assertNull(response.text());
    }

    @Test
    @DisplayName("toolChoice returns null when not set")
    void toolChoiceReturnsNullWhenNotSet() {
      Response response = createMinimalResponse("resp_123");

      assertNull(response.toolChoice());
    }

    @Test
    @DisplayName("tools returns null when not set")
    void toolsReturnsNullWhenNotSet() {
      Response response = createMinimalResponse("resp_123");

      assertNull(response.tools());
    }

    @Test
    @DisplayName("truncation returns null when not set")
    void truncationReturnsNullWhenNotSet() {
      Response response = createMinimalResponse("resp_123");

      assertNull(response.truncation());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // OUTPUT TEXT AGGREGATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Output Text Aggregation")
  class OutputTextAggregation {

    @Test
    @DisplayName("outputText aggregates text from output")
    void outputTextAggregates() {
      Response response = createResponseWithOutput("Hello, World!");

      assertEquals("Hello, World!", response.outputText());
    }

    @Test
    @DisplayName("outputText returns empty for no text output")
    void outputTextEmptyWhenNoText() {
      Response response = createMinimalResponseWithEmptyOutput();

      assertEquals("", response.outputText());
    }

    @Test
    @DisplayName("outputText returns null when output is null")
    void outputTextNullWhenNoOutput() {
      Response response = createResponseWithNullOutput();

      assertNull(response.outputText());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PARSE METHOD
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Parse Method")
  class ParseMethod {

    @Test
    @DisplayName("parse returns parsed response")
    void parseReturnsParsedResponse() throws Exception {
      Response response = createResponseWithOutput("{\"name\": \"John\", \"age\": 30}");

      ParsedResponse<Person> parsed = response.parse(Person.class, objectMapper);

      assertNotNull(parsed);
      assertNotNull(parsed.outputParsed());
      assertEquals("John", parsed.outputParsed().name());
      assertEquals(30, parsed.outputParsed().age());
    }

    @Test
    @DisplayName("parse throws on invalid JSON")
    void parseThrowsOnInvalidJson() {
      Response response = createResponseWithOutput("not valid json");

      assertThrows(Exception.class, () -> response.parse(Person.class, objectMapper));
    }

    @Test
    @DisplayName("parse throws when no message in output")
    void parseThrowsWhenNoMessage() {
      Response response = createMinimalResponseWithEmptyOutput();

      assertThrows(RuntimeException.class, () -> response.parse(Person.class, objectMapper));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // STATUS HELPERS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Status Helpers")
  class StatusHelpers {

    @Test
    @DisplayName("failed returns false for completed")
    void failedReturnsFalseForCompleted() {
      Response response = createMinimalResponse("resp_123");

      assertFalse(response.failed());
    }

    @Test
    @DisplayName("failed returns true for failed status")
    void failedReturnsTrueForFailed() {
      Response response = createFailedResponse();

      assertTrue(response.failed());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TOOL CALLS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Tool Calls")
  class ToolCallsTests {

    @Test
    @DisplayName("functionToolCalls returns empty for no tool calls")
    void functionToolCallsReturnsEmptyForNoToolCalls() {
      Response response = createMinimalResponse("resp_123");

      assertTrue(response.functionToolCalls().isEmpty());
    }

    @Test
    @DisplayName("functionToolCalls returns empty when output is null")
    void functionToolCallsEmptyWhenOutputNull() {
      Response response = createResponseWithNullOutput();

      assertTrue(response.functionToolCalls().isEmpty());
    }

    @Test
    @DisplayName("toolCalls returns empty for no tool calls")
    void toolCallsReturnsEmptyForNoToolCalls() {
      Response response = createMinimalResponse("resp_123");

      assertTrue(response.toolCalls().isEmpty());
    }

    @Test
    @DisplayName("toolCalls returns empty when output is null")
    void toolCallsEmptyWhenOutputNull() {
      Response response = createResponseWithNullOutput();

      assertTrue(response.toolCalls().isEmpty());
    }

    @Test
    @DisplayName("images returns empty for no images")
    void imagesReturnsEmptyForNoImages() {
      Response response = createMinimalResponse("resp_123");

      assertTrue(response.images().isEmpty());
    }

    @Test
    @DisplayName("images returns empty when output is null")
    void imagesEmptyWhenOutputNull() {
      Response response = createResponseWithNullOutput();

      assertTrue(response.images().isEmpty());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // IS INCOMPLETE
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Is Incomplete")
  class IsIncompleteTests {

    @Test
    @DisplayName("isIncomplete returns false when incompleteDetails is null")
    void isIncompleteReturnsFalseWhenNull() {
      Response response = createMinimalResponse("resp_123");

      assertFalse(response.isIncomplete());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // EQUALS AND HASHCODE
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Equals and HashCode")
  class EqualsHashCode {

    @Test
    @DisplayName("equals returns true for same instance")
    void equalsReturnsTrueForSameInstance() {
      Response response = createMinimalResponse("resp_123");

      assertEquals(response, response);
    }

    @Test
    @DisplayName("equals returns true for equal objects")
    void equalsReturnsTrueForEqualObjects() {
      Response response1 = createMinimalResponseFixed("resp_123", 1000L);
      Response response2 = createMinimalResponseFixed("resp_123", 1000L);

      assertEquals(response1, response2);
    }

    @Test
    @DisplayName("equals returns false for different ids")
    void equalsReturnsFalseForDifferentIds() {
      Response response1 = createMinimalResponseFixed("resp_123", 1000L);
      Response response2 = createMinimalResponseFixed("resp_456", 1000L);

      assertNotEquals(response1, response2);
    }

    @Test
    @DisplayName("equals returns false for non-Response object")
    void equalsReturnsFalseForNonResponse() {
      Response response = createMinimalResponse("resp_123");

      assertNotEquals(response, "not a response");
    }

    @Test
    @DisplayName("hashCode is consistent for equal objects")
    void hashCodeConsistentForEqualObjects() {
      Response response1 = createMinimalResponseFixed("resp_123", 1000L);
      Response response2 = createMinimalResponseFixed("resp_123", 1000L);

      assertEquals(response1.hashCode(), response2.hashCode());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TO STRING
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ToString")
  class ToStringTests {

    @Test
    @DisplayName("toString contains id")
    void toStringContainsId() {
      Response response = createMinimalResponse("resp_123");

      assertTrue(response.toString().contains("resp_123"));
    }

    @Test
    @DisplayName("toString contains model")
    void toStringContainsModel() {
      Response response = createMinimalResponse("resp_123");

      assertTrue(response.toString().contains("gpt-4o"));
    }

    @Test
    @DisplayName("toString contains status")
    void toStringContainsStatus() {
      Response response = createMinimalResponse("resp_123");

      assertTrue(response.toString().contains("COMPLETED"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPERS
  // ═══════════════════════════════════════════════════════════════════════════

  private Response createMinimalResponse(String id) {
    // Response constructor: 28 parameters
    return new Response(
        null, // background
        null, // conversation
        System.currentTimeMillis() / 1000, // createdAt
        null, // error
        id, // id
        null, // incompleteDetails
        null, // instructions (ResponseInputItem)
        null, // maxOutputTokens
        null, // maxToolCalls
        null, // metadata
        "gpt-4o", // model
        ResponseObject.RESPONSE, // object
        List.of(createOutputMessage("Test output")), // output
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
        );
  }

  private Response createMinimalResponseFixed(String id, long createdAt) {
    return new Response(
        null,
        null,
        createdAt,
        null,
        id,
        null,
        null,
        null,
        null,
        null,
        "gpt-4o",
        ResponseObject.RESPONSE,
        List.of(createOutputMessage("Test output")),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        ResponseGenerationStatus.COMPLETED,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private Response createMinimalResponseWithEmptyOutput() {
    return new Response(
        null,
        null,
        System.currentTimeMillis() / 1000,
        null,
        "resp_123",
        null,
        null,
        null,
        null,
        null,
        "gpt-4o",
        ResponseObject.RESPONSE,
        List.of(),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        ResponseGenerationStatus.COMPLETED,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private Response createResponseWithNullOutput() {
    return new Response(
        null,
        null,
        System.currentTimeMillis() / 1000,
        null,
        "resp_123",
        null,
        null,
        null,
        null,
        null,
        "gpt-4o",
        ResponseObject.RESPONSE,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        ResponseGenerationStatus.COMPLETED,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private Response createResponseWithOutput(String text) {
    return new Response(
        null,
        null,
        System.currentTimeMillis() / 1000,
        null,
        "resp_123",
        null,
        null,
        null,
        null,
        null,
        "gpt-4o",
        ResponseObject.RESPONSE,
        List.of(createOutputMessage(text)),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        ResponseGenerationStatus.COMPLETED,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private Response createResponseWithAllFields() {
    return new Response(
        true, // background
        null, // conversation
        System.currentTimeMillis() / 1000, // createdAt
        null, // error
        "resp_all", // id
        null, // incompleteDetails
        null, // instructions
        1000, // maxOutputTokens
        10, // maxToolCalls
        Map.of("key1", "value1"), // metadata
        "gpt-4o", // model
        ResponseObject.RESPONSE, // object
        List.of(createOutputMessage("Test")), // output
        true, // parallelToolCalls
        null, // prompt
        "cache-key", // promptCacheKey
        "24h", // promptCacheRetention
        null, // reasoning
        "safety-123", // safetyIdentifier
        "default", // serviceTier
        ResponseGenerationStatus.COMPLETED, // status
        0.7, // temperature
        null, // text
        null, // toolChoice
        null, // tools
        5, // topLogprobs
        0.9, // topP
        null // truncation
        );
  }

  private Response createFailedResponse() {
    return new Response(
        null,
        null,
        System.currentTimeMillis() / 1000,
        null,
        "resp_fail",
        null,
        null,
        null,
        null,
        null,
        "gpt-4o",
        ResponseObject.RESPONSE,
        List.of(),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        ResponseGenerationStatus.FAILED,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private ResponseOutput createOutputMessage(String text) {
    // OutputMessage constructor: content, id, status, parsed
    return new OutputMessage<Void>(
        List.of(Text.valueOf(text)), "msg_123", InputMessageStatus.COMPLETED, null);
  }

  public record Person(String name, int age) {}
}
