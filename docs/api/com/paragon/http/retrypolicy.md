# :material-database: RetryPolicy

> This docs was updated at: 2026-02-23

`com.paragon.http.RetryPolicy` &nbsp;·&nbsp; **Record**

---

Configuration for retry behavior with exponential backoff.

Use this to configure how HTTP clients handle transient failures such as rate limiting (HTTP
429), provider overload (HTTP 529), and server errors (HTTP 5xx).

Example usage:

```java
// Simple configuration
AsyncHttpClient.builder()
    .baseUrl("https://api.example.com")
    .retryPolicy(RetryPolicy.defaults())
    .build();
// Advanced configuration
AsyncHttpClient.builder()
    .baseUrl("https://api.example.com")
    .retryPolicy(RetryPolicy.builder()
        .maxRetries(5)
        .initialDelay(Duration.ofMillis(500))
        .maxDelay(Duration.ofSeconds(30))
        .multiplier(2.0)
        .build())
    .build();
```

## Methods

### `of`

```java
public static final Set<Integer> DEFAULT_RETRYABLE_STATUS_CODES =
      Set.of(429, 500, 502, 503, 504, 529)
```

Default retryable status codes: 429 (rate limit), 500, 502, 503, 504 (server errors), 529
(provider overloaded).

---

### `defaults`

```java
public static @NonNull RetryPolicy defaults()
```

Returns the default retry policy with sensible defaults:

  
- maxRetries: 3
- initialDelay: 1 second
- maxDelay: 30 seconds
- multiplier: 2.0
- retryableStatusCodes: 429, 500, 502, 503, 504, 529

---

### `disabled`

```java
public static @NonNull RetryPolicy disabled()
```

Returns a retry policy that disables retries (maxRetries = 0). Use this when you want to handle
retries manually or disable them entirely.

---

### `none`

```java
public static @NonNull RetryPolicy none()
```

Alias for `.disabled()`. Returns a retry policy that disables retries.

**Returns**

a retry policy with no retries

---

### `builder`

```java
public static @NonNull Builder builder()
```

Creates a new builder for constructing a custom retry policy.

**Returns**

a new builder instance

---

### `getDelayForAttempt`

```java
public @NonNull Duration getDelayForAttempt(int attempt)
```

Calculates the delay for a specific retry attempt using exponential backoff.

**Parameters**

| Name | Description |
|------|-------------|
| `attempt` | the retry attempt number (1-based) |

**Returns**

the delay duration, capped at maxDelay

---

### `delayForAttempt`

```java
public long delayForAttempt(int attempt)
```

Returns the delay in milliseconds for a specific retry attempt using exponential backoff.

**Parameters**

| Name | Description |
|------|-------------|
| `attempt` | the retry attempt number (1-based) |

**Returns**

the delay in milliseconds, capped at maxDelay

---

### `isRetryable`

```java
public boolean isRetryable(int statusCode)
```

Checks if the given HTTP status code should trigger a retry.

**Parameters**

| Name | Description |
|------|-------------|
| `statusCode` | the HTTP status code |

**Returns**

true if the status code is retryable

---

### `shouldRetry`

```java
public boolean shouldRetry(@NonNull HttpException error, int attempt)
```

Determines if a request should be retried based on the error and attempt count.

**Parameters**

| Name | Description |
|------|-------------|
| `error` | the HTTP exception that occurred |
| `attempt` | the current attempt number (1-based, after failure) |

**Returns**

true if the request should be retried

---

### `maxRetries`

```java
public @NonNull Builder maxRetries(int maxRetries)
```

Sets the maximum number of retry attempts.

**Parameters**

| Name | Description |
|------|-------------|
| `maxRetries` | max retries (0 = no retries) |

**Returns**

this builder

---

### `initialDelay`

```java
public @NonNull Builder initialDelay(@NonNull Duration initialDelay)
```

Sets the initial delay before the first retry.

**Parameters**

| Name | Description |
|------|-------------|
| `initialDelay` | the initial delay |

**Returns**

this builder

---

### `maxDelay`

```java
public @NonNull Builder maxDelay(@NonNull Duration maxDelay)
```

Sets the maximum delay between retries (caps exponential growth).

**Parameters**

| Name | Description |
|------|-------------|
| `maxDelay` | the maximum delay |

**Returns**

this builder

---

### `multiplier`

```java
public @NonNull Builder multiplier(double multiplier)
```

Sets the multiplier for exponential backoff.

**Parameters**

| Name | Description |
|------|-------------|
| `multiplier` | the multiplier (must be >= 1.0) |

**Returns**

this builder

---

### `retryableStatusCodes`

```java
public @NonNull Builder retryableStatusCodes(@NonNull Set<Integer> statusCodes)
```

Sets the HTTP status codes that should trigger a retry.

**Parameters**

| Name | Description |
|------|-------------|
| `statusCodes` | the retryable status codes |

**Returns**

this builder

---

### `build`

```java
public @NonNull RetryPolicy build()
```

Builds the retry policy.

**Returns**

the constructed RetryPolicy

