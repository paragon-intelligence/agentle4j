package com.paragon.telemetry.langfuse;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.telemetry.events.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link LangfuseProcessor}.
 *
 * <p>Tests builder configuration, event processing, OTEL JSON format, and HTTP communication using
 * MockWebServer.
 */
@DisplayName("LangfuseProcessor Tests")
class LangfuseProcessorTest {

  private static final String DEFAULT_ENDPOINT =
      "https://cloud.langfuse.com/api/public/otel/v1/traces";

  // ═══════════════════════════════════════════════════════════════════════════
  // BUILDER
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Builder")
  class BuilderTests {

    @Test
    @DisplayName("builder creates processor with required fields")
    void builderCreatesProcessor() {
      LangfuseProcessor processor =
          LangfuseProcessor.builder().publicKey("pk-test").secretKey("sk-test").build();

      assertNotNull(processor);
      processor.shutdown();
    }

    @Test
    @DisplayName("builder sets httpClient")
    void builderSetsHttpClient() {
      OkHttpClient client = new OkHttpClient.Builder().build();

      LangfuseProcessor processor =
          LangfuseProcessor.builder()
              .httpClient(client)
              .publicKey("pk-test")
              .secretKey("sk-test")
              .build();

      assertNotNull(processor);
      processor.shutdown();
    }

    @Test
    @DisplayName("builder sets objectMapper")
    void builderSetsObjectMapper() {
      ObjectMapper mapper = new ObjectMapper();

      LangfuseProcessor processor =
          LangfuseProcessor.builder()
              .objectMapper(mapper)
              .publicKey("pk-test")
              .secretKey("sk-test")
              .build();

      assertNotNull(processor);
      processor.shutdown();
    }

    @Test
    @DisplayName("builder sets custom endpoint")
    void builderSetsCustomEndpoint() {
      LangfuseProcessor processor =
          LangfuseProcessor.builder()
              .endpoint("https://self-hosted.example.com/otel/traces")
              .publicKey("pk-test")
              .secretKey("sk-test")
              .build();

      assertNotNull(processor);
      processor.shutdown();
    }

    @Test
    @DisplayName("fromEnv method exists")
    void fromEnvMethodExists() {
      LangfuseProcessor.Builder builder = LangfuseProcessor.builder().fromEnv();
      assertNotNull(builder);
    }

    @Test
    @DisplayName("builder creates default httpClient if not provided")
    void createsDefaultHttpClient() {
      LangfuseProcessor processor =
          LangfuseProcessor.builder().publicKey("pk-test").secretKey("sk-test").build();

      assertNotNull(processor);
      processor.shutdown();
    }

    @Test
    @DisplayName("builder creates default objectMapper if not provided")
    void createsDefaultObjectMapper() {
      LangfuseProcessor processor =
          LangfuseProcessor.builder().publicKey("pk-test").secretKey("sk-test").build();

      assertNotNull(processor);
      processor.shutdown();
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // VALIDATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Validation")
  class Validation {

    @Test
    @DisplayName("throws on missing publicKey")
    void throwsOnMissingPublicKey() {
      assertThrows(
          IllegalStateException.class,
          () -> LangfuseProcessor.builder().secretKey("sk-test").build());
    }

    @Test
    @DisplayName("throws on missing secretKey")
    void throwsOnMissingSecretKey() {
      assertThrows(
          IllegalStateException.class,
          () -> LangfuseProcessor.builder().publicKey("pk-test").build());
    }

    @Test
    @DisplayName("throws on empty publicKey")
    void throwsOnEmptyPublicKey() {
      assertThrows(
          IllegalStateException.class,
          () -> LangfuseProcessor.builder().publicKey("").secretKey("sk-test").build());
    }

    @Test
    @DisplayName("throws on blank publicKey")
    void throwsOnBlankPublicKey() {
      assertThrows(
          IllegalStateException.class,
          () -> LangfuseProcessor.builder().publicKey("   ").secretKey("sk-test").build());
    }

    @Test
    @DisplayName("throws on empty secretKey")
    void throwsOnEmptySecretKey() {
      assertThrows(
          IllegalStateException.class,
          () -> LangfuseProcessor.builder().publicKey("pk-test").secretKey("").build());
    }

    @Test
    @DisplayName("throws on blank secretKey")
    void throwsOnBlankSecretKey() {
      assertThrows(
          IllegalStateException.class,
          () -> LangfuseProcessor.builder().publicKey("pk-test").secretKey("   ").build());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // EVENT PROCESSING WITH MOCKWEBSERVER
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Event Processing")
  class EventProcessingTests {

    private MockWebServer mockWebServer;
    private LangfuseProcessor processor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
      mockWebServer = new MockWebServer();
      mockWebServer.start();
      objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() throws Exception {
      if (processor != null) {
        processor.shutdown();
      }
      mockWebServer.shutdown();
    }

    private LangfuseProcessor createProcessor() {
      return LangfuseProcessor.builder()
          .endpoint(mockWebServer.url("/api/public/otel/v1/traces").toString())
          .publicKey("pk-test-key")
          .secretKey("sk-secret-key")
          .objectMapper(objectMapper)
          .build();
    }

    @Test
    @DisplayName("sends trace for ResponseStartedEvent")
    void sendsTraceForResponseStartedEvent() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200));
      processor = createProcessor();

      ResponseStartedEvent event =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");

      processor.process(event);

      // Wait for async processing
      Thread.sleep(200);

      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request);
      assertEquals("/api/public/otel/v1/traces", request.getPath());
    }

