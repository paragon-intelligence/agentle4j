# :material-code-braces: ApiException

> This docs was updated at: 2026-02-23

`com.paragon.responses.exception.ApiException` &nbsp;·&nbsp; **Class**

Extends `AgentleException`

---

Exception thrown when an API request fails.

Provides HTTP-specific context:

  
- `.statusCode()` - The HTTP status code
- `.requestId()` - Request correlation ID for debugging
- `.responseBody()` - Raw error response from the API

Example usage:

```java
if (error instanceof ApiException e) {
    log.error("API error {} (request {}): {}",
        e.statusCode(), e.requestId(), e.getMessage());
}
```

**See Also**

- `RateLimitException`
- `AuthenticationException`
- `ServerException`
- `InvalidRequestException`

## Methods

### `ApiException`

```java
public ApiException(
      @NonNull ErrorCode code,
      int statusCode,
      @NonNull String message,
      @Nullable String requestId,
      @Nullable String responseBody,
      @Nullable String suggestion,
      boolean retryable)
```

Creates a new ApiException.

**Parameters**

| Name | Description |
|------|-------------|
| `code` | the error code |
| `statusCode` | the HTTP status code |
| `message` | the error message |
| `requestId` | optional request correlation ID |
| `responseBody` | optional raw response body |
| `suggestion` | optional resolution hint |
| `retryable` | whether the error is retryable |

---

### `ApiException`

```java
public ApiException(
      @NonNull ErrorCode code,
      int statusCode,
      @NonNull String message,
      @NonNull Throwable cause,
      @Nullable String requestId,
      @Nullable String responseBody,
      @Nullable String suggestion,
      boolean retryable)
```

Creates a new ApiException with a cause.

**Parameters**

| Name | Description |
|------|-------------|
| `code` | the error code |
| `statusCode` | the HTTP status code |
| `message` | the error message |
| `cause` | the underlying cause |
| `requestId` | optional request correlation ID |
| `responseBody` | optional raw response body |
| `suggestion` | optional resolution hint |
| `retryable` | whether the error is retryable |

---

### `fromStatusCode`

```java
public static ApiException fromStatusCode(
      int statusCode,
      @NonNull String message,
      @Nullable String requestId,
      @Nullable String responseBody)
```

Creates an ApiException from an HTTP status code.

Automatically determines the appropriate subclass based on status code:

  
- 401/403 → `AuthenticationException`
- 429 → `RateLimitException`
- 4xx → `InvalidRequestException`
- 5xx → `ServerException`

**Parameters**

| Name | Description |
|------|-------------|
| `statusCode` | the HTTP status code |
| `message` | the error message |
| `requestId` | optional request correlation ID |
| `responseBody` | optional raw response body |

**Returns**

the appropriate ApiException subclass

---

### `statusCode`

```java
public int statusCode()
```

Returns the HTTP status code.

**Returns**

the status code

---

### `requestId`

```java
public @Nullable String requestId()
```

Returns the request correlation ID for debugging.

**Returns**

the request ID, or null if not available

---

### `responseBody`

```java
public @Nullable String responseBody()
```

Returns the raw error response body from the API.

**Returns**

the response body, or null if not available

