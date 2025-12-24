package com.paragon.responses.openrouter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import okhttp3.*;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry for OpenRouter models with TTL-based caching.
 *
 * <p>This registry fetches the models list from OpenRouter only once, and caches it for a
 * configurable TTL (default: 1 hour). The cache is refreshed automatically when expired.
 *
 * <p>Thread-safe for concurrent access.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * var registry = OpenRouterModelRegistry.builder()
 *     .apiKey("your-api-key")
 *     .build();
 *
 * Optional<BigDecimal> cost = registry.calculateCost("openai/gpt-4o", 1000, 500);
 * }</pre>
 */
public class OpenRouterModelRegistry {

  private static final Logger logger = LoggerFactory.getLogger(OpenRouterModelRegistry.class);
  private static final String MODELS_ENDPOINT = "https://openrouter.ai/api/v1/models";
  private static final Duration DEFAULT_TTL = Duration.ofHours(1);

  private final OkHttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final Duration cacheTtl;

  // Cache state
  private final Map<String, OpenRouterModel> modelsCache = new ConcurrentHashMap<>();
  private final AtomicReference<Instant> cacheExpiry = new AtomicReference<>(Instant.EPOCH);
  private final ReentrantLock refreshLock = new ReentrantLock();
  private volatile boolean initialized = false;

  private OpenRouterModelRegistry(
      @NonNull OkHttpClient httpClient,
      @NonNull ObjectMapper objectMapper,
      @NonNull String apiKey,
      @NonNull Duration cacheTtl) {
    this.httpClient = Objects.requireNonNull(httpClient);
    this.objectMapper = Objects.requireNonNull(objectMapper);
    this.apiKey = Objects.requireNonNull(apiKey);
    this.cacheTtl = Objects.requireNonNull(cacheTtl);
  }

  /** Creates a new builder for OpenRouterModelRegistry. */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /** Creates a registry from environment variables. Uses OPENROUTER_API_KEY for authentication. */
  public static @NonNull OpenRouterModelRegistry fromEnv() {
    return builder().fromEnv().build();
  }

  /**
   * Gets a model by its ID.
   *
   * @param modelId the model ID (e.g., "openai/gpt-4o")
   * @return the model if found
   */
  public @NonNull Optional<OpenRouterModel> getModel(@NonNull String modelId) {
    ensureCacheFresh();
    return Optional.ofNullable(modelsCache.get(modelId));
  }

  /**
   * Gets pricing information for a model.
   *
   * @param modelId the model ID
   * @return the pricing if model is found
   */
  public @NonNull Optional<OpenRouterModelPricing> getPricing(@NonNull String modelId) {
    return getModel(modelId).map(OpenRouterModel::pricing);
  }

  /**
   * Calculates the cost for a request with the given model and token counts.
   *
   * @param modelId the model ID
   * @param inputTokens number of input tokens
   * @param outputTokens number of output tokens
   * @return the calculated cost in USD, or empty if model not found or pricing invalid
   */
  public @NonNull Optional<BigDecimal> calculateCost(
      @NonNull String modelId, int inputTokens, int outputTokens) {
    return getModel(modelId).map(model -> model.calculateCost(inputTokens, outputTokens));
  }

  /** Forces a cache refresh on the next access. */
  public void invalidateCache() {
    cacheExpiry.set(Instant.EPOCH);
    initialized = false;
  }

  /** Returns the number of models in the cache. */
  public int getCachedModelCount() {
    return modelsCache.size();
  }

  /** Returns true if the cache has been initialized. */
  public boolean isInitialized() {
    return initialized;
  }

  /** Ensures the cache is fresh, refreshing if expired. */
  private void ensureCacheFresh() {
    Instant expiry = cacheExpiry.get();
    Instant now = Instant.now();

    if (now.isAfter(expiry)) {
      if (refreshLock.tryLock()) {
        try {
          // Double-check after acquiring lock
          if (now.isAfter(cacheExpiry.get())) {
            refreshCache();
          }
        } finally {
          refreshLock.unlock();
        }
      }
      // If we couldn't get the lock, another thread is refreshing
      // Use stale cache data until refresh completes
    }
  }

  /** Fetches models from API and updates the cache. */
  private void refreshCache() {
    logger.info("Fetching OpenRouter models list...");

    Request request =
        new Request.Builder()
            .url(MODELS_ENDPOINT)
            .header("Authorization", "Bearer " + apiKey)
            .get()
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        logger.error(
            "Failed to fetch OpenRouter models: {} {}", response.code(), response.message());
        return;
      }

      ResponseBody body = response.body();
      if (body == null) {
        logger.error("Empty response from OpenRouter models API");
        return;
      }

      String json = body.string();
      OpenRouterModelsResponse modelsResponse =
          objectMapper.readValue(json, OpenRouterModelsResponse.class);

      // Update cache
      modelsCache.clear();
      for (OpenRouterModel model : modelsResponse.data()) {
        modelsCache.put(model.id(), model);
      }

      // Set new expiry
      cacheExpiry.set(Instant.now().plus(cacheTtl));
      initialized = true;

      logger.info("Cached {} OpenRouter models", modelsCache.size());

    } catch (IOException e) {
      logger.error("Error fetching OpenRouter models: {}", e.getMessage(), e);
    }
  }

  /** Builder for OpenRouterModelRegistry. */
  public static class Builder {
    private OkHttpClient httpClient;
    private ObjectMapper objectMapper;
    private String apiKey;
    private Duration cacheTtl = DEFAULT_TTL;

    /** Sets the HTTP client. */
    public @NonNull Builder httpClient(@NonNull OkHttpClient httpClient) {
      this.httpClient = Objects.requireNonNull(httpClient);
      return this;
    }

    /** Sets the ObjectMapper for JSON parsing. */
    public @NonNull Builder objectMapper(@NonNull ObjectMapper objectMapper) {
      this.objectMapper = Objects.requireNonNull(objectMapper);
      return this;
    }

    /** Sets the OpenRouter API key. */
    public @NonNull Builder apiKey(@NonNull String apiKey) {
      this.apiKey = Objects.requireNonNull(apiKey);
      return this;
    }

    /** Sets the cache TTL (time-to-live). */
    public @NonNull Builder cacheTtl(@NonNull Duration cacheTtl) {
      this.cacheTtl = Objects.requireNonNull(cacheTtl);
      return this;
    }

    /** Loads configuration from environment variables. Uses OPENROUTER_API_KEY for the API key. */
    public @NonNull Builder fromEnv() {
      this.apiKey = System.getenv("OPENROUTER_API_KEY");
      return this;
    }

    /** Builds the registry. */
    public @NonNull OpenRouterModelRegistry build() {
      if (apiKey == null || apiKey.isBlank()) {
        throw new IllegalStateException("OpenRouter API key is required");
      }
      if (httpClient == null) {
        httpClient = new OkHttpClient();
      }
      if (objectMapper == null) {
        objectMapper = new ObjectMapper();
      }

      return new OpenRouterModelRegistry(httpClient, objectMapper, apiKey, cacheTtl);
    }
  }
}
