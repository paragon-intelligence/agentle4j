package com.paragon.telemetry.grafana;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
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
}
