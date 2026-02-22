package com.paragon.embeddings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.http.RetryPolicy;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import okhttp3.*;
import org.jspecify.annotations.NonNull;

/**
 * Embedding provider for OpenRouter's embedding API with built-in retry support.
 *
 * <p>Automatically retries on:
 *
 * <ul>
 *   <li>429 Too Many Requests - Rate limit exceeded
 *   <li>529 Provider Overloaded - Temporary overload, uses fallback providers when enabled
 *   <li>5xx Server Errors - Transient server issues
 * </ul>
 *
 * <p><b>Virtual Thread Design:</b> Uses synchronous API optimized for Java 21+ virtual threads.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * EmbeddingProvider embeddings = OpenRouterEmbeddingProvider.builder()
 *     .apiKey(System.getenv("OPENROUTER_API_KEY"))
 *     .retryPolicy(RetryPolicy.defaults())  // Retry on 429, 529, 5xx
 *     .allowFallbacks(true)                 // Use backup providers on 529
 *     .build();
 *
 * List<Embedding> results = embeddings.createEmbeddings(
 *     List.of("Hello world", "AI is amazing"),
 *     "openai/text-embedding-3-small"
 * );
 * }</pre>
 */
public class OpenRouterEmbeddingProvider implements EmbeddingProvider {
  private static final String BASE_URL = "https://openrouter.ai/api/v1";
  private static final String EMBEDDINGS_PATH = "/embeddings";
  private static final MediaType JSON = MediaType.get("application/json");

  private final @NonNull OkHttpClient httpClient;
  private final @NonNull ObjectMapper objectMapper;
  private final @NonNull String apiKey;
  private final @NonNull RetryPolicy retryPolicy;
  private final boolean allowFallbacks;

  private OpenRouterEmbeddingProvider(Builder builder) {
    this.objectMapper = builder.objectMapper != null ? builder.objectMapper : new ObjectMapper();
    this.apiKey = builder.apiKey;
    this.retryPolicy = builder.retryPolicy;
    this.allowFallbacks = builder.allowFallbacks;
    this.httpClient = builder.httpClient != null ? builder.httpClient : new OkHttpClient();
  }

  /**
   * Creates a new builder for OpenRouterEmbeddingProvider.
   *
   * @return a new builder instance
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  @Override
  public @NonNull List<Embedding> createEmbeddings(
      @NonNull List<String> input, @NonNull String model) {
    Objects.requireNonNull(input, "input cannot be null");
    Objects.requireNonNull(model, "model cannot be null");

    try {
      String jsonBody =
          objectMapper.writeValueAsString(
              Map.of("input", input, "model", model, "allow_fallbacks", allowFallbacks));

      Request request =
          new Request.Builder()
              .url(BASE_URL + EMBEDDINGS_PATH)
              .post(RequestBody.create(jsonBody, JSON))
              .addHeader("Authorization", "Bearer " + apiKey)
              .addHeader("Content-Type", "application/json")
              .build();

      return executeWithRetry(request, 0);
    } catch (IOException e) {
      throw new RuntimeException("Failed to create embeddings", e);
    }
  }

  private List<Embedding> executeWithRetry(Request request, int attempt) throws IOException {
    try {
      Response response = httpClient.newCall(request).execute();

      try (ResponseBody body = response.body()) {
        String json = body != null ? body.string() : "";

        if (!response.isSuccessful()) {
          // Check if we should retry this status code
          if (retryPolicy.isRetryable(response.code()) && attempt < retryPolicy.maxRetries()) {
            sleepForRetry(attempt);
            return executeWithRetry(request, attempt + 1);
          }

          throw new RuntimeException(
              String.format(
                  "Embedding API Error: %d - %s%nResponse: %s",
                  response.code(), response.message(), json));
        }

        EmbeddingResponse embeddingResponse = objectMapper.readValue(json, EmbeddingResponse.class);
        return embeddingResponse.data();
      }
    } catch (IOException e) {
      // Network errors are retryable
      if (attempt < retryPolicy.maxRetries()) {
        sleepForRetry(attempt);
        return executeWithRetry(request, attempt + 1);
      }
      throw e;
    }
  }

  private void sleepForRetry(int attempt) {
    Duration delay = retryPolicy.getDelayForAttempt(attempt + 1);
    try {
      Thread.sleep(delay.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Retry interrupted", e);
    }
  }

  /** Closes the underlying HTTP client and releases resources. */
  public void close() {
    // OkHttpClient doesn't need explicit close, but keep method for API compatibility
  }

