package com.paragon.responses;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.json.JacksonJsonSchemaProducer;
import com.paragon.responses.json.JsonSchemaProducer;
import com.paragon.responses.openrouter.OpenRouterModelRegistry;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.spec.ParsedResponse;
import com.paragon.responses.spec.Response;
import com.paragon.responses.spec.ResponsesAPIProvider;
import com.paragon.responses.streaming.ResponseStream;
import com.paragon.telemetry.TelemetryContext;
import com.paragon.telemetry.events.ResponseCompletedEvent;
import com.paragon.telemetry.events.ResponseFailedEvent;
import com.paragon.telemetry.events.ResponseStartedEvent;
import com.paragon.telemetry.processors.ProcessorRegistry;
import com.paragon.telemetry.processors.TelemetryProcessor;
import com.paragon.telemetry.processors.TraceIdGenerator;
import okhttp3.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Core class for sending requests to the Responses API.
 *
 * <p>Supports OpenTelemetry tracing through configurable telemetry processors. Telemetry is emitted
 * asynchronously and does not impact response latency.
 *
 * <p>When using OpenRouter, an optional {@link OpenRouterModelRegistry} can be provided to
 * calculate request costs based on token usage.
 */
@SuppressWarnings("ClassCanBeRecord")
public class Responder {
  private static final @NonNull ObjectMapper responsesApiObjectMapper =
          ResponsesApiObjectMapper.create();
  private static final @NonNull Logger logger = LoggerFactory.getLogger(Responder.class);
  private static final MediaType JSON = MediaType.get("application/json");

  private final @Nullable ResponsesAPIProvider provider;
  private final @NonNull HttpUrl baseUrl;
  private final @NonNull JsonSchemaProducer jsonSchemaProducer;
  private final @NonNull OkHttpClient httpClient;
  private final @NonNull ObjectMapper objectMapper;
  private final @NonNull Headers headers;
  private final @NonNull ProcessorRegistry telemetryProcessors;
  private final @Nullable OpenRouterModelRegistry modelRegistry;
  private final @NonNull RetryPolicy retryPolicy;

