package com.paragon.responses.streaming;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.spec.FunctionToolCallOutput;
import com.paragon.responses.spec.FunctionToolStore;
import com.paragon.responses.spec.ParsedResponse;
import com.paragon.responses.spec.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import okhttp3.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A streaming response wrapper for OpenAI Responses API Server-Sent Events (SSE).
 *
 * <p>Provides a fluent, callback-based API for processing streaming events. Uses Java 21+ virtual
 * threads for non-blocking async processing.
 *
 * <h2>Usage Examples:</h2>
 *
 * <pre>{@code
 * // Simple text streaming
 * responder.respondStream(payload)
 *     .onTextDelta(System.out::print)
 *     .onComplete(response -> System.out.println("\nDone!"))
 *     .onError(Throwable::printStackTrace)
 *     .start();
 *
 * // Wait for completion (blocking)
 * Response response = responder.respondStream(payload).get();
 *
 * // Collect all text (blocking)
 * String text = responder.respondStream(payload).getText();
 *
 * // Structured output streaming (blocking)
 * ParsedResponse<MyClass> parsed = responder.respondStream(structuredPayload)
 *     .getParsed();
 * }</pre>
 *
 * @param <T> the type for structured output parsing, or {@code Void} for regular streaming
 */
public final class ResponseStream<T> {
  private static final Logger logger = LoggerFactory.getLogger(ResponseStream.class);
  private static final String SSE_DATA_PREFIX = "data: ";
  private static final String SSE_DONE_SIGNAL = "[DONE]";

  private final @NonNull OkHttpClient httpClient;
  private final @NonNull Request request;
  private final @NonNull ObjectMapper objectMapper;
  private final @Nullable Class<T> responseType;

  // Callbacks
  private @Nullable Consumer<String> onTextDeltaHandler;
  private @Nullable Consumer<Response> onCompleteHandler;
  private @Nullable Consumer<ParsedResponse<T>> onParsedCompleteHandler;
  private @Nullable Consumer<T> onPartialParsedHandler;
  private @Nullable Consumer<Map<String, Object>> onPartialJsonHandler;
  private @Nullable Consumer<Throwable> onErrorHandler;
  private @Nullable Consumer<StreamingEvent> onEventHandler;
  private @Nullable BiConsumer<String, String> onToolCallHandler;
  private @Nullable BiConsumer<String, FunctionToolCallOutput> onToolResultHandler;

  // Tool execution
  private @Nullable FunctionToolStore toolStore;

  // State for partial parsing
  private final StringBuilder accumulatedText = new StringBuilder();
  private @Nullable PartialJsonParser<T> partialParser;

  private final AtomicBoolean started = new AtomicBoolean(false);
  private final AtomicBoolean cancelled = new AtomicBoolean(false);
  private final AtomicReference<Call> currentCall = new AtomicReference<>();

  /**
   * Creates a new ResponseStream.
   *
   * @param httpClient the OkHttp client
   * @param request the prepared HTTP request
   * @param objectMapper the Jackson ObjectMapper for event deserialization
   * @param responseType the structured output type, or null for regular streaming
   */
  public ResponseStream(
      @NonNull OkHttpClient httpClient,
      @NonNull Request request,
      @NonNull ObjectMapper objectMapper,
      @Nullable Class<T> responseType) {
    this.httpClient = Objects.requireNonNull(httpClient);
    this.request = Objects.requireNonNull(request);
    this.objectMapper = Objects.requireNonNull(objectMapper);
    this.responseType = responseType;
  }

  /**
   * Registers a handler for text delta events. Called for each chunk of text as it streams.
   *
   * @param handler Consumer that receives text deltas
   * @return this ResponseStream for chaining
   */
  public @NonNull ResponseStream<T> onTextDelta(@NonNull Consumer<String> handler) {
    this.onTextDeltaHandler = Objects.requireNonNull(handler);
    return this;
  }

