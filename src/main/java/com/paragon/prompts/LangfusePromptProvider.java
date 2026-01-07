package com.paragon.prompts;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.http.RetryPolicy;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
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
 * <p>This provider fetches prompts from Langfuse's prompt management service, supporting versioned
 * prompts, labels, and both text and chat prompt types. It includes automatic retry with
 * exponential backoff for transient failures.
 *
 * <h2>Usage Example</h2>
 *
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
 *
 * <ul>
 *   <li>{@code version} - Retrieve a specific prompt version (integer)
 *   <li>{@code label} - Retrieve prompt by label (e.g., "production", "staging")
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
    this.authHeader =
        "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

    this.retryPolicy = builder.retryPolicy != null ? builder.retryPolicy : RetryPolicy.defaults();

    this.objectMapper =
        builder.objectMapper != null ? builder.objectMapper : createDefaultObjectMapper();
  }

  private static ObjectMapper createDefaultObjectMapper() {
    return new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
  public @NonNull Prompt providePrompt(
      @NonNull String promptId, @Nullable Map<String, String> filters) {
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
          "Failed to fetch prompt from Langfuse: " + e.getMessage(), promptId, e, isRetryable(e));
    }
  }

  private LangfusePromptResponse fetchPromptWithRetry(
      String promptId, @Nullable Map<String, String> filters, int attempt) {

    try {
      return fetchPrompt(promptId, filters);
    } catch (PromptProviderException ppe) {
      if (ppe.isRetryable() && attempt < retryPolicy.maxRetries()) {
        sleepForRetry(attempt + 1);
        return fetchPromptWithRetry(promptId, filters, attempt + 1);
      }
      throw ppe;
    } catch (Exception e) {
      // Check if we should retry based on the error
      if (isRetryable(e) && attempt < retryPolicy.maxRetries()) {
        sleepForRetry(attempt + 1);
        return fetchPromptWithRetry(promptId, filters, attempt + 1);
      }
      throw new PromptProviderException(
          "Failed to fetch prompt: " + e.getMessage(), promptId, e, false);
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

  private LangfusePromptResponse fetchPrompt(
      String promptId, @Nullable Map<String, String> filters) {

    String encodedPromptId = URLEncoder.encode(promptId, StandardCharsets.UTF_8);
    HttpUrl.Builder urlBuilder =
        HttpUrl.parse(baseUrl + "/api/public/v2/prompts/" + encodedPromptId).newBuilder();

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

    Request request =
        new Request.Builder()
            .url(urlBuilder.build())
            .header("Authorization", authHeader)
            .header("Accept", "application/json")
            .get()
            .build();

    try {
      Response response = httpClient.newCall(request).execute();

      try (ResponseBody body = response.body()) {
        int code = response.code();

        if (!response.isSuccessful()) {
          String errorBody = body != null ? body.string() : "";
          boolean retryable = retryPolicy.isRetryable(code);

          String message =
              switch (code) {
                case 401 -> "Unauthorized: Invalid Langfuse credentials";
                case 403 -> "Forbidden: Access denied to prompt '" + promptId + "'";
                case 404 -> "Prompt not found: '" + promptId + "'";
                case 429 -> "Rate limited by Langfuse API";
                default -> "HTTP " + code + ": " + errorBody;
              };

          throw new PromptProviderException(message, promptId, null, retryable);
        }

        if (body == null) {
          throw new PromptProviderException("Empty response body", promptId);
        }

        String json = body.string();
        return objectMapper.readValue(json, LangfusePromptResponse.class);
      }
    } catch (IOException e) {
      throw new PromptProviderException(
          "Network error fetching prompt: " + e.getMessage(), promptId, e, true);
    }
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

  @Override
  public boolean exists(@NonNull String promptId) {
    Objects.requireNonNull(promptId, "promptId must not be null");

    if (promptId.isEmpty()) {
      return false;
    }

    try {
      // Use the list API with name filter to check existence
      LangfusePromptListResponse response = fetchPromptListWithRetry(promptId, 1, 1, 0);
      return response.getData().stream()
          .anyMatch(meta -> promptId.equals(meta.getName()));
    } catch (PromptProviderException e) {
      if (e.getMessage() != null && e.getMessage().contains("not found")) {
        return false;
      }
      throw e;
    } catch (Exception e) {
      throw new PromptProviderException(
          "Failed to check prompt existence: " + e.getMessage(), promptId, e, isRetryable(e));
    }
  }

  @Override
  public java.util.Set<String> listPromptIds() {
    java.util.Set<String> promptIds = new java.util.HashSet<>();
    int page = 1;
    int limit = 100; // Max items per page

    try {
      while (true) {
        LangfusePromptListResponse response = fetchPromptListWithRetry(null, page, limit, 0);
        
        for (LangfusePromptListResponse.PromptMeta meta : response.getData()) {
          if (meta.getName() != null) {
            promptIds.add(meta.getName());
          }
        }

        // Check if we've retrieved all pages
        LangfusePromptListResponse.PageMeta pageMeta = response.getMeta();
        if (pageMeta == null || page >= pageMeta.getTotalPages()) {
          break;
        }
        page++;
      }
    } catch (Exception e) {
      throw new PromptProviderException(
          "Failed to list prompts from Langfuse: " + e.getMessage(), null, e, isRetryable(e));
    }

    return java.util.Collections.unmodifiableSet(promptIds);
  }

  private LangfusePromptListResponse fetchPromptListWithRetry(
      @Nullable String name, int page, int limit, int attempt) {

    try {
      return fetchPromptList(name, page, limit);
    } catch (PromptProviderException ppe) {
      if (ppe.isRetryable() && attempt < retryPolicy.maxRetries()) {
        sleepForRetry(attempt + 1);
        return fetchPromptListWithRetry(name, page, limit, attempt + 1);
      }
      throw ppe;
    } catch (Exception e) {
      if (isRetryable(e) && attempt < retryPolicy.maxRetries()) {
        sleepForRetry(attempt + 1);
        return fetchPromptListWithRetry(name, page, limit, attempt + 1);
      }
      throw new PromptProviderException(
          "Failed to fetch prompt list: " + e.getMessage(), null, e, false);
    }
  }

  private LangfusePromptListResponse fetchPromptList(
      @Nullable String name, int page, int limit) {

    HttpUrl.Builder urlBuilder =
        HttpUrl.parse(baseUrl + "/api/public/v2/prompts").newBuilder()
            .addQueryParameter("page", String.valueOf(page))
            .addQueryParameter("limit", String.valueOf(limit));

    if (name != null && !name.isEmpty()) {
      urlBuilder.addQueryParameter("name", name);
    }

    Request request =
        new Request.Builder()
            .url(urlBuilder.build())
            .header("Authorization", authHeader)
            .header("Accept", "application/json")
            .get()
            .build();

    try {
      Response response = httpClient.newCall(request).execute();

      try (ResponseBody body = response.body()) {
        int code = response.code();

        if (!response.isSuccessful()) {
          String errorBody = body != null ? body.string() : "";
          boolean retryable = retryPolicy.isRetryable(code);

          String message =
              switch (code) {
                case 401 -> "Unauthorized: Invalid Langfuse credentials";
                case 403 -> "Forbidden: Access denied to prompts list";
                case 429 -> "Rate limited by Langfuse API";
                default -> "HTTP " + code + ": " + errorBody;
              };

          throw new PromptProviderException(message, null, null, retryable);
        }

        if (body == null) {
          throw new PromptProviderException("Empty response body", null);
        }

        String json = body.string();
        return objectMapper.readValue(json, LangfusePromptListResponse.class);
      }
    } catch (IOException e) {
      throw new PromptProviderException(
          "Network error fetching prompt list: " + e.getMessage(), null, e, true);
    }
  }

  /** Builder for creating {@link LangfusePromptProvider} instances. */
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
     * <p>Reads {@code LANGFUSE_PUBLIC_KEY}, {@code LANGFUSE_SECRET_KEY}, and optionally {@code
     * LANGFUSE_HOST}.
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
