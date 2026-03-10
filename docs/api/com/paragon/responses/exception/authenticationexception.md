# :material-code-braces: AuthenticationException

> This docs was updated at: 2026-03-09












`com.paragon.responses.exception.AuthenticationException` &nbsp;·&nbsp; **Class**

Extends `ApiException`

---

Exception thrown when authentication fails (HTTP 401/403).

This exception is not retryable—the API key or credentials must be fixed.

Example usage:

```java
if (error instanceof AuthenticationException e) {
    log.error("Auth failed: {}", e.suggestion());
    // Prompt user to check API key
}
```

## Methods

### `AuthenticationException`

```java
public AuthenticationException(
      int statusCode,
      @NonNull String message,
      @Nullable String requestId,
      @Nullable String responseBody)
```

Creates a new AuthenticationException.

**Parameters**

| Name | Description |
|------|-------------|
| `statusCode` | the HTTP status code (401 or 403) |
| `message` | the error message |
| `requestId` | optional request correlation ID |
| `responseBody` | optional raw response body |