  /**
   * Registers a handler for stream completion. Called once when the response is complete.
   *
   * @param handler Consumer that receives the final Response
   * @return this ResponseStream for chaining
   */
  public @NonNull ResponseStream<T> onComplete(@NonNull Consumer<Response> handler) {
    this.onCompleteHandler = Objects.requireNonNull(handler);
    return this;
  }

  /**
   * Registers a handler for errors. Called if an error occurs during streaming.
   *
   * @param handler Consumer that receives the error
   * @return this ResponseStream for chaining
   */
  public @NonNull ResponseStream<T> onError(@NonNull Consumer<Throwable> handler) {
    this.onErrorHandler = Objects.requireNonNull(handler);
    return this;
  }

  /**
   * Registers a handler for all streaming events. Called for every event received.
   *
   * @param handler Consumer that receives each StreamingEvent
   * @return this ResponseStream for chaining
   */
  public @NonNull ResponseStream<T> onEvent(@NonNull Consumer<StreamingEvent> handler) {
    this.onEventHandler = Objects.requireNonNull(handler);
    return this;
  }

  /**
   * Registers a handler for typed completion (structured output only). Called once when the
   * response is complete, with parsed result.
   *
   * <p>This is the recommended callback for structured streaming as it provides direct access to
   * the parsed object.
   *
   * <p>Example:
   *
   * <pre>{@code
   * responder.respond(structuredPayload)
   *     .onTextDelta(System.out::print)
   *     .onParsedComplete(parsed -> {
   *         Person person = parsed.parsed();
   *         System.out.println("Name: " + person.name());
   *     })
   *     .start();
   * }</pre>
   *
   * @param handler Consumer that receives the ParsedResponse with typed content
   * @return this ResponseStream for chaining
   * @throws IllegalStateException if this is not a structured output stream
   */
  public @NonNull ResponseStream<T> onParsedComplete(@NonNull Consumer<ParsedResponse<T>> handler) {
    if (responseType == null) {
      throw new IllegalStateException(
          "onParsedComplete is only available for structured output streams");
    }
    this.onParsedCompleteHandler = Objects.requireNonNull(handler);
    return this;
  }

  /**
   * Registers a handler for partial parsed updates during streaming. Called on each text delta with
   * a partially-filled instance.
   *
   * <p>This enables real-time UI updates as JSON fields are populated. The target class should have
   * all fields as {@code @Nullable} to accept partially-filled objects.
   *
   * <p>Example:
   *
   * <pre>{@code
   * record PartialPerson(@Nullable String name, @Nullable Integer age) {}
   *
   * responder.respond(structuredPayload)
   *     .onPartialParsed(PartialPerson.class, partial -> {
   *         if (partial.name() != null) {
   *             updateNameField(partial.name());
   *         }
   *     })
   *     .start();
   * }</pre>
   *
   * @param partialType the nullable wrapper class for partial parsing
   * @param handler Consumer that receives partially-filled instances
   * @param <P> the partial type (should have all nullable fields)
   * @return this ResponseStream for chaining
   * @throws IllegalStateException if this is not a structured output stream
   */
  @SuppressWarnings("unchecked")
  public <P> @NonNull ResponseStream<T> onPartialParsed(
      @NonNull Class<P> partialType, @NonNull Consumer<P> handler) {
    if (responseType == null) {
      throw new IllegalStateException(
          "onPartialParsed is only available for structured output streams");
    }
    // Create parser for the partial type
    PartialJsonParser<P> parser = new PartialJsonParser<>(objectMapper, partialType);

    // Store as T consumer (type erasure allows this)
    this.partialParser = (PartialJsonParser<T>) parser;
    this.onPartialParsedHandler = (Consumer<T>) handler;
    return this;
  }

