# :material-code-braces: ServerException

> This docs was updated at: 2026-02-23

`com.paragon.responses.exception.ServerException` &nbsp;·&nbsp; **Class**

Extends `ApiException`

---

Exception thrown when the server encounters an error (HTTP 5xx).

Server errors are typically retryable. The built-in retry policy will automatically retry on
500, 502, 503, and 504 status codes.

Example usage:

```java
if (error instanceof ServerException e && e.isRetryable()) {
    // Log and let built-in retry handle it
    log.warn("Server error {}, retrying: {}", e.statusCode(), e.getMessage());
}
```

## Methods

### `ServerException`

```java
public ServerException(
      int statusCode,
      @NonNull String message,
      @Nullable String requestId,
      @Nullable String responseBody)
```

Creates a new ServerException.

**Parameters**

| Name | Description |
|------|-------------|
| `statusCode` | the HTTP status code (5xx) |
| `message` | the error message |
| `requestId` | optional request correlation ID |
| `responseBody` | optional raw response body |

