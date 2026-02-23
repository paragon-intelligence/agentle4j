# :material-code-braces: OpenRouterEmbeddingProvider

> This docs was updated at: 2026-02-23

`com.paragon.embeddings.OpenRouterEmbeddingProvider` &nbsp;·&nbsp; **Class**

Implements `EmbeddingProvider`

---

Embedding provider for OpenRouter's embedding API with built-in retry support.

Automatically retries on:

  
- 429 Too Many Requests - Rate limit exceeded
- 529 Provider Overloaded - Temporary overload, uses fallback providers when enabled
- 5xx Server Errors - Transient server issues

**Virtual Thread Design:** Uses synchronous API optimized for Java 21+ virtual threads.

Example usage:

```java
EmbeddingProvider embeddings = OpenRouterEmbeddingProvider.builder()
    .apiKey(System.getenv("OPENROUTER_API_KEY"))
    .retryPolicy(RetryPolicy.defaults())  // Retry on 429, 529, 5xx
    .allowFallbacks(true)                 // Use backup providers on 529
    .build();
List results = embeddings.createEmbeddings(
    List.of("Hello world", "AI is amazing"),
    "openai/text-embedding-3-small"
);
```

## Methods

### `builder`

```java
public static @NonNull Builder builder()
```

Creates a new builder for OpenRouterEmbeddingProvider.

**Returns**

a new builder instance

---

### `close`

```java
public void close()
```

Closes the underlying HTTP client and releases resources.

---

### `apiKey`

```java
public @NonNull Builder apiKey(@NonNull String apiKey)
```

Sets the OpenRouter API key (required).

Can also be loaded from the environment variable `OPENROUTER_API_KEY` if not
provided explicitly.

**Parameters**

| Name | Description |
|------|-------------|
| `apiKey` | the API key |

**Returns**

this builder

---

### `objectMapper`

```java
public @NonNull Builder objectMapper(@NonNull ObjectMapper objectMapper)
```

Sets the ObjectMapper for JSON serialization/deserialization.

**Parameters**

| Name | Description |
|------|-------------|
| `objectMapper` | the object mapper |

**Returns**

this builder

---

### `httpClient`

```java
public @NonNull Builder httpClient(@NonNull OkHttpClient httpClient)
```

Sets the OkHttpClient for HTTP requests.

**Parameters**

| Name | Description |
|------|-------------|
| `httpClient` | the HTTP client |

**Returns**

this builder

---

### `retryPolicy`

```java
public @NonNull Builder retryPolicy(@NonNull RetryPolicy retryPolicy)
```

Sets the retry policy for handling transient failures.

Default: `RetryPolicy.defaults()` which retries 3 times on 429, 529, and 5xx errors.

**Parameters**

| Name | Description |
|------|-------------|
| `retryPolicy` | the retry policy |

**Returns**

this builder

---

### `allowFallbacks`

```java
public @NonNull Builder allowFallbacks(boolean allowFallbacks)
```

Configures whether to allow fallback providers when the primary is overloaded (529).

When enabled, OpenRouter will automatically route to backup embedding providers if the
primary provider is overloaded. Default: true.

**Parameters**

| Name | Description |
|------|-------------|
| `allowFallbacks` | true to enable fallbacks |

**Returns**

this builder

---

### `build`

```java
public @NonNull OpenRouterEmbeddingProvider build()
```

Builds the OpenRouterEmbeddingProvider.

**Returns**

a new OpenRouterEmbeddingProvider instance

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if required fields are missing |

