package com.paragon.prompts;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.http.RetryPolicy;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A {@link PromptProvider} that retrieves prompts from the Langfuse API.
 *
 * <p>This provider fetches prompts from Langfuse's prompt management service, supporting
 * versioned prompts, labels, and both text and chat prompt types. It includes automatic
 * retry with exponential backoff for transient failures.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create provider with builder
 * PromptProvider provider = LangfusePromptProvider.builder()
 *     .httpClient(new OkHttpClient())
 *     .publicKey("pk-lf-xxx")
 *     .secretKey("sk-lf-xxx")
 *     .build();
 *
 * // Retrieve a prompt
 * Prompt prompt = provider.providePrompt("my-prompt-name");
 *
 * // Retrieve a specific version
 * Prompt prompt = provider.providePrompt("my-prompt", Map.of("version", "2"));
 *
 * // Retrieve by label
 * Prompt prompt = provider.providePrompt("my-prompt", Map.of("label", "staging"));
 * }</pre>
 *
 * <h2>Supported Filters</h2>
 * <ul>
 *   <li>{@code version} - Retrieve a specific prompt version (integer)</li>
 *   <li>{@code label} - Retrieve prompt by label (e.g., "production", "staging")</li>
 * </ul>
 *
 * @author Agentle Framework
 * @since 1.0
 */
public final class LangfusePromptProvider implements PromptProvider {

  /** Default Langfuse cloud API base URL. */
  public static final String DEFAULT_BASE_URL = "https://cloud.langfuse.com";

  private final OkHttpClient httpClient;
  private final String baseUrl;
  private final String authHeader;
  private final RetryPolicy retryPolicy;
  private final ObjectMapper objectMapper;

  private LangfusePromptProvider(Builder builder) {
    this.httpClient = Objects.requireNonNull(builder.httpClient, "httpClient must not be null");
    this.baseUrl = builder.baseUrl != null ? builder.baseUrl : DEFAULT_BASE_URL;
    
    Objects.requireNonNull(builder.publicKey, "publicKey must not be null");
    Objects.requireNonNull(builder.secretKey, "secretKey must not be null");
    
    String credentials = builder.publicKey + ":" + builder.secretKey;
    this.authHeader = "Basic " + Base64.getEncoder().encodeToString(
        credentials.getBytes(StandardCharsets.UTF_8));
    
    this.retryPolicy = builder.retryPolicy != null 
        ? builder.retryPolicy 
        : RetryPolicy.defaults();
    
    this.objectMapper = builder.objectMapper != null 
        ? builder.objectMapper 
        : createDefaultObjectMapper();
  }

