package com.paragon.telemetry.grafana;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.telemetry.events.*;
import com.paragon.telemetry.otel.*;
import com.paragon.telemetry.processors.TelemetryProcessor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import okhttp3.*;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Telemetry processor that sends telemetry to Grafana Cloud via OTLP/HTTP.
 *
 * <p>Grafana supports traces, metrics, and logs. Each signal type can be individually
 * enabled/disabled via the builder.
 *
 * <p>Default configuration enables traces only. Enable metrics and logs as needed for your
 * observability requirements.
 *
 * @see <a href="https://grafana.com/docs/grafana-cloud/send-data/otlp/">Grafana OTLP Docs</a>
 */
public class GrafanaProcessor extends TelemetryProcessor {

  private static final Logger logger = LoggerFactory.getLogger(GrafanaProcessor.class);
  private static final MediaType JSON = MediaType.get("application/json");

  private final OkHttpClient httpClient;
  private final String tracesEndpoint;
  private final String metricsEndpoint;
  private final String logsEndpoint;
  private final String authHeader;
  private final ObjectMapper objectMapper;
  private final boolean tracesEnabled;
  private final boolean metricsEnabled;
  private final boolean logsEnabled;

  private GrafanaProcessor(
      @NonNull OkHttpClient httpClient,
      @NonNull String baseUrl,
      @NonNull String instanceId,
      @NonNull String apiToken,
      @NonNull ObjectMapper objectMapper,
      boolean tracesEnabled,
      boolean metricsEnabled,
      boolean logsEnabled) {
    super("GrafanaProcessor");
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.tracesEnabled = tracesEnabled;
    this.metricsEnabled = metricsEnabled;
    this.logsEnabled = logsEnabled;

    // Grafana Cloud OTLP endpoints
    this.tracesEndpoint = baseUrl + "/v1/traces";
    this.metricsEndpoint = baseUrl + "/v1/metrics";
    this.logsEndpoint = baseUrl + "/v1/logs";

    // Create Basic auth header: Base64(instanceId:apiToken)
    String credentials = instanceId + ":" + apiToken;
    this.authHeader =
        "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }

  /** Creates a new builder for GrafanaProcessor. */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /** Creates a processor from environment variables. */
  public static @NonNull GrafanaProcessor fromEnv() {
    return builder().fromEnv().build();
  }

  @Override
  protected void doProcess(@NonNull TelemetryEvent event) {
    if (tracesEnabled) {
      sendTrace(event);
    }

    if (metricsEnabled && event instanceof ResponseCompletedEvent completed) {
      sendMetrics(completed);
    }

    if (logsEnabled) {
      sendLog(event);
    }
  }

  /** Sends trace data to Grafana. */
  private void sendTrace(@NonNull TelemetryEvent event) {
    OtelSpan span = convertToSpan(event);
    OtelExportRequest request = OtelExportRequest.forSpan(span);

    try {
      String json = objectMapper.writeValueAsString(request);
      sendToEndpoint(tracesEndpoint, json, "trace");
    } catch (JsonProcessingException e) {
      logger.error("Failed to serialize trace: {}", e.getMessage(), e);
    }
  }

  /** Sends metrics data to Grafana. Creates gauge/counter metrics from the completed event. */
  private void sendMetrics(@NonNull ResponseCompletedEvent event) {
    List<OtelMetric> metrics = new ArrayList<>();
    List<OtelAttribute> attrs =
        List.of(
            OtelAttribute.ofString("session.id", event.sessionId()),
            OtelAttribute.ofString(
                "gen_ai.response.model", event.model() != null ? event.model() : "unknown"));

    // Add duration metric (gauge)
    metrics.add(
        OtelMetric.gauge(
            "gen_ai.response.duration",
            "Response generation duration in milliseconds",
            "ms",
            List.of(OtelDataPoint.gaugeInt(event.durationMs(), attrs))));

    // Add token metrics if available
    if (event.inputTokens() != null) {
      metrics.add(
          OtelMetric.gauge(
              "gen_ai.usage.input_tokens",
              "Number of input tokens used",
              "{token}",
              List.of(OtelDataPoint.gaugeInt(event.inputTokens(), attrs))));
    }
    if (event.outputTokens() != null) {
      metrics.add(
          OtelMetric.gauge(
              "gen_ai.usage.output_tokens",
              "Number of output tokens generated",
              "{token}",
              List.of(OtelDataPoint.gaugeInt(event.outputTokens(), attrs))));
    }
    if (event.totalTokens() != null) {
      metrics.add(
          OtelMetric.gauge(
              "gen_ai.usage.total_tokens",
              "Total number of tokens used",
              "{token}",
              List.of(OtelDataPoint.gaugeInt(event.totalTokens(), attrs))));
    }

    // Export metrics
    OtelMetricsExportRequest request = OtelMetricsExportRequest.forMetrics(metrics);
    try {
      String json = objectMapper.writeValueAsString(request);
      sendToEndpoint(metricsEndpoint, json, "metrics");
    } catch (JsonProcessingException e) {
      logger.error("Failed to serialize metrics: {}", e.getMessage(), e);
    }
  }

  /** Sends log data to Grafana. */
  private void sendLog(@NonNull TelemetryEvent event) {
    List<OtelAttribute> attributes = new ArrayList<>();
    attributes.add(OtelAttribute.ofString("session.id", event.sessionId()));

    OtelLogRecord logRecord;

    switch (event) {
      case ResponseStartedEvent started -> {
        attributes.add(OtelAttribute.ofString("event.name", "response.started"));
        if (started.model() != null) {
          attributes.add(OtelAttribute.ofString("gen_ai.request.model", started.model()));
        }
        logRecord =
            OtelLogRecord.info(
                "Response generation started", attributes, started.traceId(), started.spanId());
      }

      case ResponseCompletedEvent completed -> {
        attributes.add(OtelAttribute.ofString("event.name", "response.completed"));
        if (completed.model() != null) {
          attributes.add(OtelAttribute.ofString("gen_ai.response.model", completed.model()));
        }
        attributes.add(OtelAttribute.ofInt("duration_ms", completed.durationMs()));
        logRecord =
            OtelLogRecord.info(
                "Response generation completed in " + completed.durationMs() + "ms",
                attributes,
                completed.traceId(),
                completed.spanId());
      }

      case ResponseFailedEvent failed -> {
        attributes.add(OtelAttribute.ofString("event.name", "response.failed"));
        attributes.add(OtelAttribute.ofString("exception.type", failed.errorType()));
        attributes.add(OtelAttribute.ofString("exception.message", failed.errorMessage()));
        if (failed.httpStatusCode() != null) {
          attributes.add(OtelAttribute.ofInt("http.status_code", failed.httpStatusCode()));
        }
        logRecord =
            OtelLogRecord.error(
                "Response generation failed: " + failed.errorMessage(),
                attributes,
                failed.traceId(),
                failed.spanId());
      }
    }

    // Export log
    OtelLogsExportRequest request = OtelLogsExportRequest.forLogRecord(logRecord);
    try {
      String json = objectMapper.writeValueAsString(request);
      sendToEndpoint(logsEndpoint, json, "log");
    } catch (JsonProcessingException e) {
      logger.error("Failed to serialize log: {}", e.getMessage(), e);
    }
  }

  /** Converts a TelemetryEvent to an OtelSpan. */
  private @NonNull OtelSpan convertToSpan(@NonNull TelemetryEvent event) {
    OtelSpan.Builder spanBuilder =
        OtelSpan.builder()
            .traceId(event.traceId())
            .spanId(event.spanId())
            .parentSpanId(event.parentSpanId())
            .clientKind();

    List<OtelAttribute> attributes = new ArrayList<>();
    attributes.add(OtelAttribute.ofString("session.id", event.sessionId()));

    switch (event) {
      case ResponseStartedEvent started -> {
        spanBuilder.name("agentle.respond").startTimeNanos(started.timestampNanos());

        if (started.model() != null) {
          attributes.add(OtelAttribute.ofString("gen_ai.request.model", started.model()));
        }
      }

      case ResponseCompletedEvent completed -> {
        spanBuilder
            .name("agentle.respond")
            .startTimeNanos(completed.startTimestampNanos())
            .endTimeNanos(completed.timestampNanos())
            .status(OtelStatus.ok());

        if (completed.model() != null) {
          attributes.add(OtelAttribute.ofString("gen_ai.response.model", completed.model()));
        }
        if (completed.inputTokens() != null) {
          attributes.add(OtelAttribute.ofInt("gen_ai.usage.input_tokens", completed.inputTokens()));
        }
        if (completed.outputTokens() != null) {
          attributes.add(
              OtelAttribute.ofInt("gen_ai.usage.output_tokens", completed.outputTokens()));
        }
        if (completed.totalTokens() != null) {
          attributes.add(OtelAttribute.ofInt("gen_ai.usage.total_tokens", completed.totalTokens()));
        }
        attributes.add(OtelAttribute.ofInt("duration_ms", completed.durationMs()));
      }

      case ResponseFailedEvent failed -> {
        spanBuilder
            .name("agentle.respond")
            .startTimeNanos(failed.startTimestampNanos())
            .endTimeNanos(failed.timestampNanos())
            .status(OtelStatus.error(failed.errorMessage()));

        attributes.add(OtelAttribute.ofString("exception.type", failed.errorType()));
        attributes.add(OtelAttribute.ofString("exception.message", failed.errorMessage()));
        if (failed.httpStatusCode() != null) {
          attributes.add(OtelAttribute.ofInt("http.status_code", failed.httpStatusCode()));
        }
      }
    }

    event.attributes().forEach((key, value) -> attributes.add(OtelAttribute.of(key, value)));

    return spanBuilder.attributes(attributes).build();
  }

  /** Sends JSON payload to the specified endpoint. */
  private void sendToEndpoint(
      @NonNull String endpoint, @NonNull String json, @NonNull String type) {
    Request request =
        new Request.Builder()
            .url(endpoint)
            .header("Authorization", authHeader)
            .header("Content-Type", "application/json")
            .post(RequestBody.create(json, JSON))
            .build();

    httpClient
        .newCall(request)
        .enqueue(
            new Callback() {
              @Override
              public void onFailure(@NonNull Call call, @NonNull IOException e) {
                logger.error("Failed to send {} to Grafana: {}", type, e.getMessage(), e);
              }

              @Override
              public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody body = response.body()) {
                  if (!response.isSuccessful()) {
                    String errorBody = body != null ? body.string() : "No body";
                    logger.error(
                        "Grafana returned error {} for {}: {}", response.code(), type, errorBody);
                  } else {
                    logger.debug("Successfully sent {} to Grafana", type);
                  }
                } catch (IOException e) {
                  logger.error("Error reading Grafana response: {}", e.getMessage(), e);
                }
              }
            });
  }

  /** Returns whether traces are enabled. */
  public boolean isTracesEnabled() {
    return tracesEnabled;
  }

  /** Returns whether metrics are enabled. */
  public boolean isMetricsEnabled() {
    return metricsEnabled;
  }

  /** Returns whether logs are enabled. */
  public boolean isLogsEnabled() {
    return logsEnabled;
  }

  /** Builder for GrafanaProcessor. */
  public static class Builder {
    private OkHttpClient httpClient;
    private String baseUrl;
    private String instanceId;
    private String apiToken;
    private ObjectMapper objectMapper;
    private boolean tracesEnabled = true;
    private boolean metricsEnabled = false;
    private boolean logsEnabled = false;

    /** Sets the HTTP client. */
    public @NonNull Builder httpClient(@NonNull OkHttpClient httpClient) {
      this.httpClient = Objects.requireNonNull(httpClient);
      return this;
    }

    /**
     * Sets the Grafana OTLP base URL (e.g., https://otlp-gateway-prod-us-east-0.grafana.net/otlp).
     */
    public @NonNull Builder baseUrl(@NonNull String baseUrl) {
      this.baseUrl = Objects.requireNonNull(baseUrl);
      return this;
    }

    /** Sets the Grafana Cloud instance ID. */
    public @NonNull Builder instanceId(@NonNull String instanceId) {
      this.instanceId = Objects.requireNonNull(instanceId);
      return this;
    }

    /** Sets the Grafana Cloud API token. */
    public @NonNull Builder apiToken(@NonNull String apiToken) {
      this.apiToken = Objects.requireNonNull(apiToken);
      return this;
    }

    /** Sets the ObjectMapper for JSON serialization. */
    public @NonNull Builder objectMapper(@NonNull ObjectMapper objectMapper) {
      this.objectMapper = Objects.requireNonNull(objectMapper);
      return this;
    }

    /** Enables or disables trace sending. */
    public @NonNull Builder tracesEnabled(boolean enabled) {
      this.tracesEnabled = enabled;
      return this;
    }

    /** Enables or disables metrics sending. */
    public @NonNull Builder metricsEnabled(boolean enabled) {
      this.metricsEnabled = enabled;
      return this;
    }

    /** Enables or disables log sending. */
    public @NonNull Builder logsEnabled(boolean enabled) {
      this.logsEnabled = enabled;
      return this;
    }

    /** Loads configuration from environment variables. */
    public @NonNull Builder fromEnv() {
      this.baseUrl = System.getenv("GRAFANA_OTLP_URL");
      this.instanceId = System.getenv("GRAFANA_INSTANCE_ID");
      this.apiToken = System.getenv("GRAFANA_API_TOKEN");
      return this;
    }

    /** Builds the GrafanaProcessor. */
    public @NonNull GrafanaProcessor build() {
      if (baseUrl == null || baseUrl.isBlank()) {
        throw new IllegalStateException("Grafana OTLP base URL is required");
      }
      if (instanceId == null || instanceId.isBlank()) {
        throw new IllegalStateException("Grafana instance ID is required");
      }
      if (apiToken == null || apiToken.isBlank()) {
        throw new IllegalStateException("Grafana API token is required");
      }
      if (httpClient == null) {
        httpClient = new OkHttpClient();
      }
      if (objectMapper == null) {
        objectMapper = new ObjectMapper();
      }

      return new GrafanaProcessor(
          httpClient,
          baseUrl,
          instanceId,
          apiToken,
          objectMapper,
          tracesEnabled,
          metricsEnabled,
          logsEnabled);
    }
  }
}