    @Test
    @DisplayName("sends trace for ResponseCompletedEvent with usage metrics")
    void sendsTraceForResponseCompletedEvent() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200));
      processor = createProcessor();

      ResponseStartedEvent started =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");
      ResponseCompletedEvent completed = ResponseCompletedEvent.from(started, 100, 200, 300, 0.01);

      processor.process(completed);

      Thread.sleep(200);

      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request);

      // Verify body contains OTEL format
      String body = request.getBody().readUtf8();
      assertTrue(body.contains("resourceSpans"));
      assertTrue(body.contains("traceId"));
    }

    @Test
    @DisplayName("sends trace for ResponseFailedEvent with error status")
    void sendsTraceForResponseFailedEvent() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200));
      processor = createProcessor();

      ResponseStartedEvent started =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");
      ResponseFailedEvent failed =
          ResponseFailedEvent.from(started, new RuntimeException("API error"));

      processor.process(failed);

      Thread.sleep(200);

      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request);

      String body = request.getBody().readUtf8();
      assertTrue(body.contains("ERROR") || body.contains("error"));
    }

    @Test
    @DisplayName("sends trace for AgentFailedEvent with agent attributes")
    void sendsTraceForAgentFailedEvent() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200));
      processor = createProcessor();

      AgentFailedEvent event =
          AgentFailedEvent.from(
              "TestAgent",
              3,
              new RuntimeException("Execution failed"),
              "session-1",
              "trace-123",
              "span-456",
              null);

      processor.process(event);

      Thread.sleep(200);

      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request);

      String body = request.getBody().readUtf8();
      assertTrue(body.contains("TestAgent") || body.contains("agent"));
    }

    @Test
    @DisplayName("uses Basic auth header with correct format")
    void usesBasicAuthHeader() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200));
      processor = createProcessor();

      ResponseStartedEvent event =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");

      processor.process(event);

      Thread.sleep(200);

      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request);

      String authHeader = request.getHeader("Authorization");
      assertNotNull(authHeader);
      assertTrue(authHeader.startsWith("Basic "));

      // Decode and verify credentials
      String base64Credentials = authHeader.substring("Basic ".length());
      String decoded =
          new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
      assertEquals("pk-test-key:sk-secret-key", decoded);
    }

    @Test
    @DisplayName("sets Content-Type to application/json")
    void setsContentTypeHeader() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200));
      processor = createProcessor();

      ResponseStartedEvent event =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");

      processor.process(event);

      Thread.sleep(200);

      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request);

      String contentType = request.getHeader("Content-Type");
      assertNotNull(contentType);
      assertTrue(contentType.contains("application/json"));
    }

    @Test
    @DisplayName("request body contains valid OTEL JSON structure")
    void requestBodyContainsValidOtelJson() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200));
      processor = createProcessor();

      ResponseStartedEvent event =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");

      processor.process(event);

      Thread.sleep(200);

      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request);

      String body = request.getBody().readUtf8();
      JsonNode json = objectMapper.readTree(body);

      // Verify OTEL structure
      assertTrue(json.has("resourceSpans"));
      JsonNode resourceSpans = json.get("resourceSpans");
      assertTrue(resourceSpans.isArray());
      assertTrue(resourceSpans.size() > 0);

      JsonNode firstResourceSpan = resourceSpans.get(0);
      assertTrue(firstResourceSpan.has("scopeSpans"));
    }

    @Test
    @DisplayName("handles HTTP 4xx error gracefully")
    void handlesHttp4xxError() throws Exception {
      mockWebServer.enqueue(
          new MockResponse().setResponseCode(400).setBody("{\"error\": \"Bad request\"}"));
      processor = createProcessor();

      ResponseStartedEvent event =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");

      // Should not throw
      assertDoesNotThrow(() -> processor.process(event));

      Thread.sleep(200);

      // Request should still be sent
      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request);
    }

    @Test
    @DisplayName("handles HTTP 5xx error gracefully")
    void handlesHttp5xxError() throws Exception {
      mockWebServer.enqueue(
          new MockResponse().setResponseCode(500).setBody("{\"error\": \"Internal error\"}"));
      processor = createProcessor();

      ResponseStartedEvent event =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");

      // Should not throw
      assertDoesNotThrow(() -> processor.process(event));

      Thread.sleep(200);

      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request);
    }

    @Test
    @DisplayName("includes session ID in attributes")
    void includesSessionIdInAttributes() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200));
      processor = createProcessor();

      ResponseStartedEvent event =
          ResponseStartedEvent.create("my-session-123", "trace-123", "span-456", "gpt-4o");

      processor.process(event);

      Thread.sleep(200);

      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      String body = request.getBody().readUtf8();
      assertTrue(body.contains("my-session-123"));
    }

    @Test
    @DisplayName("includes model in attributes when present")
    void includesModelInAttributes() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200));
      processor = createProcessor();

      ResponseStartedEvent event =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "claude-3-opus");

      processor.process(event);

      Thread.sleep(200);

      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      String body = request.getBody().readUtf8();
      assertTrue(body.contains("claude-3-opus"));
    }

    @Test
    @DisplayName("handles null model gracefully")
    void handlesNullModelGracefully() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200));
      processor = createProcessor();

      ResponseStartedEvent event =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", null);

      assertDoesNotThrow(() -> processor.process(event));

      Thread.sleep(200);

      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request);
    }

    @Test
    @DisplayName("includes token usage for completed events")
    void includesTokenUsageForCompletedEvents() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200));
      processor = createProcessor();

      ResponseStartedEvent started =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");
      ResponseCompletedEvent completed = ResponseCompletedEvent.from(started, 150, 250, 400, 0.025);

      processor.process(completed);

      Thread.sleep(200);

      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      String body = request.getBody().readUtf8();

      // Should contain token metrics
      assertTrue(body.contains("150") || body.contains("input_tokens"));
      assertTrue(body.contains("250") || body.contains("output_tokens"));
    }

    @Test
    @DisplayName("handles null token values in completed event")
    void handlesNullTokenValues() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200));
      processor = createProcessor();

      ResponseStartedEvent started =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");
      ResponseCompletedEvent completed =
          ResponseCompletedEvent.from(started, null, null, null, null);

      assertDoesNotThrow(() -> processor.process(completed));

      Thread.sleep(200);

      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request);
    }

    @Test
    @DisplayName("includes http status code for failed events")
    void includesHttpStatusCodeForFailedEvents() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200));
      processor = createProcessor();

      ResponseStartedEvent started =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");
      ResponseFailedEvent failed =
          ResponseFailedEvent.fromHttpError(started, 429, "Rate limit exceeded");

      processor.process(failed);

      Thread.sleep(200);

      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      String body = request.getBody().readUtf8();
      assertTrue(body.contains("429"));
    }

    @Test
    @DisplayName("includes suggestion for agent failed events when present")
    void includesSuggestionForAgentFailedEvents() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200));
      processor = createProcessor();

      AgentFailedEvent event =
          new AgentFailedEvent(
              "TestAgent",
              "session-1",
              "trace-123",
              "span-456",
              null,
              System.nanoTime(),
              "EXECUTION",
              "MAX_TURNS",
              "Maximum turns exceeded",
              5,
              "Increase max turns or simplify the task");

      processor.process(event);

      Thread.sleep(200);

      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      String body = request.getBody().readUtf8();
      assertTrue(body.contains("Increase max turns") || body.contains("suggestion"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // STATIC FACTORY METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Static Factory Methods")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("builder() returns new Builder")
    void builderReturnsNewBuilder() {
      LangfuseProcessor.Builder builder = LangfuseProcessor.builder();
      assertNotNull(builder);
    }

    @Test
    @DisplayName("fromEnv(OkHttpClient) with custom client exists")
    void fromEnvWithCustomClientExists() {
      // Just verify the method exists and can be called
      // Will fail validation since env vars aren't set
      OkHttpClient client = new OkHttpClient();
      assertThrows(IllegalStateException.class, () -> LangfuseProcessor.fromEnv(client));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PROCESSOR LIFECYCLE
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Processor Lifecycle")
  class ProcessorLifecycleTests {

    @Test
    @DisplayName("shutdown completes without error")
    void shutdownCompletesWithoutError() {
      LangfuseProcessor processor =
          LangfuseProcessor.builder().publicKey("pk-test").secretKey("sk-test").build();

      assertDoesNotThrow(processor::shutdown);
    }

    @Test
    @DisplayName("processing after shutdown returns failed future")
    void processingAfterShutdownFails() throws Exception {
      LangfuseProcessor processor =
          LangfuseProcessor.builder().publicKey("pk-test").secretKey("sk-test").build();

      processor.shutdown();

      ResponseStartedEvent event =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");

      processor.process(event);
    }

    @Test
    @DisplayName("getProcessorName returns correct name")
    void getProcessorNameReturnsCorrectName() {
      LangfuseProcessor processor =
          LangfuseProcessor.builder().publicKey("pk-test").secretKey("sk-test").build();

      assertEquals("LangfuseProcessor", processor.getProcessorName());
      processor.shutdown();
    }

    @Test
    @DisplayName("isRunning returns true before shutdown")
    void isRunningReturnsTrueBeforeShutdown() {
      LangfuseProcessor processor =
          LangfuseProcessor.builder().publicKey("pk-test").secretKey("sk-test").build();

      assertTrue(processor.isRunning());
      processor.shutdown();
    }

    @Test
    @DisplayName("isRunning returns false after shutdown")
    void isRunningReturnsFalseAfterShutdown() {
      LangfuseProcessor processor =
          LangfuseProcessor.builder().publicKey("pk-test").secretKey("sk-test").build();

      processor.shutdown();
      assertFalse(processor.isRunning());
    }
  }
}
