package com.paragon.responses.streaming;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.Responder;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.spec.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for streaming functionality. */
class ResponseStreamTest {

  private static final ObjectMapper objectMapper = ResponsesApiObjectMapper.create();
  private static final String TEST_API_KEY = "test-api-key-12345";

  private MockWebServer mockWebServer;
  private OkHttpClient okHttpClient;
  private Responder responder;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    okHttpClient = new OkHttpClient();
    responder =
        Responder.builder()
            .httpClient(okHttpClient)
            .apiKey(TEST_API_KEY)
            .baseUrl(mockWebServer.url("/v1/responses"))
            .build();
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  // ===== Text Delta Streaming Tests =====

  @Test
  void respondStreaming_collectsTextDeltas() throws Exception {
    String sseResponse =
        """
        data: {"type":"response.output_text.delta","item_id":"msg-1","output_index":0,"content_index":0,"delta":"Hello","sequence_number":1}

        data: {"type":"response.output_text.delta","item_id":"msg-1","output_index":0,"content_index":0,"delta":" ","sequence_number":2}

        data: {"type":"response.output_text.delta","item_id":"msg-1","output_index":0,"content_index":0,"delta":"World","sequence_number":3}

        data: [DONE]
        """;

    mockWebServer.enqueue(
        new MockResponse()
            .setBody(sseResponse)
            .addHeader("Content-Type", "text/event-stream")
            .setResponseCode(200));

    // Use .streaming().build() for proper type inference
    var payload =
        CreateResponsePayload.builder()
            .model("gpt-4o")
            .addUserMessage("Say Hello World")
            .streaming()
            .build();

    List<String> deltas = new ArrayList<>();

    // No cast needed - payload type is inferred
    responder.respond(payload).onTextDelta(deltas::add).start();

    Thread.sleep(500);

    assertEquals(3, deltas.size());
    assertEquals("Hello", deltas.get(0));
    assertEquals(" ", deltas.get(1));
    assertEquals("World", deltas.get(2));
  }

  @Test
  void respondStreaming_onEventReceivesEvents() throws Exception {
    String sseResponse =
        """
        data: {"type":"response.output_text.delta","item_id":"msg-1","output_index":0,"content_index":0,"delta":"Hi","sequence_number":1}

        data: {"type":"response.output_text.done","item_id":"msg-1","output_index":0,"content_index":0,"text":"Hi","sequence_number":2}

        data: [DONE]
        """;

    mockWebServer.enqueue(
        new MockResponse()
            .setBody(sseResponse)
            .addHeader("Content-Type", "text/event-stream")
            .setResponseCode(200));

    var payload =
        CreateResponsePayload.builder().model("gpt-4o").addUserMessage("Test").streaming().build();

    List<StreamingEvent> events = new ArrayList<>();

    responder.respond(payload).onEvent(events::add).start();

    Thread.sleep(500);

    assertEquals(2, events.size());
    assertInstanceOf(OutputTextDeltaEvent.class, events.get(0));
    assertInstanceOf(OutputTextDoneEvent.class, events.get(1));

    OutputTextDeltaEvent delta = (OutputTextDeltaEvent) events.get(0);
    assertEquals("Hi", delta.delta());

    OutputTextDoneEvent doneEvent = (OutputTextDoneEvent) events.get(1);
    assertEquals("Hi", doneEvent.text());
  }

  @Test
  void respondStreaming_cancellationStopsProcessing() throws Exception {
    String sseResponse =
        """
        data: {"type":"response.output_text.delta","item_id":"msg-1","output_index":0,"content_index":0,"delta":"One","sequence_number":1}

        data: {"type":"response.output_text.delta","item_id":"msg-1","output_index":0,"content_index":0,"delta":"Two","sequence_number":2}

        data: [DONE]
        """;

    mockWebServer.enqueue(
        new MockResponse()
            .setBody(sseResponse)
            .addHeader("Content-Type", "text/event-stream")
            .throttleBody(50, 200, TimeUnit.MILLISECONDS)
            .setResponseCode(200));

    var payload =
        CreateResponsePayload.builder().model("gpt-4o").addUserMessage("Test").streaming().build();

    ResponseStream<Void> stream = responder.respond(payload);
    stream.start();

    Thread.sleep(100);
    stream.cancel();

    assertTrue(stream.isCancelled());
  }