  /**
   * Response structure from OpenRouter embeddings API.
   *
   * @param data the list of embeddings
   * @param model the model used
   * @param usage token usage information
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  private record EmbeddingResponse(
      @NonNull List<Embedding> data, String model, EmbeddingUsage usage) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record EmbeddingUsage(int prompt_tokens, int total_tokens) {}

  /** Builder for constructing OpenRouterEmbeddingProvider instances. */
  public static final class Builder {
    private String apiKey;
    private ObjectMapper objectMapper;
    private OkHttpClient httpClient;
    private RetryPolicy retryPolicy = RetryPolicy.defaults();
    private boolean allowFallbacks = true;

    private Builder() {}

    /**
     * Sets the OpenRouter API key (required).
     *
     * <p>Can also be loaded from the environment variable {@code OPENROUTER_API_KEY} if not
     * provided explicitly.
     *
     * @param apiKey the API key
     * @return this builder
     */
    public @NonNull Builder apiKey(@NonNull String apiKey) {
      this.apiKey = Objects.requireNonNull(apiKey, "apiKey cannot be null");
      return this;
    }

    /**
     * Sets the ObjectMapper for JSON serialization/deserialization.
     *
     * @param objectMapper the object mapper
     * @return this builder
     */
    public @NonNull Builder objectMapper(@NonNull ObjectMapper objectMapper) {
      this.objectMapper = Objects.requireNonNull(objectMapper);
      return this;
    }

    /**
     * Sets the OkHttpClient for HTTP requests.
     *
     * @param httpClient the HTTP client
     * @return this builder
     */
    public @NonNull Builder httpClient(@NonNull OkHttpClient httpClient) {
      this.httpClient = Objects.requireNonNull(httpClient);
      return this;
    }

    /**
     * Sets the retry policy for handling transient failures.
     *
     * <p>Default: {@link RetryPolicy#defaults()} which retries 3 times on 429, 529, and 5xx errors.
     *
     * @param retryPolicy the retry policy
     * @return this builder
     */
    public @NonNull Builder retryPolicy(@NonNull RetryPolicy retryPolicy) {
      this.retryPolicy = Objects.requireNonNull(retryPolicy);
      return this;
    }

    /**
     * Configures whether to allow fallback providers when the primary is overloaded (529).
     *
     * <p>When enabled, OpenRouter will automatically route to backup embedding providers if the
     * primary provider is overloaded. Default: true.
     *
     * @param allowFallbacks true to enable fallbacks
     * @return this builder
     */
    public @NonNull Builder allowFallbacks(boolean allowFallbacks) {
      this.allowFallbacks = allowFallbacks;
      return this;
    }

    /**
     * Builds the OpenRouterEmbeddingProvider.
     *
     * @return a new OpenRouterEmbeddingProvider instance
     * @throws NullPointerException if required fields are missing
     */
    public @NonNull OpenRouterEmbeddingProvider build() {
      if (apiKey == null) {
        apiKey = System.getenv("OPENROUTER_API_KEY");
      }
      if (apiKey == null) {
        throw new IllegalStateException(
            "API key must be set either explicitly or via OPENROUTER_API_KEY environment variable");
      }
      return new OpenRouterEmbeddingProvider(this);
    }
  }
}