  private Responder(
          @Nullable ResponsesAPIProvider provider,
          @NonNull HttpUrl baseUrl,
          @NonNull JsonSchemaProducer jsonSchemaProducer,
          @NonNull OkHttpClient httpClient,
          @NonNull String apiKey,
          @NonNull ObjectMapper objectMapper,
          @NonNull ProcessorRegistry telemetryProcessors,
          @Nullable OpenRouterModelRegistry modelRegistry,
          @NonNull RetryPolicy retryPolicy) {
    this.provider = provider;
    this.baseUrl = Objects.requireNonNull(baseUrl);
    this.jsonSchemaProducer = Objects.requireNonNull(jsonSchemaProducer);
    this.httpClient = Objects.requireNonNull(httpClient);
    this.objectMapper = objectMapper;
    this.telemetryProcessors = telemetryProcessors;
    this.modelRegistry = modelRegistry;
    this.retryPolicy = Objects.requireNonNull(retryPolicy);
    this.headers =
            Headers.of(
                    "Authorization",
                    String.format("Bearer %s", Objects.requireNonNull(apiKey)),
                    "Content-Type",
                    JSON.toString());
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Sends a request to the API and returns the response. Automatically generates a session ID for
   * telemetry.
   */
  public @NonNull CompletableFuture<Response> respond(@NonNull CreateResponsePayload payload) {
    return respond(payload, UUID.randomUUID().toString(), TelemetryContext.empty());
  }

  /**
   * Sends a request to the API with telemetry context for rich metadata. Automatically generates a
   * session ID.
   *
   * @param payload the request payload
   * @param context telemetry context with user_id, tags, metadata
   * @return a future that completes with the response
   */
  public @NonNull CompletableFuture<Response> respond(
          @NonNull CreateResponsePayload payload, @NonNull TelemetryContext context) {
    return respond(payload, UUID.randomUUID().toString(), context);
  }

  /**
   * Sends a request to the API with a specific session ID for telemetry correlation.
   *
   * @param payload   the request payload
   * @param sessionId unique identifier for this session (used for trace correlation)
   * @return a future that completes with the response
   */
  public @NonNull CompletableFuture<Response> respond(
          @NonNull CreateResponsePayload payload, @NonNull String sessionId) {
    return respond(payload, sessionId, TelemetryContext.empty());
  }

  /**
   * Sends a request to the API with telemetry context for rich metadata.
   *
   * @param payload   the request payload
   * @param sessionId unique identifier for this session (used for trace correlation)
   * @param context   telemetry context with user_id, tags, metadata
   * @return a future that completes with the response
   */
  public @NonNull CompletableFuture<Response> respond(
          @NonNull CreateResponsePayload payload,
          @NonNull String sessionId,
          @NonNull TelemetryContext context) {

    // Use parent trace context if provided, otherwise generate new IDs
    String traceId = context.parentTraceId() != null
            ? context.parentTraceId()
            : TraceIdGenerator.generateTraceId();
    String spanId = TraceIdGenerator.generateSpanId();

    // Emit started event with context (async, non-blocking)
    ResponseStartedEvent startedEvent =
            ResponseStartedEvent.create(sessionId, traceId, spanId, payload.model(), context);
    telemetryProcessors.broadcast(startedEvent);

    Request request = payload.toRequest(responsesApiObjectMapper, JSON, baseUrl, headers);

    return executeWithRetry(request, startedEvent, 0);
  }

  /**
   * Executes an HTTP request with retry logic and exponential backoff.
   */
  private @NonNull CompletableFuture<Response> executeWithRetry(
          @NonNull Request request,
          @NonNull ResponseStartedEvent startedEvent,
          int attempt) {

    CompletableFuture<Response> future = new CompletableFuture<>();

    httpClient
            .newCall(request)
            .enqueue(
                    new Callback() {
                      @Override
                      public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        // Network errors are retryable
                        if (attempt < retryPolicy.maxRetries()) {
                          logger.warn("Request failed (attempt {}), retrying: {}", attempt + 1, e.getMessage());
                          scheduleRetry(request, startedEvent, attempt, future);
                        } else {
                          telemetryProcessors.broadcast(ResponseFailedEvent.from(startedEvent, e));
                          future.completeExceptionally(e);
                        }
                      }

                      @Override
                      public void onResponse(@NonNull Call call, okhttp3.Response response) {
                        try (ResponseBody body = response.body()) {
                          String json = body.string();
                          logger.debug("Raw API Response:\n{}", json);

                          if (!response.isSuccessful()) {
                            // Check if we should retry this status code
                            if (retryPolicy.isRetryable(response.code()) && attempt < retryPolicy.maxRetries()) {
                              logger.warn("Received {} (attempt {}), retrying", response.code(), attempt + 1);
                              scheduleRetry(request, startedEvent, attempt, future);
                              return;
                            }

                            String errorMessage =
                                    String.format(
                                            "API Error: %s %d - %s%nResponse Body: %s",
                                            response.protocol(), response.code(), response.message(), json);
                            logger.error(errorMessage);

                            telemetryProcessors.broadcast(
                                    ResponseFailedEvent.fromHttpError(
                                            startedEvent, response.code(), errorMessage));

                            future.completeExceptionally(new RuntimeException(errorMessage));
                            return;
                          }

                          Response mapped = responsesApiObjectMapper.readValue(json, Response.class);

                          telemetryProcessors.broadcast(
                                  ResponseCompletedEvent.from(startedEvent, null, null, null, null));

                          future.complete(mapped);
                        } catch (Exception e) {
                          telemetryProcessors.broadcast(ResponseFailedEvent.from(startedEvent, e));
                          future.completeExceptionally(e);
                        }
                      }
                    });

    return future;
  }

