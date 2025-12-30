package com.paragon.embeddings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.http.AsyncHttpClient;
import com.paragon.http.HttpRequest;
import com.paragon.http.RetryPolicy;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
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
 * ).join();
 * }</pre>
 */
public class OpenRouterEmbeddingProvider implements EmbeddingProvider {
  private static final String BASE_URL = "https://openrouter.ai/api/v1";
  private static final String EMBEDDINGS_PATH = "/embeddings";

  private final @NonNull AsyncHttpClient httpClient;
  private final @NonNull ObjectMapper objectMapper;
  private final boolean allowFallbacks;

  private OpenRouterEmbeddingProvider(Builder builder) {
    this.objectMapper =
        builder.objectMapper != null ? builder.objectMapper : new ObjectMapper();

    this.allowFallbacks = builder.allowFallbacks;

    // Build the HTTP client with retry support
    this.httpClient =
        AsyncHttpClient.builder()
            .baseUrl(BASE_URL)
            .defaultHeader("Authorization", "Bearer " + builder.apiKey)
            .defaultHeader("Content-Type", "application/json")
            .retryPolicy(builder.retryPolicy)
            .objectMapper(objectMapper)
            .build();
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
  public @NonNull CompletableFuture<List<Embedding>> createEmbeddings(
      @NonNull List<String> input, @NonNull String model) {
    Objects.requireNonNull(input, "input cannot be null");
    Objects.requireNonNull(model, "model cannot be null");

    var request =
        HttpRequest.post(EMBEDDINGS_PATH)
            .jsonBody(Map.of("input", input, "model", model, "allow_fallbacks", allowFallbacks))
            .build();

    return httpClient.execute(request, EmbeddingResponse.class).thenApply(EmbeddingResponse::data);
  }

  /** Closes the underlying HTTP client and releases resources. */
  public void close() {
    httpClient.close();
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
     * Sets the retry policy for handling transient failures.
     *
     * <p>Default: {@link RetryPolicy#defaults()} which retries 3 times on 429, 529, and 5xx
     * errors.
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