  @Test
  void respondStreaming_cannotStartTwice() throws Exception {
    String sseResponse =
        """
        data: {"type":"response.output_text.delta","item_id":"msg-1","output_index":0,"content_index":0,"delta":"Hi","sequence_number":1}

        data: [DONE]
        """;

    mockWebServer.enqueue(
        new MockResponse()
            .setBody(sseResponse)
            .addHeader("Content-Type", "text/event-stream")
            .setResponseCode(200));

    var payload =
        CreateResponsePayload.builder().model("gpt-4o").addUserMessage("Test").streaming().build();

    ResponseStream<Void> stream = responder.respond(payload);
    stream.start();

    assertThrows(IllegalStateException.class, stream::start);
  }

  @Test
  void respondStreaming_handlesErrorEvent() throws Exception {
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
        CreateResponsePayload.builder().model("gpt-4o").addUserMessage("Test").streaming().build();

    CompletableFuture<Throwable> error = new CompletableFuture<>();

    responder.respond(payload).onError(error::complete).start();

    Throwable ex = error.get(5, TimeUnit.SECONDS);
    assertNotNull(ex);
    assertTrue(ex.getMessage().contains("rate_limit_exceeded"));
  }

  @Test
  void respondStreaming_handlesHttpError() throws Exception {
    mockWebServer.enqueue(
        new MockResponse().setBody("{\"error\": \"Unauthorized\"}").setResponseCode(401));

    var payload =
        CreateResponsePayload.builder().model("gpt-4o").addUserMessage("Test").streaming().build();

    CompletableFuture<Throwable> error = new CompletableFuture<>();

    responder.respond(payload).onError(error::complete).start();

    Throwable ex = error.get(5, TimeUnit.SECONDS);
    assertNotNull(ex);
    assertTrue(ex.getMessage().contains("401"));
  }

  // ===== Builder Pattern Tests =====

  @Test
  void builderWithStreaming_returnsStreamingPayload() {
    var payload =
        CreateResponsePayload.builder().model("gpt-4o").addUserMessage("Test").streaming().build();

    assertInstanceOf(CreateResponsePayload.Streaming.class, payload);
  }

  @Test
  void builderWithStreamFalse_returnsRegularPayload() {
    CreateResponsePayload payload =
        CreateResponsePayload.builder().model("gpt-4o").addUserMessage("Test").stream(false)
            .build();

    assertEquals(CreateResponsePayload.class, payload.getClass());
  }

  // ===== Partial JSON Callback Tests =====

  @Test
  void onPartialJson_callbackIsRegistered() throws Exception {
    // Test that onPartialJson correctly registers and does NOT throw for structured streaming
    var payload =
        CreateResponsePayload.builder()
            .model("gpt-4o")
            .addUserMessage("Test")
            .withStructuredOutput(TestPerson.class)
            .streaming()
            .build();

    // Should not throw - structured streaming allows onPartialJson
    ResponseStream<?> stream = responder.respond(payload).onPartialJson(map -> {});
    assertNotNull(stream);
  }

  @Test
  void onPartialJson_throwsForNonStructuredStream() {
    var payload =
        CreateResponsePayload.builder().model("gpt-4o").addUserMessage("Test").streaming().build();

    // Non-structured payload - should throw
    assertThrows(
        IllegalStateException.class, () -> responder.respond(payload).onPartialJson(map -> {}));
  }

  // ===== Tool Call Streaming Tests =====

