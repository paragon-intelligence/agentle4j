package com.paragon.telemetry.langfuse;

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
 * Telemetry processor that sends traces to Langfuse via OTLP/HTTP.
 *
 * <p>Langfuse only supports traces (not metrics or logs), so this processor converts
 * TelemetryEvents into OtelSpans and sends them to the {@code /api/public/otel/v1/traces} endpoint.
 *
 * <p>Authentication uses HTTP Basic Auth with public key as username and secret key as password.
 *
 * @see <a href="https://langfuse.com/docs/integrations/opentelemetry">Langfuse OpenTelemetry
 *     Docs</a>
 */
public class LangfuseProcessor extends TelemetryProcessor {

  private static final Logger logger = LoggerFactory.getLogger(LangfuseProcessor.class);
  private static final MediaType JSON = MediaType.get("application/json");
  private static final String DEFAULT_ENDPOINT =
      "https://cloud.langfuse.com/api/public/otel/v1/traces";

  private final OkHttpClient httpClient;
  private final String endpoint;
  private final String authHeader;
  private final ObjectMapper objectMapper;

  private LangfuseProcessor(
      @NonNull OkHttpClient httpClient,
      @NonNull String endpoint,
      @NonNull String publicKey,
      @NonNull String secretKey,
      @NonNull ObjectMapper objectMapper) {
    super("LangfuseProcessor");
    this.httpClient = httpClient;
    this.endpoint = endpoint;
    this.objectMapper = objectMapper;

    // Create Basic auth header: Base64(publicKey:secretKey)
    String credentials = publicKey + ":" + secretKey;
    this.authHeader =
        "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }

  /** Creates a new builder for LangfuseProcessor. */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Creates a processor from environment variables. Requires LANGFUSE_PUBLIC_KEY and
   * LANGFUSE_SECRET_KEY.
   */
  public static @NonNull LangfuseProcessor fromEnv() {
    return builder().fromEnv().build();
  }

  /** Creates a processor from environment variables with a custom HTTP client. */
  public static @NonNull LangfuseProcessor fromEnv(@NonNull OkHttpClient httpClient) {
    return builder().fromEnv().httpClient(httpClient).build();
  }

  @Override
  protected void doProcess(@NonNull TelemetryEvent event) {
    OtelSpan span = convertToSpan(event);
    OtelExportRequest request = OtelExportRequest.forSpan(span);

    try {
      String json = objectMapper.writeValueAsString(request);
      sendToLangfuse(json);
    } catch (JsonProcessingException e) {
      logger.error("Failed to serialize OTEL request: {}", e.getMessage(), e);
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
    attributes.add(OtelAttribute.ofString("langfuse.session.id", event.sessionId()));

    // Add event-specific attributes
    switch (event) {
      case ResponseStartedEvent started -> {
        spanBuilder.name("respond").startTimeNanos(started.timestampNanos());

        if (started.model() != null) {
          attributes.add(OtelAttribute.ofString("gen_ai.request.model", started.model()));
        }
        attributes.add(OtelAttribute.ofString("langfuse.observation.type", "generation"));
      }

      case ResponseCompletedEvent completed -> {
        spanBuilder
            .name("respond")
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
        if (completed.costUsd() != null) {
          attributes.add(OtelAttribute.ofDouble("gen_ai.usage.cost", completed.costUsd()));
        }
        attributes.add(
            OtelAttribute.ofInt("langfuse.observation.latency_ms", completed.durationMs()));
        attributes.add(OtelAttribute.ofString("langfuse.observation.type", "generation"));
      }

      case ResponseFailedEvent failed -> {
        spanBuilder
            .name("respond")
            .startTimeNanos(failed.startTimestampNanos())
            .endTimeNanos(failed.timestampNanos())
            .status(OtelStatus.error(failed.errorMessage()));

        if (failed.model() != null) {
          attributes.add(OtelAttribute.ofString("gen_ai.request.model", failed.model()));
        }
        attributes.add(OtelAttribute.ofString("exception.type", failed.errorType()));
        attributes.add(OtelAttribute.ofString("exception.message", failed.errorMessage()));
        if (failed.httpStatusCode() != null) {
          attributes.add(OtelAttribute.ofInt("http.status_code", failed.httpStatusCode()));
        }
        attributes.add(OtelAttribute.ofString("langfuse.observation.type", "generation"));
        attributes.add(OtelAttribute.ofString("langfuse.observation.level", "ERROR"));
      }

      case AgentFailedEvent agentFailed -> {
        spanBuilder
            .name("agent." + agentFailed.agentName())
            .startTimeNanos(agentFailed.timestampNanos() - 1_000_000_000L)  // Estimate 1s earlier
            .endTimeNanos(agentFailed.timestampNanos())
            .status(OtelStatus.error(agentFailed.errorMessage()));

        attributes.add(OtelAttribute.ofString("agent.name", agentFailed.agentName()));
        attributes.add(OtelAttribute.ofString("agent.phase", agentFailed.phase()));
        attributes.add(OtelAttribute.ofString("error.code", agentFailed.errorCode()));
        attributes.add(OtelAttribute.ofString("exception.type", agentFailed.errorCode()));
        attributes.add(OtelAttribute.ofString("exception.message", agentFailed.errorMessage()));
        attributes.add(OtelAttribute.ofInt("agent.turns_completed", agentFailed.turnsCompleted()));
        if (agentFailed.suggestion() != null) {
          attributes.add(OtelAttribute.ofString("error.suggestion", agentFailed.suggestion()));
        }
        attributes.add(OtelAttribute.ofString("langfuse.observation.type", "span"));
        attributes.add(OtelAttribute.ofString("langfuse.observation.level", "ERROR"));
      }
    }

    // Add custom attributes from event
    event.attributes().forEach((key, value) -> attributes.add(OtelAttribute.of(key, value)));

    return spanBuilder.attributes(attributes).build();
  }

  /** Sends JSON payload to Langfuse OTEL endpoint. */
  private void sendToLangfuse(@NonNull String json) {
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
                logger.error("Failed to send trace to Langfuse: {}", e.getMessage(), e);
              }

              @Override
              public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody body = response.body()) {
                  if (!response.isSuccessful()) {
                    String errorBody = body != null ? body.string() : "No body";
                    logger.error("Langfuse returned error {}: {}", response.code(), errorBody);
                  } else {
                    logger.debug("Successfully sent trace to Langfuse");
                  }
                } catch (IOException e) {
                  logger.error("Error reading Langfuse response: {}", e.getMessage(), e);
                }
              }
            });
  }

  /** Builder for LangfuseProcessor. */
  public static class Builder {
    private OkHttpClient httpClient;
    private String endpoint = DEFAULT_ENDPOINT;
    private String publicKey;
    private String secretKey;
    private ObjectMapper objectMapper;

    /** Sets the HTTP client. */
    public @NonNull Builder httpClient(@NonNull OkHttpClient httpClient) {
      this.httpClient = Objects.requireNonNull(httpClient);
      return this;
    }

    /** Sets the Langfuse endpoint URL. */
    public @NonNull Builder endpoint(@NonNull String endpoint) {
      this.endpoint = Objects.requireNonNull(endpoint);
      return this;
    }

    /** Sets the Langfuse public key. */
    public @NonNull Builder publicKey(@NonNull String publicKey) {
      this.publicKey = Objects.requireNonNull(publicKey);
      return this;
    }

    /** Sets the Langfuse secret key. */
    public @NonNull Builder secretKey(@NonNull String secretKey) {
      this.secretKey = Objects.requireNonNull(secretKey);
      return this;
    }

    /** Sets the ObjectMapper for JSON serialization. */
    public @NonNull Builder objectMapper(@NonNull ObjectMapper objectMapper) {
      this.objectMapper = Objects.requireNonNull(objectMapper);
      return this;
    }

    /**
     * Loads configuration from environment variables. Reads LANGFUSE_PUBLIC_KEY,
     * LANGFUSE_SECRET_KEY, and optionally LANGFUSE_HOST.
     */
    public @NonNull Builder fromEnv() {
      this.publicKey = System.getenv("LANGFUSE_PUBLIC_KEY");
      this.secretKey = System.getenv("LANGFUSE_SECRET_KEY");

      String host = System.getenv("LANGFUSE_HOST");
      if (host != null && !host.isBlank()) {
        this.endpoint = host + "/api/public/otel/v1/traces";
      }

      return this;
    }

    /** Builds the LangfuseProcessor. */
    public @NonNull LangfuseProcessor build() {
      if (publicKey == null || publicKey.isBlank()) {
        throw new IllegalStateException("Langfuse public key is required");
      }
      if (secretKey == null || secretKey.isBlank()) {
        throw new IllegalStateException("Langfuse secret key is required");
      }
      if (httpClient == null) {
        httpClient = new OkHttpClient();
      }
      if (objectMapper == null) {
        objectMapper = new ObjectMapper();
      }

      return new LangfuseProcessor(httpClient, endpoint, publicKey, secretKey, objectMapper);
    }
  }
}