  /**
   * Registers a handler for partial JSON updates during streaming as a Map.
   *
   * <p>This is the <b>zero-class</b> approach to partial parsing - no need to create a separate
   * partial class with nullable fields. Simply access fields as they become available.
   *
   * <p>Example:
   *
   * <pre>{@code
   * responder.respond(structuredPayload)
   *     .onPartialJson(fields -> {
   *         if (fields.containsKey("name")) {
   *             updateNameField(fields.get("name").toString());
   *         }
   *         if (fields.containsKey("age")) {
   *             updateAgeField((Integer) fields.get("age"));
   *         }
   *     })
   *     .start();
   * }</pre>
   *
   * @param handler Consumer that receives partially-parsed JSON as a Map
   * @return this ResponseStream for chaining
   * @throws IllegalStateException if this is not a structured output stream
   */
  @SuppressWarnings("unchecked")
  public @NonNull ResponseStream<T> onPartialJson(@NonNull Consumer<Map<String, Object>> handler) {
    if (responseType == null) {
      throw new IllegalStateException(
          "onPartialJson is only available for structured output streams");
    }
    this.onPartialJsonHandler = Objects.requireNonNull(handler);
    return this;
  }

  /**
   * Registers a handler for tool call detection during streaming. Called when a function tool call
   * is complete with its name and arguments.
   *
   * <p>Example:
   *
   * <pre>{@code
   * responder.respond(payload)
   *     .onToolCall((toolName, argsJson) -> {
   *         System.out.println("Tool called: " + toolName);
   *         System.out.println("Arguments: " + argsJson);
   *     })
   *     .start();
   * }</pre>
   *
   * @param handler BiConsumer that receives (tool name, JSON arguments)
   * @return this ResponseStream for chaining
   */
  public @NonNull ResponseStream<T> onToolCall(@NonNull BiConsumer<String, String> handler) {
    this.onToolCallHandler = Objects.requireNonNull(handler);
    return this;
  }

  /**
   * Registers a FunctionToolStore for automatic tool execution during streaming.
   *
   * <p>When a tool call is detected and a matching tool is found in the store, it will be executed
   * automatically. Use with {@link #onToolResult} to receive execution results.
   *
   * <p>Example:
   *
   * <pre>{@code
   * var toolStore = FunctionToolStore.create()
   *     .add(new GetWeatherTool())
   *     .add(new GetTimeTool());
   *
   * responder.respond(payload)
   *     .withToolStore(toolStore)
   *     .onToolResult((toolName, result) -> {
   *         System.out.println("Tool " + toolName + " returned: " + result.output());
   *     })
   *     .start();
   * }</pre>
   *
   * @param store the FunctionToolStore containing tool implementations
   * @return this ResponseStream for chaining
   */
  public @NonNull ResponseStream<T> withToolStore(@NonNull FunctionToolStore store) {
    this.toolStore = Objects.requireNonNull(store);
    return this;
  }

  /**
   * Registers a handler for tool execution results. Called after a tool is auto-executed via {@link
   * #withToolStore}.
   *
   * @param handler BiConsumer that receives (tool name, tool output)
   * @return this ResponseStream for chaining
   */
  public @NonNull ResponseStream<T> onToolResult(
      @NonNull BiConsumer<String, FunctionToolCallOutput> handler) {
    this.onToolResultHandler = Objects.requireNonNull(handler);
    return this;
  }

  /**
   * Starts streaming on a virtual thread. Non-blocking - returns immediately.
   *
   * @throws IllegalStateException if already started
   */
  public void start() {
    if (!started.compareAndSet(false, true)) {
      throw new IllegalStateException("Stream already started");
    }

    Thread.startVirtualThread(this::executeStream);
  }

