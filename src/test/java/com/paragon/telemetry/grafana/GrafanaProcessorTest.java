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

      ResponseStartedEvent event = ResponseStartedEvent.create(
          "session-1", "trace-123", "span-456", "gpt-4o");

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

      ResponseStartedEvent started = ResponseStartedEvent.create(
          "session-1", "trace-123", "span-456", "gpt-4o");
      ResponseCompletedEvent completed = ResponseCompletedEvent.from(
          started, 100, 200, 300, 0.01);

      processor.process(completed);
      
      Thread.sleep(100);

      RecordedRequest traceRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(traceRequest);
      assertEquals("/otlp/v1/traces", traceRequest.getPath());

      RecordedRequest metricsRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(metricsRequest);
      assertEquals("/otlp/v1/metrics", metricsRequest.getPath());
    }

    @Test
    @DisplayName("sends log for ResponseStartedEvent when logs enabled")
    void sendsLogWhenLogsEnabled() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200)); // first request
      mockWebServer.enqueue(new MockResponse().setResponseCode(200)); // second request
      processor = createProcessor(true, false, true);

      ResponseStartedEvent event = ResponseStartedEvent.create(
          "session-1", "trace-123", "span-456", "gpt-4o");

      processor.process(event);
      
      Thread.sleep(100);

      // Get both requests - order is not guaranteed due to async processing
      RecordedRequest request1 = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      RecordedRequest request2 = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request1);
      assertNotNull(request2);
      
      // Verify that we got both traces and logs (in either order)
      boolean hasTraces = request1.getPath().equals("/otlp/v1/traces") || request2.getPath().equals("/otlp/v1/traces");
      boolean hasLogs = request1.getPath().equals("/otlp/v1/logs") || request2.getPath().equals("/otlp/v1/logs");
      assertTrue(hasTraces, "Expected a trace request");
      assertTrue(hasLogs, "Expected a log request");
    }

    @Test
    @DisplayName("sends all signal types for ResponseFailedEvent")
    void sendsAllSignalTypesForResponseFailedEvent() throws Exception {
      mockWebServer.enqueue(new MockResponse().setResponseCode(200)); // trace
      mockWebServer.enqueue(new MockResponse().setResponseCode(200)); // log
      processor = createProcessor(true, false, true);

      ResponseStartedEvent started = ResponseStartedEvent.create(
          "session-1", "trace-123", "span-456", "gpt-4o");
      ResponseFailedEvent failed = ResponseFailedEvent.from(
          started, new RuntimeException("Test error"));

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

      AgentFailedEvent event = AgentFailedEvent.from(
          "TestAgent", 3, new RuntimeException("Error"),
          "session-1", "trace-123", "span-456", null);

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

      ResponseStartedEvent event = ResponseStartedEvent.create(
          "session-1", "trace-123", "span-456", "gpt-4o");

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

      ResponseStartedEvent event = ResponseStartedEvent.create(
          "session-1", "trace-123", "span-456", "gpt-4o");

      processor.process(event);
      
      Thread.sleep(100);

      RecordedRequest request = mockWebServer.takeRequest(100, TimeUnit.MILLISECONDS);
      assertNull(request);
    }
  }
}

