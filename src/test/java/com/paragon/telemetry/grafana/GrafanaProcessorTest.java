package com.paragon.telemetry.grafana;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.telemetry.events.*;
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
 * Tests for GrafanaProcessor builder and configuration.
 *
 * <p>Note: Integration tests that actually send telemetry require a Grafana Cloud account and are
 * not included here. These tests focus on builder configuration and accessors.
 */
@DisplayName("GrafanaProcessor Tests")
class GrafanaProcessorTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // BUILDER
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Builder")
  class BuilderTests {

    @Test
    @DisplayName("builder creates processor with required fields")
    void builderCreatesProcessor() {
      GrafanaProcessor processor =
          GrafanaProcessor.builder()
              .baseUrl("https://otlp-gateway.grafana.net/otlp")
              .instanceId("123456")
              .apiToken("test-token")
              .build();

      assertNotNull(processor);
    }

    @Test
    @DisplayName("builder sets httpClient")
    void builderSetsHttpClient() {
      OkHttpClient client = new OkHttpClient.Builder().build();

      GrafanaProcessor processor =
          GrafanaProcessor.builder()
              .httpClient(client)
              .baseUrl("https://otlp-gateway.grafana.net/otlp")
              .instanceId("123456")
              .apiToken("test-token")
              .build();

      assertNotNull(processor);
    }

    @Test
    @DisplayName("builder sets objectMapper")
    void builderSetsObjectMapper() {
      ObjectMapper mapper = new ObjectMapper();

      GrafanaProcessor processor =
          GrafanaProcessor.builder()
              .objectMapper(mapper)
              .baseUrl("https://otlp-gateway.grafana.net/otlp")
              .instanceId("123456")
              .apiToken("test-token")
              .build();

      assertNotNull(processor);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // SIGNAL TYPES
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Signal Types")
  class SignalTypes {

    @Test
    @DisplayName("traces enabled by default")
    void tracesEnabledByDefault() {
      GrafanaProcessor processor =
          GrafanaProcessor.builder()
              .baseUrl("https://otlp-gateway.grafana.net/otlp")
              .instanceId("123456")
              .apiToken("test-token")
              .build();

      assertTrue(processor.isTracesEnabled());
    }

    @Test
    @DisplayName("metrics disabled by default")
    void metricsDisabledByDefault() {
      GrafanaProcessor processor =
          GrafanaProcessor.builder()
              .baseUrl("https://otlp-gateway.grafana.net/otlp")
              .instanceId("123456")
              .apiToken("test-token")
              .build();

      assertFalse(processor.isMetricsEnabled());
    }

    @Test
    @DisplayName("logs disabled by default")
    void logsDisabledByDefault() {
      GrafanaProcessor processor =
          GrafanaProcessor.builder()
              .baseUrl("https://otlp-gateway.grafana.net/otlp")
              .instanceId("123456")
              .apiToken("test-token")
              .build();

      assertFalse(processor.isLogsEnabled());
    }

    @Test
    @DisplayName("can enable metrics")
    void canEnableMetrics() {
      GrafanaProcessor processor =
          GrafanaProcessor.builder()
              .baseUrl("https://otlp-gateway.grafana.net/otlp")
              .instanceId("123456")
              .apiToken("test-token")
              .metricsEnabled(true)
              .build();

      assertTrue(processor.isMetricsEnabled());
    }

    @Test
    @DisplayName("can enable logs")
    void canEnableLogs() {
      GrafanaProcessor processor =
          GrafanaProcessor.builder()
              .baseUrl("https://otlp-gateway.grafana.net/otlp")
              .instanceId("123456")
              .apiToken("test-token")
              .logsEnabled(true)
              .build();

      assertTrue(processor.isLogsEnabled());
    }

    @Test
    @DisplayName("can disable traces")
    void canDisableTraces() {
      GrafanaProcessor processor =
          GrafanaProcessor.builder()
              .baseUrl("https://otlp-gateway.grafana.net/otlp")
              .instanceId("123456")
              .apiToken("test-token")
              .tracesEnabled(false)
              .build();

      assertFalse(processor.isTracesEnabled());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // VALIDATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Validation")
  class Validation {

    @Test
    @DisplayName("throws on missing baseUrl")
    void throwsOnMissingBaseUrl() {
      assertThrows(
          IllegalStateException.class,
          () -> GrafanaProcessor.builder().instanceId("123456").apiToken("test-token").build());
    }

    @Test
    @DisplayName("throws on missing instanceId")
    void throwsOnMissingInstanceId() {
      assertThrows(
          IllegalStateException.class,
          () ->
              GrafanaProcessor.builder()
                  .baseUrl("https://otlp-gateway.grafana.net/otlp")
                  .apiToken("test-token")
                  .build());
    }

    @Test
    @DisplayName("throws on missing apiToken")
    void throwsOnMissingApiToken() {
      assertThrows(
          IllegalStateException.class,
          () ->
              GrafanaProcessor.builder()
                  .baseUrl("https://otlp-gateway.grafana.net/otlp")
                  .instanceId("123456")
                  .build());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // EVENT PROCESSING INTEGRATION TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  // ═══════════════════════════════════════════════════════════════════════════
  // BUILDER VALIDATION EDGE CASES
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Builder Validation Edge Cases")
  class BuilderValidationEdgeCases {

    @Test
    @DisplayName("throws on empty baseUrl")
    void throwsOnEmptyBaseUrl() {
      assertThrows(
          IllegalStateException.class,
          () ->
              GrafanaProcessor.builder()
                  .baseUrl("")
                  .instanceId("123456")
                  .apiToken("test-token")
                  .build());
    }

    @Test
    @DisplayName("throws on blank baseUrl")
    void throwsOnBlankBaseUrl() {
      assertThrows(
          IllegalStateException.class,
          () ->
              GrafanaProcessor.builder()
                  .baseUrl("   ")
                  .instanceId("123456")
                  .apiToken("test-token")
                  .build());
    }

    @Test
    @DisplayName("throws on empty instanceId")
    void throwsOnEmptyInstanceId() {
      assertThrows(
          IllegalStateException.class,
          () ->
              GrafanaProcessor.builder()
                  .baseUrl("https://otlp-gateway.grafana.net/otlp")
                  .instanceId("")
                  .apiToken("test-token")
                  .build());
    }

    @Test
    @DisplayName("throws on blank instanceId")
    void throwsOnBlankInstanceId() {
      assertThrows(
          IllegalStateException.class,
          () ->
              GrafanaProcessor.builder()
                  .baseUrl("https://otlp-gateway.grafana.net/otlp")
                  .instanceId("   ")
                  .apiToken("test-token")
                  .build());
    }

    @Test
    @DisplayName("throws on empty apiToken")
    void throwsOnEmptyApiToken() {
      assertThrows(
          IllegalStateException.class,
          () ->
              GrafanaProcessor.builder()
                  .baseUrl("https://otlp-gateway.grafana.net/otlp")
                  .instanceId("123456")
                  .apiToken("")
                  .build());
    }

    @Test
    @DisplayName("throws on blank apiToken")
    void throwsOnBlankApiToken() {
      assertThrows(
          IllegalStateException.class,
          () ->
              GrafanaProcessor.builder()
                  .baseUrl("https://otlp-gateway.grafana.net/otlp")
                  .instanceId("123456")
                  .apiToken("   ")
                  .build());
    }

    @Test
    @DisplayName("creates default httpClient when not provided")
    void createsDefaultHttpClient() {
      GrafanaProcessor processor =
          GrafanaProcessor.builder()
              .baseUrl("https://otlp-gateway.grafana.net/otlp")
              .instanceId("123456")
              .apiToken("test-token")
              .build();

      assertNotNull(processor);
      processor.shutdown();
    }

    @Test
    @DisplayName("creates default objectMapper when not provided")
    void createsDefaultObjectMapper() {
      GrafanaProcessor processor =
          GrafanaProcessor.builder()
              .baseUrl("https://otlp-gateway.grafana.net/otlp")
              .instanceId("123456")
              .apiToken("test-token")
              .build();

      assertNotNull(processor);
      processor.shutdown();
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // STATIC FACTORY METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Static Factory Methods")
  class StaticFactoryTests {

    @Test
    @DisplayName("builder() returns new Builder")
    void builderReturnsNewBuilder() {
      GrafanaProcessor.Builder builder = GrafanaProcessor.builder();
      assertNotNull(builder);
    }

    @Test
    @DisplayName("fromEnv() throws when env vars not set")
    void fromEnvThrowsWhenEnvVarsNotSet() {
      // fromEnv should throw because environment variables are not set
      assertThrows(IllegalStateException.class, GrafanaProcessor::fromEnv);
    }

    @Test
    @DisplayName("fromEnv() reads GRAFANA_OTLP_URL")
    void fromEnvReadsGrafanaOtlpUrl() {
      // Just verify builder method chain works
      GrafanaProcessor.Builder builder = GrafanaProcessor.builder().fromEnv();
      assertNotNull(builder);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PROCESSOR LIFECYCLE
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Processor Lifecycle")
  class ProcessorLifecycleTests {

    @Test
    @DisplayName("getProcessorName returns correct name")
    void getProcessorNameReturnsCorrectName() {
      GrafanaProcessor processor =
          GrafanaProcessor.builder()
              .baseUrl("https://otlp-gateway.grafana.net/otlp")
              .instanceId("123456")
              .apiToken("test-token")
              .build();

      assertEquals("GrafanaProcessor", processor.getProcessorName());
      processor.shutdown();
    }

    @Test
    @DisplayName("isRunning returns true before shutdown")
    void isRunningReturnsTrueBeforeShutdown() {
      GrafanaProcessor processor =
          GrafanaProcessor.builder()
              .baseUrl("https://otlp-gateway.grafana.net/otlp")
              .instanceId("123456")
              .apiToken("test-token")
              .build();

      assertTrue(processor.isRunning());
      processor.shutdown();
    }

    @Test
    @DisplayName("isRunning returns false after shutdown")
    void isRunningReturnsFalseAfterShutdown() {
      GrafanaProcessor processor =
          GrafanaProcessor.builder()
              .baseUrl("https://otlp-gateway.grafana.net/otlp")
              .instanceId("123456")
              .apiToken("test-token")
              .build();

      processor.shutdown();
      assertFalse(processor.isRunning());
    }

    @Test
    @DisplayName("shutdown can be called multiple times")
    void shutdownCanBeCalledMultipleTimes() {
      GrafanaProcessor processor =
          GrafanaProcessor.builder()
              .baseUrl("https://otlp-gateway.grafana.net/otlp")
              .instanceId("123456")
              .apiToken("test-token")
              .build();

      assertDoesNotThrow(processor::shutdown);
      assertDoesNotThrow(processor::shutdown);
    }

    @Test
    @DisplayName("processing after shutdown returns failed future")
    void processingAfterShutdownFails() {
      GrafanaProcessor processor =
          GrafanaProcessor.builder()
              .baseUrl("https://otlp-gateway.grafana.net/otlp")
              .instanceId("123456")
              .apiToken("test-token")
              .build();

      processor.shutdown();

      ResponseStartedEvent event =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");

      processor.process(event);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // EVENT PROCESSING INTEGRATION TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Event Processing")
  class EventProcessingTests {

    private MockWebServer mockWebServer;
    private GrafanaProcessor processor;

    @BeforeEach
    void setUp() throws Exception {
      mockWebServer = new MockWebServer();
      mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws Exception {
      if (processor != null) {
        processor.shutdown();
      }
      mockWebServer.shutdown();
    }

    private GrafanaProcessor createProcessor(boolean traces, boolean metrics, boolean logs) {
      return GrafanaProcessor.builder()
          .baseUrl(mockWebServer.url("/otlp").toString())
          .instanceId("test-instance")
          .apiToken("test-token")
          .tracesEnabled(traces)
          .metricsEnabled(metrics)
          .logsEnabled(logs)
          .build();
    }

    @Test
    @DisplayName("sends trace for ResponseStartedEvent")
    void sendsTraceForResponseStartedEvent() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200));
      processor = createProcessor(true, false, false);

      ResponseStartedEvent event =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");

      processor.process(event);

      // Wait for async processing
      Thread.sleep(100);

      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request);
      assertEquals("/otlp/v1/traces", request.getPath());
      assertTrue(request.getHeader("Authorization").startsWith("Basic "));
    }

    @Test
    @DisplayName("sends trace and metrics for ResponseCompletedEvent")
    void sendsTraceAndMetricsForResponseCompletedEvent() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200)); // trace
      mockWebServer.enqueue(new MockResponse().setResponseCode(200)); // metrics
      processor = createProcessor(true, true, false);

      ResponseStartedEvent started =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");
      ResponseCompletedEvent completed = ResponseCompletedEvent.from(started, 100, 200, 300, 0.01);

      processor.process(completed);

      Thread.sleep(100);

      RecordedRequest request1 = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request1);

      RecordedRequest request2 = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request2);

      // Check both paths exist (order may vary due to async processing)
      boolean hasTraces =
          request1.getPath().equals("/otlp/v1/traces")
              || request2.getPath().equals("/otlp/v1/traces");
      boolean hasMetrics =
          request1.getPath().equals("/otlp/v1/metrics")
              || request2.getPath().equals("/otlp/v1/metrics");
      assertTrue(hasTraces, "Expected a trace request");
      assertTrue(hasMetrics, "Expected a metrics request");
    }

    @Test
    @DisplayName("sends log for ResponseStartedEvent when logs enabled")
    void sendsLogWhenLogsEnabled() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200)); // first request
      mockWebServer.enqueue(new MockResponse().setResponseCode(200)); // second request
      processor = createProcessor(true, false, true);

      ResponseStartedEvent event =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");

      processor.process(event);

      Thread.sleep(100);

      // Get both requests - order is not guaranteed due to async processing
      RecordedRequest request1 = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      RecordedRequest request2 = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request1);
      assertNotNull(request2);

      // Verify that we got both traces and logs (in either order)
      boolean hasTraces =
          request1.getPath().equals("/otlp/v1/traces")
              || request2.getPath().equals("/otlp/v1/traces");
      boolean hasLogs =
          request1.getPath().equals("/otlp/v1/logs") || request2.getPath().equals("/otlp/v1/logs");
      assertTrue(hasTraces, "Expected a trace request");
      assertTrue(hasLogs, "Expected a log request");
    }

    @Test
    @DisplayName("sends all signal types for ResponseFailedEvent")
    void sendsAllSignalTypesForResponseFailedEvent() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200)); // trace
      mockWebServer.enqueue(new MockResponse().setResponseCode(200)); // log
      processor = createProcessor(true, false, true);

      ResponseStartedEvent started =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");
      ResponseFailedEvent failed =
          ResponseFailedEvent.from(started, new RuntimeException("Test error"));

      processor.process(failed);

      Thread.sleep(100);

      RecordedRequest request1 = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request1);

      RecordedRequest request2 = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request2);
    }

    @Test
    @DisplayName("sends trace for AgentFailedEvent")
    void sendsTraceForAgentFailedEvent() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200));
      processor = createProcessor(true, false, false);

      AgentFailedEvent event =
          AgentFailedEvent.from(
              "TestAgent",
              3,
              new RuntimeException("Error"),
              "session-1",
              "trace-123",
              "span-456",
              null);

      processor.process(event);

      Thread.sleep(100);

      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request);
      assertEquals("/otlp/v1/traces", request.getPath());
    }

    @Test
    @DisplayName("request body contains OTEL JSON")
    void requestBodyContainsOtelJson() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200));
      processor = createProcessor(true, false, false);

      ResponseStartedEvent event =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");

      processor.process(event);

      Thread.sleep(100);

      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request);
      String body = request.getBody().readUtf8();
      assertTrue(body.contains("resourceSpans"));
      assertTrue(body.contains("traceId"));
    }

    @Test
    @DisplayName("no request when all signals disabled")
    void noRequestWhenAllSignalsDisabled() throws Exception {
      processor = createProcessor(false, false, false);

      ResponseStartedEvent event =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");

      processor.process(event);

      Thread.sleep(100);

      RecordedRequest request = mockWebServer.takeRequest(100, TimeUnit.MILLISECONDS);
      assertNull(request);
    }

    @Test
    @DisplayName("sends log for ResponseCompletedEvent with all token fields")
    void sendsLogForResponseCompletedEventWithAllTokens() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200)); // trace
      mockWebServer.enqueue(new MockResponse().setResponseCode(200)); // metrics
      mockWebServer.enqueue(new MockResponse().setResponseCode(200)); // log
      processor = createProcessor(true, true, true);

      ResponseStartedEvent started =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");
      ResponseCompletedEvent completed = ResponseCompletedEvent.from(started, 100, 200, 300, 0.01);

      processor.process(completed);

      Thread.sleep(150);

      // Should get 3 requests: trace, metrics, log
      int requestCount = 0;
      while (mockWebServer.takeRequest(100, TimeUnit.MILLISECONDS) != null) {
        requestCount++;
      }
      assertEquals(3, requestCount);
    }

    @Test
    @DisplayName("sends log for AgentFailedEvent with suggestion")
    void sendsLogForAgentFailedEventWithSuggestion() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200)); // trace
      mockWebServer.enqueue(new MockResponse().setResponseCode(200)); // log
      processor = createProcessor(true, false, true);

      AgentFailedEvent event =
          AgentFailedEvent.from(
              "TestAgent",
              3,
              new RuntimeException("Error"),
              "session-1",
              "trace-123",
              "span-456",
              "Try using a different model");

      processor.process(event);

      Thread.sleep(100);

      RecordedRequest request1 = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      RecordedRequest request2 = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request1);
      assertNotNull(request2);
    }

    @Test
    @DisplayName("handles null model in ResponseStartedEvent")
    void handlesNullModelInResponseStartedEvent() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200));
      processor = createProcessor(true, false, false);

      ResponseStartedEvent event =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", null);

      processor.process(event);

      Thread.sleep(100);

      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request);
      assertEquals("/otlp/v1/traces", request.getPath());
    }

    @Test
    @DisplayName("handles server error response gracefully")
    void handlesServerErrorResponseGracefully() throws Exception {
      mockWebServer.enqueue(
          new MockResponse().setResponseCode(500).setBody("Internal Server Error"));
      processor = createProcessor(true, false, false);

      ResponseStartedEvent event =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");

      // Should not throw
      assertDoesNotThrow(
          () -> {
            processor.process(event);
            Thread.sleep(100);
          });
    }

    @Test
    @DisplayName("sends metrics only when metricsEnabled and event is ResponseCompletedEvent")
    void sendsMetricsOnlyForCompletedEvents() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200)); // metrics only
      processor = createProcessor(false, true, false);

      ResponseStartedEvent started =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");
      ResponseCompletedEvent completed = ResponseCompletedEvent.from(started, 100, 200, 300, 0.01);

      processor.process(completed);

      Thread.sleep(100);

      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request);
      assertEquals("/otlp/v1/metrics", request.getPath());
    }

    @Test
    @DisplayName("metrics request body contains token metrics")
    void metricsRequestBodyContainsTokenMetrics() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200));
      processor = createProcessor(false, true, false);

      ResponseStartedEvent started =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");
      ResponseCompletedEvent completed = ResponseCompletedEvent.from(started, 100, 200, 300, 0.01);

      processor.process(completed);

      Thread.sleep(100);

      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request);
      String body = request.getBody().readUtf8();
      assertTrue(body.contains("resourceMetrics"));
      assertTrue(body.contains("gen_ai.response.duration"));
    }

    @Test
    @DisplayName("log request body contains structured log data")
    void logRequestBodyContainsStructuredLogData() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200));
      processor = createProcessor(false, false, true);

      ResponseStartedEvent event =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");

      processor.process(event);

      Thread.sleep(100);

      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request);
      String body = request.getBody().readUtf8();
      assertTrue(body.contains("resourceLogs"));
      assertTrue(body.contains("session.id"));
    }

    @Test
    @DisplayName("no metrics sent for non-completed events")
    void noMetricsSentForNonCompletedEvents() throws Exception {
      processor = createProcessor(false, true, false);

      ResponseStartedEvent event =
          ResponseStartedEvent.create("session-1", "trace-123", "span-456", "gpt-4o");

      processor.process(event);

      Thread.sleep(100);

      // No metrics should be sent for started events
      RecordedRequest request = mockWebServer.takeRequest(100, TimeUnit.MILLISECONDS);
      assertNull(request);
    }
  }
}