  /**
   * Blocks until streaming completes and returns the final Response. Automatically starts streaming
   * if not already started.
   *
   * <p>On virtual threads, blocking is efficient and does not consume platform threads.
   *
   * @return the final Response
   * @throws RuntimeException if streaming fails
   */
  public @NonNull Response get() {
    final AtomicReference<Response> resultRef = new AtomicReference<>();
    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    final Object lock = new Object();
    final AtomicBoolean done = new AtomicBoolean(false);

    Consumer<Response> originalComplete = this.onCompleteHandler;
    Consumer<Throwable> originalError = this.onErrorHandler;

    this.onCompleteHandler =
        response -> {
          if (originalComplete != null) {
            originalComplete.accept(response);
          }
          resultRef.set(response);
          synchronized (lock) {
            done.set(true);
            lock.notifyAll();
          }
        };

    this.onErrorHandler =
        error -> {
          if (originalError != null) {
            originalError.accept(error);
          }
          errorRef.set(error);
          synchronized (lock) {
            done.set(true);
            lock.notifyAll();
          }
        };

    if (started.compareAndSet(false, true)) {
      Thread.startVirtualThread(this::executeStream);
    }

    synchronized (lock) {
      while (!done.get()) {
        try {
          lock.wait();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException("Stream wait interrupted", e);
        }
      }
    }

    if (errorRef.get() != null) {
      throw new RuntimeException("Streaming failed", errorRef.get());
    }

    return resultRef.get();
  }

  /**
   * Blocks until streaming completes and returns a parsed structured response. Only available for
   * structured output streams.
   *
   * @return the ParsedResponse with typed content
   * @throws IllegalStateException if this is not a structured output stream
   * @throws RuntimeException if streaming or parsing fails
   */
  public @NonNull ParsedResponse<T> getParsed() {
    if (responseType == null) {
      throw new IllegalStateException(
          "getParsed() is only available for structured output streams");
    }

    Response response = get();
    try {
      return response.parse(responseType, objectMapper);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to parse structured output", e);
    }
  }

  /**
   * Collects all text deltas and returns the complete text. Blocks until streaming completes.
   *
   * @return the concatenated text
   * @throws RuntimeException if streaming fails
   */
  public @NonNull String getText() {
    StringBuilder textBuilder = new StringBuilder();

    Consumer<String> originalDelta = this.onTextDeltaHandler;
    this.onTextDeltaHandler =
        delta -> {
          if (originalDelta != null) {
            originalDelta.accept(delta);
          }
          textBuilder.append(delta);
        };

    get(); // Wait for completion
    return textBuilder.toString();
  }

  /** Cancels the stream. Safe to call multiple times. */
  public void cancel() {
    cancelled.set(true);
    Call call = currentCall.get();
    if (call != null) {
      call.cancel();
    }
  }

  /**
   * Checks if the stream has been cancelled.
   *
   * @return true if cancelled
   */
  public boolean isCancelled() {
    return cancelled.get();
  }

