package com.paragon.responses.streaming;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.Responder;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.spec.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

/**
 * Extended tests for ResponseStream.java covering: - toFuture() and toParsedFuture() execution
 * paths - collectText() method - onPartialParsed() with partial JSON parsing - onPartialJson()
 * map-based parsing - withToolStore() and onToolResult() - Error handling paths
 */
@DisplayName("ResponseStream Extended Tests")
class ResponseStreamExtendedTest {

  private static final ObjectMapper objectMapper = ResponsesApiObjectMapper.create();
  private static final String TEST_API_KEY = "test-api-key-12345";

  private MockWebServer mockWebServer;
  private Responder responder;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    responder =
        Responder.builder()
            .apiKey(TEST_API_KEY)
            .baseUrl(mockWebServer.url("/v1/responses"))
            .build();
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // toFuture() TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("toFuture()")
  class ToFutureTests {

    @Test
    @DisplayName("toFuture returns completed response")
    void toFutureReturnsCompletedResponse() throws Exception {
      String sseResponse =
          """
          data: {"type":"response.output_text.delta","item_id":"msg-1","output_index":0,"content_index":0,"delta":"Hello","sequence_number":1}

          data: {"type":"response.completed","response":{"id":"resp-1","object":"response","created_at":1234567890,"status":"completed","status_details":null,"output":[{"type":"message","id":"msg-1","status":"completed","role":"assistant","content":[{"type":"output_text","text":"Hello","annotations":[]}]}],"error":null,"metadata":null,"usage":null,"incomplete_details":null,"model":"gpt-4o","output_text":"Hello"},"sequence_number":2}

          data: [DONE]
          """;

      mockWebServer.enqueue(
          new MockResponse()
              .setBody(sseResponse)
              .addHeader("Content-Type", "text/event-stream")
              .setResponseCode(200));

      var payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .addUserMessage("Say Hello")
              .streaming()
              .build();

      Response response = responder.respond(payload).get();

      assertNotNull(response);
      assertEquals("resp-1", response.id());
    }

