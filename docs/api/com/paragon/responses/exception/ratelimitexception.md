# :material-code-braces: RateLimitException

`com.paragon.responses.exception.RateLimitException` &nbsp;·&nbsp; **Class**

Extends `ApiException`

---

Exception thrown when rate limited by the API (HTTP 429).

This exception is always retryable. Use `.retryAfter()` to determine when to retry.

Example usage:

```java
if (error instanceof RateLimitException e) {
    Duration wait = e.retryAfter();
    if (wait != null) {
        Thread.sleep(wait.toMillis());
    }
    // Retry request
}
```

## Methods

### `RateLimitException`

```java
public RateLimitException(
      @NonNull String message,
      @Nullable String requestId,
      @Nullable String responseBody,
      @Nullable Duration retryAfter)
```

Creates a new RateLimitException.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the error message |
| `requestId` | optional request correlation ID |
| `responseBody` | optional raw response body |
| `retryAfter` | optional duration to wait before retrying |

---

### `retryAfter`

```java
public @Nullable Duration retryAfter()
```

Returns the recommended duration to wait before retrying.

This is parsed from the API's Retry-After header if available.

**Returns**

the retry-after duration, or null if not specified

