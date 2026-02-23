# :material-code-braces: OpenRouterModelRegistry

> This docs was updated at: 2026-02-23

`com.paragon.responses.openrouter.OpenRouterModelRegistry` &nbsp;·&nbsp; **Class**

---

Registry for OpenRouter models with TTL-based caching.

This registry fetches the models list from OpenRouter only once, and caches it for a
configurable TTL (default: 1 hour). The cache is refreshed automatically when expired.

Thread-safe for concurrent access.

Usage:

```java
var registry = OpenRouterModelRegistry.builder()
    .apiKey("your-api-key")
    .build();
Optional cost = registry.calculateCost("openai/gpt-4o", 1000, 500);
```

## Methods

### `builder`

```java
public static @NonNull Builder builder()
```

Creates a new builder for OpenRouterModelRegistry.

---

### `fromEnv`

```java
public static @NonNull OpenRouterModelRegistry fromEnv()
```

Creates a registry from environment variables. Uses OPENROUTER_API_KEY for authentication.

---

### `getModel`

```java
public @NonNull Optional<OpenRouterModel> getModel(@NonNull String modelId)
```

Gets a model by its ID.

**Parameters**

| Name | Description |
|------|-------------|
| `modelId` | the model ID (e.g., "openai/gpt-4o") |

**Returns**

the model if found

---

### `getPricing`

```java
public @NonNull Optional<OpenRouterModelPricing> getPricing(@NonNull String modelId)
```

Gets pricing information for a model.

**Parameters**

| Name | Description |
|------|-------------|
| `modelId` | the model ID |

**Returns**

the pricing if model is found

---

### `calculateCost`

```java
public @NonNull Optional<BigDecimal> calculateCost(
      @NonNull String modelId, int inputTokens, int outputTokens)
```

Calculates the cost for a request with the given model and token counts.

**Parameters**

| Name | Description |
|------|-------------|
| `modelId` | the model ID |
| `inputTokens` | number of input tokens |
| `outputTokens` | number of output tokens |

**Returns**

the calculated cost in USD, or empty if model not found or pricing invalid

---

### `invalidateCache`

```java
public void invalidateCache()
```

Forces a cache refresh on the next access.

---

### `getCachedModelCount`

```java
public int getCachedModelCount()
```

Returns the number of models in the cache.

---

### `isInitialized`

```java
public boolean isInitialized()
```

Returns true if the cache has been initialized.

---

### `ensureCacheFresh`

```java
private void ensureCacheFresh()
```

Ensures the cache is fresh, refreshing if expired.

---

### `refreshCache`

```java
private void refreshCache()
```

Fetches models from API and updates the cache.

---

### `httpClient`

```java
public @NonNull Builder httpClient(@NonNull OkHttpClient httpClient)
```

Sets the HTTP client.

---

### `objectMapper`

```java
public @NonNull Builder objectMapper(@NonNull ObjectMapper objectMapper)
```

Sets the ObjectMapper for JSON parsing.

---

### `apiKey`

```java
public @NonNull Builder apiKey(@NonNull String apiKey)
```

Sets the OpenRouter API key.

---

### `cacheTtl`

```java
public @NonNull Builder cacheTtl(@NonNull Duration cacheTtl)
```

Sets the cache TTL (time-to-live).

---

### `fromEnv`

```java
public @NonNull Builder fromEnv()
```

Loads configuration from environment variables. Uses OPENROUTER_API_KEY for the API key.

---

### `build`

```java
public @NonNull OpenRouterModelRegistry build()
```

Builds the registry.
