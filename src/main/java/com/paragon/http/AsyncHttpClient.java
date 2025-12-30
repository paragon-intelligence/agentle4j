package com.paragon.http;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paragon.http.RetryPolicy;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Async HTTP client with JSON support and SSE streaming.
 *
 * <pre>{@code
 * var client = HttpClient.builder()
 *     .baseUrl("https://api.example.com")
 *     .defaultHeader("Authorization", "Bearer token")
 *     .build();
 *
 * // Simple request
 * var user = client.execute(HttpRequest.get("/users/1").build(), User.class).join();
 *
 * // Streaming
 * client.stream(
 *     HttpRequest.post("/chat").jsonBody(req).build(),
 *     Chunk.class,
 *     chunk -> System.out.print(chunk.text()),
 *     err -> err.printStackTrace(),
 *     () -> System.out.println("Done")
 * );
 * }</pre>
 */
public final class AsyncHttpClient implements AutoCloseable {

  private static final MediaType JSON = MediaType.parse("application/json");

  private final OkHttpClient okHttp;
  private final ObjectMapper mapper;
  private final String baseUrl;
  private final Map<String, String> defaultHeaders;
  private final RetryPolicy retryPolicy;
  private final HttpEventListener listener;
  private final ScheduledExecutorService scheduler;