  private void executeStream() {
    Response finalResponse = null;

    try {
      Call call = httpClient.newCall(request);
      currentCall.set(call);

      if (cancelled.get()) {
        return;
      }

      try (okhttp3.Response httpResponse = call.execute()) {
        if (!httpResponse.isSuccessful()) {
          String errorBody = httpResponse.body() != null ? httpResponse.body().string() : "";
          throw new IOException(
              String.format(
                  "API Error: %s %d - %s%nResponse Body: %s",
                  httpResponse.protocol(), httpResponse.code(), httpResponse.message(), errorBody));
        }

        ResponseBody body = httpResponse.body();
        if (body == null) {
          throw new IOException("Empty response body");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(body.byteStream()))) {
          String line;
          while (!cancelled.get() && (line = reader.readLine()) != null) {
            if (line.isEmpty()) {
              continue; // Skip empty lines between events
            }

            if (line.startsWith(SSE_DATA_PREFIX)) {
              String data = line.substring(SSE_DATA_PREFIX.length()).trim();

              if (SSE_DONE_SIGNAL.equals(data)) {
                break; // Stream complete
              }

              try {
                StreamingEvent event = objectMapper.readValue(data, StreamingEvent.class);

                // Dispatch to handlers
                if (onEventHandler != null) {
                  onEventHandler.accept(event);
                }

                // Handle specific event types
                if (event instanceof OutputTextDeltaEvent deltaEvent) {
                  String delta = deltaEvent.delta();

                  // Call text delta handler
                  if (onTextDeltaHandler != null) {
                    onTextDeltaHandler.accept(delta);
                  }

                  // Accumulate for partial parsing
                  if (onPartialParsedHandler != null && partialParser != null) {
                    accumulatedText.append(delta);
                    T partial = partialParser.parsePartial(accumulatedText.toString());
                    if (partial != null) {
                      onPartialParsedHandler.accept(partial);
                    }
                  }

                  // Map-based partial parsing (zero-class approach)
                  if (onPartialJsonHandler != null) {
                    accumulatedText.append(delta);
                    try {
                      @SuppressWarnings("unchecked")
                      Map<String, Object> partialMap =
                          PartialJsonParser.parseAsMap(objectMapper, accumulatedText.toString());
                      if (partialMap != null && !partialMap.isEmpty()) {
                        onPartialJsonHandler.accept(partialMap);
                      }
                    } catch (Exception ignored) {
                      // Not yet parseable - continue accumulating
                    }
                  }

                } else if (event instanceof FunctionCallArgumentsDoneEvent toolEvent) {
                  // Notify about tool call detection
                  if (onToolCallHandler != null) {
                    onToolCallHandler.accept(toolEvent.name(), toolEvent.arguments());
                  }

                  // Auto-execute if tool store is registered
                  if (toolStore != null && toolStore.contains(toolEvent.name())) {
                    try {
                      // Create a FunctionToolCall from the streaming event
                      var toolCall =
                          new com.paragon.responses.spec.FunctionToolCall(
                              toolEvent.arguments(),
                              toolEvent.itemId(), // Use itemId as callId
                              toolEvent.name(),
                              toolEvent.itemId(), // Use itemId as id
                              null); // Status not available in streaming

                      FunctionToolCallOutput result = toolStore.execute(toolCall);
                      if (onToolResultHandler != null) {
                        onToolResultHandler.accept(toolEvent.name(), result);
                      }
                    } catch (Exception e) {
                      logger.warn(
                          "Failed to execute tool '{}': {}", toolEvent.name(), e.getMessage(), e);
                      if (onErrorHandler != null) {
                        onErrorHandler.accept(e);
                      }
                    }
                  }

                } else if (event instanceof ResponseCompletedEvent completedEvent) {
                  finalResponse = completedEvent.response();
                } else if (event instanceof ResponseFailedEvent failedEvent) {
                  finalResponse = failedEvent.response();
                  throw new RuntimeException(
                      "Response failed: "
                          + (failedEvent.response().error() != null
                              ? failedEvent.response().error().message()
                              : "Unknown error"));
                } else if (event instanceof StreamingErrorEvent errorEvent) {
                  throw new RuntimeException(
                      String.format(
                          "Streaming error [%s]: %s", errorEvent.code(), errorEvent.message()));
                }

              } catch (JsonProcessingException e) {
                logger.warn("Failed to parse SSE event: {}", data, e);
                // Continue processing - some events might be unknown/new
              }
            }
          }
        }
      }

      // Success - call completion handlers
      if (!cancelled.get() && finalResponse != null) {
        // Call original onComplete handler
        if (onCompleteHandler != null) {
          onCompleteHandler.accept(finalResponse);
        }

        // Call typed onParsedComplete handler for structured output
        if (onParsedCompleteHandler != null && responseType != null) {
          try {
            ParsedResponse<T> parsed = finalResponse.parse(responseType, objectMapper);
            onParsedCompleteHandler.accept(parsed);
          } catch (JsonProcessingException e) {
            if (onErrorHandler != null) {
              onErrorHandler.accept(new RuntimeException("Failed to parse structured output", e));
            }
          }
        }
      }

    } catch (Exception e) {
      if (!cancelled.get() && onErrorHandler != null) {
        onErrorHandler.accept(e);
      }
    }
  }
}