  /**
   * Schedules a retry with exponential backoff delay.
   */
  private void scheduleRetry(
          @NonNull Request request,
          @NonNull ResponseStartedEvent startedEvent,
          int attempt,
          @NonNull CompletableFuture<Response> originalFuture) {

    java.time.Duration delay = retryPolicy.getDelayForAttempt(attempt + 1);
    logger.debug("Scheduling retry {} after {}ms", attempt + 2, delay.toMillis());

    CompletableFuture.delayedExecutor(delay.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
            .execute(() -> {
              executeWithRetry(request, startedEvent, attempt + 1)
                      .whenComplete((result, error) -> {
                        if (error != null) {
                          originalFuture.completeExceptionally(error);
                        } else {
                          originalFuture.complete(result);
                        }
                      });
            });
  }

  /**
   * Sends a structured output request and parses the response.
   */
  public <T> @NonNull CompletableFuture<ParsedResponse<T>> respond(
          CreateResponsePayload.Structured<T> payload) {
    return respond(payload, UUID.randomUUID().toString());
  }

  /**
   * Sends a structured output request with session ID and parses the response.
   */
  public <T> @NonNull CompletableFuture<ParsedResponse<T>> respond(
          CreateResponsePayload.Structured<T> payload, @NonNull String sessionId) {
    if (payload.hasEmptyText()) {
      throw new IllegalArgumentException("\"payload.text\" parameter cannot be null.");
    }

    if (payload.hasEmptyTextFormat()) {
      throw new IllegalArgumentException("\"payload.text.format\" parameter cannot be null.");
    }

    if (!payload.hasJsonSchemaTextFormat()) {
      throw new IllegalArgumentException(
              """
                      "Format" parameter must be of type TextConfigurationOptionsJsonSchemaFormat.
                      Please, use:
                       <pre>
                       CreateResponsePayload.builder().withStructuredOutput(YourClass.class);
                       </pre>
                      """);
    }

    CompletableFuture<Response> response = respond(((CreateResponsePayload) payload), sessionId);

    return response.thenApply(
            res -> {
              try {
                return res.parse(payload.responseType(), objectMapper);
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            });
  }

  /**
   * Simple text-only respond method.
   */
  public @NonNull CompletableFuture<Response> respond(String input) {
    var payload = CreateResponsePayload.builder().addUserMessage(input);
    if (provider != null && provider.equals(ResponsesAPIProvider.OPEN_ROUTER)) {
      payload.model("openrouter/auto");
    }
    return respond(payload.build());
  }

  /**
   * Shuts down telemetry processors gracefully. Call this when the Responder is no longer needed.
   */
  public void shutdown() {
    telemetryProcessors.shutdown();
  }

  // ===== Streaming Response Methods =====

  /**
   * Sends a streaming request to the API and returns a ResponseStream for processing events. Uses
   * virtual threads for non-blocking async streaming.
   *
   * <p>The builder automatically returns a Streaming payload when stream=true:
   *
   * <pre>{@code
   * var payload = CreateResponsePayload.builder()
   *     .model("gpt-4o")
   *     .addUserMessage("Hello")
   *     .stream(true)
   *     .build();
   *
   * responder.respond(payload)
   *     .onTextDelta(System.out::print)
   *     .onComplete(response -> System.out.println("\nDone!"))
   *     .onError(Throwable::printStackTrace)
   *     .start();
   * }</pre>
   *
   * @param payload the streaming request payload
   * @return a ResponseStream for processing streaming events
   */
  public ResponseStream<Void> respond(
          CreateResponsePayload.Streaming payload) {
    return respond(payload, UUID.randomUUID().toString());
  }

  /**
   * Sends a streaming request with a specific session ID for telemetry correlation.
   *
   * @param payload   the streaming request payload
   * @param sessionId unique identifier for this session (used for trace correlation)
   * @return a ResponseStream for processing streaming events
   */
  public ResponseStream<Void> respond(
          CreateResponsePayload.Streaming payload, @NonNull String sessionId) {
    Request request = payload.toRequest(responsesApiObjectMapper, JSON, baseUrl, headers);
    return new ResponseStream<>(
            httpClient, request, objectMapper, null);
  }

  /**
   * Sends a structured output streaming request. Returns a ResponseStream that can parse the final
   * response to the structured type.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * var payload = CreateResponsePayload.builder()
   *     .model("gpt-4o")
   *     .addUserMessage("Give me a JSON person")
   *     .stream(true)
   *     .withStructuredOutput(Person.class)
   *     .build();
   *
   * ParsedResponse<Person> parsed = responder.respond(payload)
   *     .onTextDelta(System.out::print)
   *     .toParsedFuture()
   *     .get();
   * }</pre>
   *
   * @param payload the structured streaming request payload
   * @param <T>     the structured output type
   * @return a ResponseStream for processing streaming events with structured parsing
   */
  public <T> ResponseStream<T> respond(
          CreateResponsePayload.StructuredStreaming<T> payload) {
    return respond(payload, UUID.randomUUID().toString());
  }

  /**
   * Sends a structured output streaming request with session ID.
   *
   * @param payload   the structured streaming request payload
   * @param sessionId unique identifier for this session
   * @param <T>       the structured output type
   * @return a ResponseStream for processing streaming events with structured parsing
   */
  public <T> ResponseStream<T> respond(
          CreateResponsePayload.StructuredStreaming<T> payload, @NonNull String sessionId) {
    if (payload.hasEmptyText()) {
      throw new IllegalArgumentException(
              "\"payload.text\" parameter cannot be null for structured output.");
    }
    if (payload.hasEmptyTextFormat()) {
      throw new IllegalArgumentException(
              "\"payload.text.format\" parameter cannot be null for structured output.");
    }
    if (!payload.hasJsonSchemaTextFormat()) {
      throw new IllegalArgumentException(
              "\"Format\" parameter must be of type TextConfigurationOptionsJsonSchemaFormat for"
                      + " structured output.");
    }

    Request request = payload.toRequest(responsesApiObjectMapper, JSON, baseUrl, headers);
    return new ResponseStream<>(
            httpClient, request, objectMapper, payload.responseType());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (Responder) obj;
    return Objects.equals(this.provider, that.provider)
            && Objects.equals(this.baseUrl, that.baseUrl)
            && Objects.equals(this.jsonSchemaProducer, that.jsonSchemaProducer)
            && Objects.equals(this.httpClient, that.httpClient);
  }

  @Override
  public int hashCode() {
    return Objects.hash(provider, baseUrl, jsonSchemaProducer, httpClient);
  }

  @Override
  public String toString() {
    return "Responder["
            + "provider="
            + provider
            + ", "
            + "baseUrl="
            + baseUrl
            + ", "
            + "jsonSchemaProducer="
            + jsonSchemaProducer
            + ", "
            + "httpClient="
            + httpClient
            + ']';
  }

  public static final class Builder {
    @NonNull
    private final List<TelemetryProcessor> telemetryProcessors = new ArrayList<>();

    @NonNull
    private JsonSchemaProducer jsonSchemaProducer =
            new JacksonJsonSchemaProducer(new ObjectMapper());

    @NonNull
    private OkHttpClient httpClient = new OkHttpClient();
    @Nullable
    private ResponsesAPIProvider provider = ResponsesAPIProvider.OPEN_ROUTER;
    @Nullable
    private HttpUrl baseUrl = null;
    @Nullable
    private String apiKey = null;
    @NonNull
    private ObjectMapper objectMapper = ResponsesApiObjectMapper.create();
    @NonNull
    private RetryPolicy retryPolicy = RetryPolicy.defaults();

    public Builder openRouter() {
      provider = ResponsesAPIProvider.OPEN_ROUTER;
      return this;
    }

    public Builder httpClient(@NonNull OkHttpClient httpClient) {
      this.httpClient = Objects.requireNonNull(httpClient);
      return this;
    }

    public Builder apiKey(@NonNull String apiKey) {
      this.apiKey = Objects.requireNonNull(apiKey);
      return this;
    }

    public Builder jsonSchemaProducer(@NonNull JsonSchemaProducer jsonSchemaProducer) {
      this.jsonSchemaProducer = Objects.requireNonNull(jsonSchemaProducer);
      return this;
    }

    public Builder openAi() {
      provider = ResponsesAPIProvider.OPENAI;
      return this;
    }

    public Builder provider(@NonNull ResponsesAPIProvider provider) {
      this.provider = Objects.requireNonNull(provider);
      return this;
    }

    /**
     * Sets a custom base URL for API requests.
     */
    public Builder baseUrl(@NonNull HttpUrl baseUrl) {
      this.baseUrl = Objects.requireNonNull(baseUrl);
      this.provider = null;
      return this;
    }

    public Builder objectMapper(@NonNull ObjectMapper objectMapper) {
      this.objectMapper = Objects.requireNonNull(objectMapper);
      return this;
    }

    /**
     * Adds a telemetry processor for OpenTelemetry tracing.
     *
     * @param processor the processor to add (e.g., LangfuseProcessor, GrafanaProcessor)
     * @return this builder
     */
    public Builder addTelemetryProcessor(@NonNull TelemetryProcessor processor) {
      this.telemetryProcessors.add(Objects.requireNonNull(processor));
      return this;
    }

    /**
     * Sets the retry policy for handling transient failures.
     *
     * <p>By default, retries are enabled with 3 attempts and exponential backoff.
     * Use {@link RetryPolicy#disabled()} to disable retries.
     *
     * @param retryPolicy the retry policy to use
     * @return this builder
     */
    public Builder retryPolicy(@NonNull RetryPolicy retryPolicy) {
      this.retryPolicy = Objects.requireNonNull(retryPolicy);
      return this;
    }

    /**
     * Sets the maximum number of retry attempts with default backoff settings.
     *
     * <p>This is a convenience method equivalent to:
     * <pre>{@code
     * .retryPolicy(RetryPolicy.builder().maxRetries(n).build())
     * }</pre>
     *
     * @param maxRetries maximum retry attempts (0 = no retries)
     * @return this builder
     */
    public Builder maxRetries(int maxRetries) {
      this.retryPolicy = RetryPolicy.builder().maxRetries(maxRetries).build();
      return this;
    }

    public @NonNull Responder build() {
      HttpUrl resolvedBaseUrl;
      if (baseUrl != null) {
        resolvedBaseUrl = baseUrl;
      } else if (provider != null) {
        resolvedBaseUrl = provider.getBaseUrl();
      } else {
        throw new IllegalStateException("Either provider or baseUrl must be set");
      }

      String resolvedApiKey = apiKey;
      if (resolvedApiKey == null && provider != null) {
        resolvedApiKey = System.getenv(provider.getEnvKey());
      }
      if (resolvedApiKey == null) {
        throw new IllegalStateException(
                "API key must be set either explicitly or via environment variable");
      }

      // Auto-create model registry for OpenRouter (used internally for telemetry cost calculation)
      OpenRouterModelRegistry resolvedModelRegistry = null;
      if (provider == ResponsesAPIProvider.OPEN_ROUTER) {
        try {
          resolvedModelRegistry =
                  OpenRouterModelRegistry.builder()
                          .apiKey(resolvedApiKey)
                          .httpClient(httpClient)
                          .objectMapper(objectMapper)
                          .build();
        } catch (Exception e) {
          // Silently ignore if registry creation fails - cost calculation is optional
        }
      }

      return new Responder(
              provider,
              resolvedBaseUrl,
              jsonSchemaProducer,
              httpClient,
              resolvedApiKey,
              objectMapper,
              ProcessorRegistry.of(telemetryProcessors),
              resolvedModelRegistry,
              retryPolicy);
    }
  }
}
