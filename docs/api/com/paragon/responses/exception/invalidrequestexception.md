# :material-code-braces: InvalidRequestException

`com.paragon.responses.exception.InvalidRequestException` &nbsp;·&nbsp; **Class**

Extends `ApiException`

---

Exception thrown when the request is invalid (HTTP 4xx, excluding 401/403/429).

This exception is not retryable—the request must be fixed.

Common causes:

  
- Invalid JSON payload
- Missing required parameters
- Invalid parameter values
- Unsupported model or feature

Example usage:

```java
if (error instanceof InvalidRequestException e) {
    log.error("Invalid request: {}", e.getMessage());
    // Check payload construction
}
```

## Methods

### `InvalidRequestException`

```java
public InvalidRequestException(
      int statusCode,
      @NonNull String message,
      @Nullable String requestId,
      @Nullable String responseBody)
```

Creates a new InvalidRequestException.

**Parameters**

| Name | Description |
|------|-------------|
| `statusCode` | the HTTP status code (4xx) |
| `message` | the error message |
| `requestId` | optional request correlation ID |
| `responseBody` | optional raw response body |

---

### `InvalidRequestException`

```java
public InvalidRequestException(@NonNull String message, @NonNull Throwable cause)
```

Creates a new InvalidRequestException with a cause.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the error message |
| `cause` | the underlying cause |

