package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for Response class.
 *
 * <p>Tests cover: - Accessors for all fields - outputText aggregation - parse method for structured
 * output - Edge cases
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

  private ResponseOutput createOutputMessage(String text) {
    // OutputMessage constructor: content, id, status, parsed
    return new OutputMessage<Void>(
        List.of(Text.valueOf(text)), "msg_123", InputMessageStatus.COMPLETED, null);
  }

  public record Person(String name, int age) {}
}