  private AsyncHttpClient(Builder builder) {
    this.okHttp = builder.okHttp != null ? builder.okHttp : buildOkHttp(builder);
    this.mapper = builder.mapper != null ? builder.mapper : buildMapper();
    this.baseUrl = builder.baseUrl;
    this.defaultHeaders = Map.copyOf(builder.defaultHeaders);
    this.retryPolicy = builder.retryPolicy;
    this.listener = builder.listener;
    this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      var t = new Thread(r, "http-retry");
      t.setDaemon(true);
      return t;
    });
  }

  private static OkHttpClient buildOkHttp(Builder b) {
    return new OkHttpClient.Builder()
            .connectTimeout(b.connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(b.readTimeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(b.writeTimeoutMs, TimeUnit.MILLISECONDS)
            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
            .followRedirects(true)
            .build();
  }

  private static ObjectMapper buildMapper() {
    return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  }

  // ==================== Public API ====================

  public static Builder builder() {
    return new Builder();
  }

  public static AsyncHttpClient create() {
    return builder().build();
  }

  /**
   * Executes request and deserializes response.
   */
  public <T> CompletableFuture<T> execute(HttpRequest request, Class<T> responseType) {
    return execute(request).thenApply(response -> {
      try {
        if (response.body() == null) {
          throw new IllegalStateException("Response has no body");
        }
        return mapper.readValue(response.body(), responseType);
      } catch (IOException e) {
        throw HttpException.deserializationFailed(response.statusCode(), response.bodyAsString(), e);
      }
    });
  }

  /**
   * Executes request and returns raw response.
   */
  public CompletableFuture<HttpResponse> execute(HttpRequest request) {
    return executeWithRetry(request, 1);
  }

  /**
   * Executes streaming request (SSE).
   */
  public <T> CompletableFuture<Void> stream(
          HttpRequest request,
          Class<T> eventType,
          Consumer<T> onEvent,
          Consumer<Throwable> onError,
          Runnable onComplete
  ) {
    return stream(request, StreamConsumer.of(eventType, onEvent, onError, onComplete));
  }

  // ==================== Internal ====================

  /**
   * Executes streaming request with fine-grained control.
   */
  public <T> CompletableFuture<Void> stream(HttpRequest request, StreamConsumer<T> consumer) {
    var future = new CompletableFuture<Void>();
    var okRequest = buildRequest(request);

    listener.onRequestStart(request);
    listener.onStreamStart(request);

    okHttp.newCall(okRequest).enqueue(new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        var ex = HttpException.networkFailure("Stream failed", e);
        listener.onStreamEnd(request, 0, ex);
        consumer.onError(ex);
        future.completeExceptionally(ex);
      }

      @Override
      public void onResponse(Call call, Response response) {
        if (!response.isSuccessful()) {
          handleStreamError(request, response, consumer, future);
          return;
        }
        processStream(request, response, consumer, future);
      }
    });

    return future;
  }

  @Override
  public void close() {
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
    okHttp.dispatcher().executorService().shutdown();
    okHttp.connectionPool().evictAll();
  }

  private CompletableFuture<HttpResponse> executeWithRetry(HttpRequest request, int attempt) {
    var future = new CompletableFuture<HttpResponse>();
    var startTime = System.currentTimeMillis();
    var okRequest = buildRequest(request);

    listener.onRequestStart(request);

    okHttp.newCall(okRequest).enqueue(new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        handleFailure(request, HttpException.networkFailure(e.getMessage(), e), attempt, future);
      }

      @Override
      public void onResponse(Call call, Response response) {
        try (response) {
          var httpResponse = toHttpResponse(response, startTime);

          if (httpResponse.isSuccessful()) {
            listener.onResponse(request, httpResponse);
            future.complete(httpResponse);
          } else {
            var ex = HttpException.fromResponse(
                    httpResponse.statusCode(),
                    httpResponse.bodyAsString(),
                    request.method(),
                    request.url()
            );
            handleFailure(request, ex, attempt, future);
          }
        } catch (Exception e) {
          handleFailure(request, HttpException.networkFailure(e.getMessage(), e), attempt, future);
        }
      }
    });

    return future;
  }

  private void handleFailure(HttpRequest request, HttpException error, int attempt, CompletableFuture<HttpResponse> future) {
    if (retryPolicy.shouldRetry(error, attempt)) {
      var delay = retryPolicy.delayForAttempt(attempt);
      listener.onRetry(request, attempt + 1, delay, error);

      scheduler.schedule(
              () -> executeWithRetry(request, attempt + 1)
                      .whenComplete((res, ex) -> {
                        if (ex != null) future.completeExceptionally(ex);
                        else future.complete(res);
                      }),
              delay,
              TimeUnit.MILLISECONDS
      );
    } else {
      listener.onError(request, error);
      future.completeExceptionally(error);
    }
  }

  private <T> void handleStreamError(HttpRequest request, Response response, StreamConsumer<T> consumer, CompletableFuture<Void> future) {
    String body = null;
    try (response) {
      var responseBody = response.body();
      if (responseBody != null) body = responseBody.string();
    } catch (IOException ignored) {
    }

    var ex = HttpException.fromResponse(response.code(), body, request.method(), request.url());
    listener.onStreamEnd(request, 0, ex);
    consumer.onError(ex);
    future.completeExceptionally(ex);
  }

  private <T> void processStream(HttpRequest request, Response response, StreamConsumer<T> consumer, CompletableFuture<Void> future) {
    var eventCount = 0;
    var dataBuffer = new StringBuilder();

    try (response; var reader = new BufferedReader(new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (consumer.shouldCancel() || future.isCancelled()) break;

        consumer.onRawLine(line);

        if (line.isEmpty()) {
          if (!dataBuffer.isEmpty()) {
            var data = dataBuffer.toString().trim();
            dataBuffer.setLength(0);

            if (!data.isEmpty() && !data.equals("[DONE]")) {
              try {
                var event = mapper.readValue(data, consumer.eventType());
                listener.onStreamEvent(request, data);
                consumer.onEvent(event);
                eventCount++;
              } catch (Exception ignored) {
              }
            }
          }
        } else if (line.startsWith("data:")) {
          if (!dataBuffer.isEmpty()) dataBuffer.append("\n");
          dataBuffer.append(line.substring(5).trim());
        }
      }

      listener.onStreamEnd(request, eventCount, null);
      consumer.onComplete();
      future.complete(null);

    } catch (IOException e) {
      listener.onStreamEnd(request, eventCount, e);
      consumer.onError(e);
      future.completeExceptionally(e);
    }
  }

  // ==================== Builder ====================

  private Request buildRequest(HttpRequest request) {
    var url = new StringBuilder();
    var requestUrl = request.url();

    if (baseUrl != null && !requestUrl.startsWith("http://") && !requestUrl.startsWith("https://")) {
      url.append(baseUrl);
      if (!requestUrl.startsWith("/")) url.append("/");
    }
    url.append(requestUrl);

    var builder = new Request.Builder().url(url.toString());

    defaultHeaders.forEach(builder::addHeader);
    request.headers().forEach((name, values) -> values.forEach(v -> builder.addHeader(name, v)));

    RequestBody body = null;
    if (request.body() != null) {
      var contentType = request.contentType() != null
              ? MediaType.parse(request.contentType())
              : JSON;
      body = RequestBody.create(request.body(), contentType);
    }

    return builder.method(request.method(), body).build();
  }

  private HttpResponse toHttpResponse(Response response, long startTime) throws IOException {
    var headers = new LinkedHashMap<String, List<String>>();
    for (var name : response.headers().names()) {
      headers.put(name.toLowerCase(), response.headers().values(name));
    }

    byte[] body = null;
    if (response.body() != null) {
      body = response.body().bytes();
    }

    return HttpResponse.of(
            response.code(),
            response.message(),
            headers,
            body,
            System.currentTimeMillis() - startTime
    );
  }

  public static final class Builder {
    private final Map<String, String> defaultHeaders = new LinkedHashMap<>();
    private String baseUrl;
    private long connectTimeoutMs = 10_000;
    private long readTimeoutMs = 30_000;
    private long writeTimeoutMs = 30_000;
    private RetryPolicy retryPolicy = RetryPolicy.defaults();
    private HttpEventListener listener = HttpEventListener.noop();
    private OkHttpClient okHttp;
    private ObjectMapper mapper;

    private Builder() {
    }

    public Builder baseUrl(String baseUrl) {
      this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
      return this;
    }

    public Builder connectTimeout(long ms) {
      this.connectTimeoutMs = ms;
      return this;
    }

    public Builder readTimeout(long ms) {
      this.readTimeoutMs = ms;
      return this;
    }

    public Builder writeTimeout(long ms) {
      this.writeTimeoutMs = ms;
      return this;
    }

    public Builder defaultHeader(String name, String value) {
      this.defaultHeaders.put(name, value);
      return this;
    }

    public Builder retryPolicy(RetryPolicy policy) {
      this.retryPolicy = policy;
      return this;
    }

    public Builder noRetry() {
      this.retryPolicy = RetryPolicy.none();
      return this;
    }

    public Builder eventListener(HttpEventListener listener) {
      this.listener = listener;
      return this;
    }

    public Builder okHttpClient(OkHttpClient client) {
      this.okHttp = client;
      return this;
    }

    public Builder objectMapper(ObjectMapper mapper) {
      this.mapper = mapper;
      return this;
    }

    public AsyncHttpClient build() {
      return new AsyncHttpClient(this);
    }
  }
}