  private static ObjectMapper createDefaultObjectMapper() {
    return new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  /**
   * Creates a new builder for LangfusePromptProvider.
   *
   * @return a new builder instance
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  @Override
  public @NonNull Prompt providePrompt(@NonNull String promptId, @Nullable Map<String, String> filters) {
    Objects.requireNonNull(promptId, "promptId must not be null");

    if (promptId.isEmpty()) {
      throw new PromptProviderException("Prompt ID cannot be empty", promptId);
    }

    try {
      LangfusePromptResponse response = fetchPromptWithRetry(promptId, filters, 0);
      return Prompt.of(response.getPromptContent());
    } catch (PromptProviderException e) {
      throw e;
    } catch (Exception e) {
      throw new PromptProviderException(
          "Failed to fetch prompt from Langfuse: " + e.getMessage(), 
          promptId, 
          e, 
          isRetryable(e));
    }
  }

  private LangfusePromptResponse fetchPromptWithRetry(
      String promptId, 
      @Nullable Map<String, String> filters, 
      int attempt) {
    
    try {
      return fetchPrompt(promptId, filters).join();
    } catch (CompletionException e) {
      Throwable cause = e.getCause();
      
      if (cause instanceof PromptProviderException ppe) {
        if (ppe.isRetryable() && attempt < retryPolicy.maxRetries()) {
          sleepForRetry(attempt + 1);
          return fetchPromptWithRetry(promptId, filters, attempt + 1);
        }
        throw ppe;
      }
      
      // Check if we should retry based on the error
      if (isRetryable(cause) && attempt < retryPolicy.maxRetries()) {
        sleepForRetry(attempt + 1);
        return fetchPromptWithRetry(promptId, filters, attempt + 1);
      }
      
      throw new PromptProviderException(
          "Failed to fetch prompt: " + cause.getMessage(),
          promptId,
          cause,
          false);
    }
  }

  private void sleepForRetry(int attempt) {
    try {
      long delayMs = retryPolicy.delayForAttempt(attempt);
      Thread.sleep(delayMs);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new PromptProviderException("Interrupted while waiting for retry", null, ie);
    }
  }

  private boolean isRetryable(Throwable e) {
    if (e instanceof IOException) {
      return true; // Network errors are retryable
    }
    if (e instanceof PromptProviderException ppe) {
      return ppe.isRetryable();
    }
    return false;
  }

  private CompletableFuture<LangfusePromptResponse> fetchPrompt(
      String promptId, 
      @Nullable Map<String, String> filters) {
    
    CompletableFuture<LangfusePromptResponse> future = new CompletableFuture<>();

    String encodedPromptId = URLEncoder.encode(promptId, StandardCharsets.UTF_8);
    HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + "/api/public/v2/prompts/" + encodedPromptId)
        .newBuilder();

    // Add filters as query parameters
    if (filters != null) {
      String version = filters.get("version");
      if (version != null && !version.isEmpty()) {
        urlBuilder.addQueryParameter("version", version);
      }
      
      String label = filters.get("label");
      if (label != null && !label.isEmpty()) {
        urlBuilder.addQueryParameter("label", label);
      }
    }

    Request request = new Request.Builder()
        .url(urlBuilder.build())
        .header("Authorization", authHeader)
        .header("Accept", "application/json")
        .get()
        .build();

    httpClient.newCall(request).enqueue(new Callback() {
      @Override
      public void onFailure(@NonNull Call call, @NonNull IOException e) {
        future.completeExceptionally(
            new PromptProviderException(
                "Network error fetching prompt: " + e.getMessage(),
                promptId,
                e,
                true));
      }

      @Override
      public void onResponse(@NonNull Call call, @NonNull Response response) {
        try (ResponseBody body = response.body()) {
          int code = response.code();
          
          if (!response.isSuccessful()) {
            String errorBody = body != null ? body.string() : "";
            boolean retryable = retryPolicy.isRetryable(code);
            
            String message = switch (code) {
              case 401 -> "Unauthorized: Invalid Langfuse credentials";
              case 403 -> "Forbidden: Access denied to prompt '" + promptId + "'";
              case 404 -> "Prompt not found: '" + promptId + "'";
              case 429 -> "Rate limited by Langfuse API";
              default -> "HTTP " + code + ": " + errorBody;
            };
            
            future.completeExceptionally(
                new PromptProviderException(message, promptId, null, retryable));
            return;
          }

          if (body == null) {
            future.completeExceptionally(
                new PromptProviderException("Empty response body", promptId));
            return;
          }

          String json = body.string();
          LangfusePromptResponse promptResponse = objectMapper.readValue(
              json, LangfusePromptResponse.class);
          
          future.complete(promptResponse);
        } catch (IOException e) {
          future.completeExceptionally(
              new PromptProviderException(
                  "Failed to parse response: " + e.getMessage(), promptId, e));
        }
      }
    });

    return future;
  }

  /**
   * Returns the base URL for the Langfuse API.
   *
   * @return the base URL
   */
  public @NonNull String baseUrl() {
    return baseUrl;
  }

  /**
   * Returns the retry policy used by this provider.
   *
   * @return the retry policy
   */
  public @NonNull RetryPolicy retryPolicy() {
    return retryPolicy;
  }

  /**
   * Builder for creating {@link LangfusePromptProvider} instances.
   */
  public static final class Builder {
    private OkHttpClient httpClient;
    private String publicKey;
    private String secretKey;
    private String baseUrl;
    private RetryPolicy retryPolicy;
    private ObjectMapper objectMapper;

    private Builder() {}

    /**
     * Sets the HTTP client to use for API requests.
     *
     * @param httpClient the OkHttp client
     * @return this builder
     */
    public @NonNull Builder httpClient(@NonNull OkHttpClient httpClient) {
      this.httpClient = Objects.requireNonNull(httpClient);
      return this;
    }

    /**
     * Sets the Langfuse public key.
     *
     * @param publicKey the public API key
     * @return this builder
     */
    public @NonNull Builder publicKey(@NonNull String publicKey) {
      this.publicKey = Objects.requireNonNull(publicKey);
      return this;
    }

    /**
     * Sets the Langfuse secret key.
     *
     * @param secretKey the secret API key
     * @return this builder
     */
    public @NonNull Builder secretKey(@NonNull String secretKey) {
      this.secretKey = Objects.requireNonNull(secretKey);
      return this;
    }

    /**
     * Sets the Langfuse API base URL.
     *
     * <p>Defaults to {@value LangfusePromptProvider#DEFAULT_BASE_URL}.
     *
     * @param baseUrl the base URL
     * @return this builder
     */
    public @NonNull Builder baseUrl(@NonNull String baseUrl) {
      this.baseUrl = Objects.requireNonNull(baseUrl);
      return this;
    }

    /**
     * Sets the retry policy for handling transient failures.
     *
     * <p>Defaults to {@link RetryPolicy#defaults()}.
     *
     * @param retryPolicy the retry policy
     * @return this builder
     */
    public @NonNull Builder retryPolicy(@NonNull RetryPolicy retryPolicy) {
      this.retryPolicy = Objects.requireNonNull(retryPolicy);
      return this;
    }

    /**
     * Sets a custom ObjectMapper for JSON deserialization.
     *
     * @param objectMapper the ObjectMapper
     * @return this builder
     */
    public @NonNull Builder objectMapper(@NonNull ObjectMapper objectMapper) {
      this.objectMapper = Objects.requireNonNull(objectMapper);
      return this;
    }

    /**
     * Creates a provider using environment variables.
     *
     * <p>Reads {@code LANGFUSE_PUBLIC_KEY}, {@code LANGFUSE_SECRET_KEY}, 
     * and optionally {@code LANGFUSE_HOST}.
     *
     * @return this builder with environment configuration
     */
    public @NonNull Builder fromEnv() {
      String publicKey = System.getenv("LANGFUSE_PUBLIC_KEY");
      String secretKey = System.getenv("LANGFUSE_SECRET_KEY");
      String host = System.getenv("LANGFUSE_HOST");

      if (publicKey != null && !publicKey.isEmpty()) {
        this.publicKey = publicKey;
      }
      if (secretKey != null && !secretKey.isEmpty()) {
        this.secretKey = secretKey;
      }
      if (host != null && !host.isEmpty()) {
        this.baseUrl = host;
      }
      return this;
    }

    /**
     * Builds the LangfusePromptProvider.
     *
     * @return a new {@link LangfusePromptProvider}
     * @throws NullPointerException if httpClient, publicKey, or secretKey is not set
     */
    public @NonNull LangfusePromptProvider build() {
      return new LangfusePromptProvider(this);
    }
  }
}