    @Test
    @DisplayName("toFuture completes exceptionally on error")
    void toFutureCompletesExceptionallyOnError() throws Exception {
      String sseResponse =
          """
          data: {"type":"error","code":"rate_limit_exceeded","message":"Too many requests","param":null,"sequence_number":1}
          """;

      mockWebServer.enqueue(
          new MockResponse()
              .setBody(sseResponse)
              .addHeader("Content-Type", "text/event-stream")
              .setResponseCode(200));

      var payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .addUserMessage("Test")
              .streaming()
              .build();

      ResponseStream<Void> stream = responder.respond(payload);

      Exception ex = assertThrows(RuntimeException.class, stream::get);
      assertTrue(
          ex.getMessage().contains("rate_limit")
              || ex.getCause().getMessage().contains("rate_limit"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // collectText() TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("collectText()")
  class CollectTextTests {

    @Test
    @DisplayName("collectText concatenates all deltas")
    void collectTextConcatenatesDeltas() throws Exception {
      String sseResponse =
          """
          data: {"type":"response.output_text.delta","item_id":"msg-1","output_index":0,"content_index":0,"delta":"Hello","sequence_number":1}

          data: {"type":"response.output_text.delta","item_id":"msg-1","output_index":0,"content_index":0,"delta":" ","sequence_number":2}

          data: {"type":"response.output_text.delta","item_id":"msg-1","output_index":0,"content_index":0,"delta":"World","sequence_number":3}

          data: {"type":"response.completed","response":{"id":"resp-1","object":"response","created_at":1234567890,"status":"completed","status_details":null,"output":[{"type":"message","id":"msg-1","status":"completed","role":"assistant","content":[{"type":"output_text","text":"Hello World","annotations":[]}]}],"error":null,"metadata":null,"usage":null,"incomplete_details":null,"model":"gpt-4o","output_text":"Hello World"},"sequence_number":4}

          data: [DONE]
          """;

      mockWebServer.enqueue(
          new MockResponse()
              .setBody(sseResponse)
              .addHeader("Content-Type", "text/event-stream")
              .setResponseCode(200));

      var payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .addUserMessage("Test")
              .streaming()
              .build();

      String text = responder.respond(payload).getText();

      assertEquals("Hello World", text);
    }

    @Test
    @DisplayName("collectText handles empty response")
    void collectTextHandlesEmptyResponse() throws Exception {
      String sseResponse =
          """
          data: {"type":"response.completed","response":{"id":"resp-1","object":"response","created_at":1234567890,"status":"completed","status_details":null,"output":[],"error":null,"metadata":null,"usage":null,"incomplete_details":null,"model":"gpt-4o","output_text":""},"sequence_number":1}

          data: [DONE]
          """;

      mockWebServer.enqueue(
          new MockResponse()
              .setBody(sseResponse)
              .addHeader("Content-Type", "text/event-stream")
              .setResponseCode(200));

      var payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .addUserMessage("Test")
              .streaming()
              .build();

      String text = responder.respond(payload).getText();

      assertEquals("", text);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // toParsedFuture() TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("toParsedFuture()")
  class ToParsedFutureTests {

    @Test
    @DisplayName("toParsedFuture throws for non-structured stream")
    void toParsedFutureThrowsForNonStructuredStream() {
      var payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .addUserMessage("Test")
              .streaming()
              .build();

      ResponseStream<Void> stream = responder.respond(payload);

      assertThrows(IllegalStateException.class, stream::getParsed);
    }

    @Test
    @DisplayName("toParsedFuture parses structured response")
    void toParsedFutureParsesStructuredResponse() throws Exception {
      String sseResponse =
          """
          data: {"type":"response.output_text.delta","item_id":"msg-1","output_index":0,"content_index":0,"delta":"{\\"name\\":\\"Alice\\",\\"age\\":30}","sequence_number":1}

          data: {"type":"response.completed","response":{"id":"resp-1","object":"response","created_at":1234567890,"status":"completed","status_details":null,"output":[{"type":"message","id":"msg-1","status":"completed","role":"assistant","content":[{"type":"output_text","text":"{\\"name\\":\\"Alice\\",\\"age\\":30}","annotations":[]}]}],"error":null,"metadata":null,"usage":null,"incomplete_details":null,"model":"gpt-4o","output_text":"{\\"name\\":\\"Alice\\",\\"age\\":30}"},"sequence_number":2}

          data: [DONE]
          """;

      mockWebServer.enqueue(
          new MockResponse()
              .setBody(sseResponse)
              .addHeader("Content-Type", "text/event-stream")
              .setResponseCode(200));

      var payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .addUserMessage("Generate person")
              .withStructuredOutput(TestPerson.class)
              .streaming()
              .build();

      ParsedResponse<TestPerson> parsed =
          responder.respond(payload).getParsed();

      assertNotNull(parsed);
      assertEquals("Alice", parsed.outputParsed().name());
      assertEquals(30, parsed.outputParsed().age());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // onPartialParsed() TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("onPartialParsed()")
  class OnPartialParsedTests {

    @Test
    @DisplayName("onPartialParsed throws for non-structured stream")
    void onPartialParsedThrowsForNonStructuredStream() {
      var payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .addUserMessage("Test")
              .streaming()
              .build();

      ResponseStream<Void> stream = responder.respond(payload);

      assertThrows(
          IllegalStateException.class,
          () -> stream.onPartialParsed(TestPartialPerson.class, p -> {}));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // onPartialJson() TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("onPartialJson()")
  class OnPartialJsonTests {

    @Test
    @DisplayName("onPartialJson throws for non-structured stream")
    void onPartialJsonThrowsForNonStructuredStream() {
      var payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .addUserMessage("Test")
              .streaming()
              .build();

      ResponseStream<Void> stream = responder.respond(payload);

      assertThrows(IllegalStateException.class, () -> stream.onPartialJson(map -> {}));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // onParsedComplete() TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("onParsedComplete()")
  class OnParsedCompleteTests {

    @Test
    @DisplayName("onParsedComplete throws for non-structured stream")
    void onParsedCompleteThrowsForNonStructuredStream() {
      var payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .addUserMessage("Test")
              .streaming()
              .build();

      ResponseStream<Void> stream = responder.respond(payload);

      assertThrows(IllegalStateException.class, () -> stream.onParsedComplete(parsed -> {}));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ResponseFailedEvent TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("ResponseFailedEvent handling")
  class ResponseFailedEventTests {

    @Test
    @DisplayName("handles response.failed event")
    void handlesResponseFailedEvent() throws Exception {
      String sseResponse =
          """
          data: {"type":"response.failed","response":{"id":"resp-1","object":"response","created_at":1234567890,"status":"failed","status_details":null,"output":[],"error":{"type":"server_error","message":"Internal error"},"metadata":null,"usage":null,"incomplete_details":null,"model":"gpt-4o","output_text":null},"sequence_number":1}
          """;

      mockWebServer.enqueue(
          new MockResponse()
              .setBody(sseResponse)
              .addHeader("Content-Type", "text/event-stream")
              .setResponseCode(200));

      var payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .addUserMessage("Test")
              .streaming()
              .build();

      java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();

      ResponseStream<Void> stream = responder.respond(payload).onError(error::set);
      
      // Start will throw or capture error via callback
      try {
        stream.get();
      } catch (RuntimeException e) {
        // Expected
      }

      Throwable ex = error.get();
      assertNotNull(ex);
      assertTrue(ex.getMessage().contains("failed") || ex.getMessage().contains("error"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CANCEL TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Cancel functionality")
  class CancelTests {

    @Test
    @DisplayName("cancel before start works")
    void cancelBeforeStartWorks() {
      var payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .addUserMessage("Test")
              .streaming()
              .build();

      ResponseStream<Void> stream = responder.respond(payload);
      stream.cancel();

      assertTrue(stream.isCancelled());
    }

    @Test
    @DisplayName("cancel is idempotent")
    void cancelIsIdempotent() {
      var payload =
          CreateResponsePayload.builder()
              .model("gpt-4o")
              .addUserMessage("Test")
              .streaming()
              .build();

      ResponseStream<Void> stream = responder.respond(payload);
      stream.cancel();
      stream.cancel();
      stream.cancel();

      assertTrue(stream.isCancelled());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TEST TYPES
  // ═══════════════════════════════════════════════════════════════════════════

  public record TestPerson(String name, int age) {}

  public record TestPartialPerson(
      @JsonProperty("name") String name, @JsonProperty("age") Integer age) {
    @JsonCreator
    public TestPartialPerson {}
  }
}