  @Test
  void onToolCall_receivesToolCallEvents() throws Exception {
    String sseResponse =
        """
        data: {"type":"response.function_call_arguments.done","item_id":"call-123","output_index":0,"name":"get_weather","arguments":"{\\"location\\":\\"Tokyo\\"}","sequence_number":1}

        data: [DONE]
        """;

    mockWebServer.enqueue(
        new MockResponse()
            .setBody(sseResponse)
            .addHeader("Content-Type", "text/event-stream")
            .setResponseCode(200));

    var payload =
        CreateResponsePayload.builder().model("gpt-4o").addUserMessage("Test").streaming().build();

    List<String> toolNames = new ArrayList<>();
    List<String> toolArgs = new ArrayList<>();

    responder
        .respond(payload)
        .onToolCall(
            (name, args) -> {
              toolNames.add(name);
              toolArgs.add(args);
            })
        .start();

    Thread.sleep(500);

    assertEquals(1, toolNames.size());
    assertEquals("get_weather", toolNames.get(0));
    assertTrue(toolArgs.get(0).contains("Tokyo"));
  }

  @Test
  void withToolStore_autoExecutesTools() throws Exception {
    String sseResponse =
        """
        data: {"type":"response.function_call_arguments.done","item_id":"call-123","output_index":0,"name":"test_tool","arguments":"{\\"value\\":\\"test\\"}","sequence_number":1}

        data: [DONE]
        """;

    mockWebServer.enqueue(
        new MockResponse()
            .setBody(sseResponse)
            .addHeader("Content-Type", "text/event-stream")
            .setResponseCode(200));

    // Create a simple test tool
    var testTool = new TestFunctionTool();
    var toolStore = FunctionToolStore.create(objectMapper).add(testTool);

    var payload =
        CreateResponsePayload.builder().model("gpt-4o").addUserMessage("Test").streaming().build();

    List<String> resultNames = new ArrayList<>();
    List<FunctionToolCallOutput> resultOutputs = new ArrayList<>();

    responder
        .respond(payload)
        .withToolStore(toolStore)
        .onToolResult(
            (name, output) -> {
              resultNames.add(name);
              resultOutputs.add(output);
            })
        .start();

    Thread.sleep(500);

    assertEquals(1, resultNames.size());
    assertEquals("test_tool", resultNames.get(0));
    assertEquals("success: test", resultOutputs.get(0).output().toString());
  }

  // ===== Parsed Complete Callback Tests =====

  @Test
  void onParsedComplete_receivesParsedResponse() throws Exception {
    String sseResponse =
        """
        data: {"type":"response.output_text.delta","item_id":"msg-1","output_index":0,"content_index":0,"delta":"{\\"name\\":\\"Bob\\",\\"age\\":30}","sequence_number":1}

        data: {"type":"response.completed","response":{"id":"resp-1","object":"response","created_at":1234567890,"status":"completed","status_details":null,"output":[{"type":"message","id":"msg-1","status":"completed","role":"assistant","content":[{"type":"output_text","text":"{\\"name\\":\\"Bob\\",\\"age\\":30}","annotations":[]}]}],"error":null,"metadata":null,"usage":null,"incomplete_details":null,"model":"gpt-4o","output_text":"{\\"name\\":\\"Bob\\",\\"age\\":30}"},"sequence_number":2}

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
            .withStructuredOutput(TestPerson.class)
            .streaming()
            .build();

    CompletableFuture<ParsedResponse<TestPerson>> parsedFuture = new CompletableFuture<>();

    responder.respond(payload).onParsedComplete(parsedFuture::complete).start();

    ParsedResponse<TestPerson> parsed = parsedFuture.get(5, TimeUnit.SECONDS);
    assertNotNull(parsed);
    assertEquals("Bob", parsed.outputParsed().name());
    assertEquals(30, parsed.outputParsed().age());
  }

  // ===== Test helper classes =====

  public record TestPerson(String name, int age) {}

  public record TestToolParams(String value) {}

  @com.paragon.responses.annotations.FunctionMetadata(
      name = "test_tool",
      description = "A test tool")
  public static class TestFunctionTool extends FunctionTool<TestToolParams> {
    @Override
    public @org.jspecify.annotations.NonNull FunctionToolCallOutput call(
        @org.jspecify.annotations.Nullable TestToolParams params) {
      return FunctionToolCallOutput.success(
          "success: " + (params != null ? params.value() : "null"));
    }
  }
}
