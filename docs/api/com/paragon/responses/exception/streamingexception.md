# :material-code-braces: StreamingException

`com.paragon.responses.exception.StreamingException` &nbsp;·&nbsp; **Class**

Extends `AgentleException`

---

Exception thrown when an error occurs during streaming.

Provides streaming-specific context:

  
- `.partialOutput()` - Any content received before the failure
- `.bytesReceived()` - Total bytes received before failure

Example usage:

```java
responder.respond(streamingPayload)
    .onError(error -> {
        if (error instanceof StreamingException se && se.partialOutput() != null) {
            savePartialOutput(se.partialOutput());
        }
    })
    .start();
```

## Methods

### `StreamingException`

```java
public StreamingException(
      @NonNull ErrorCode code,
      @NonNull String message,
      @Nullable String partialOutput,
      long bytesReceived,
      boolean retryable)
```

Creates a new StreamingException.

**Parameters**

| Name | Description |
|------|-------------|
| `code` | the error code |
| `message` | the error message |
| `partialOutput` | any output received before failure |
| `bytesReceived` | total bytes received |
| `retryable` | whether the error is retryable |

---

### `StreamingException`

```java
public StreamingException(
      @NonNull ErrorCode code,
      @NonNull String message,
      @NonNull Throwable cause,
      @Nullable String partialOutput,
      long bytesReceived,
      boolean retryable)
```

Creates a new StreamingException with a cause.

**Parameters**

| Name | Description |
|------|-------------|
| `code` | the error code |
| `message` | the error message |
| `cause` | the underlying cause |
| `partialOutput` | any output received before failure |
| `bytesReceived` | total bytes received |
| `retryable` | whether the error is retryable |

---

### `connectionDropped`

```java
public static StreamingException connectionDropped(
      @NonNull Throwable cause, @Nullable String partialOutput, long bytesReceived)
```

Creates a connection dropped exception.

**Parameters**

| Name | Description |
|------|-------------|
| `cause` | the underlying cause |
| `partialOutput` | any output received before failure |
| `bytesReceived` | total bytes received |

**Returns**

a new StreamingException

---

### `timeout`

```java
public static StreamingException timeout(@Nullable String partialOutput, long bytesReceived)
```

Creates a stream timeout exception.

**Parameters**

| Name | Description |
|------|-------------|
| `partialOutput` | any output received before timeout |
| `bytesReceived` | total bytes received |

**Returns**

a new StreamingException

---

### `partialOutput`

```java
public @Nullable String partialOutput()
```

Returns any output received before the failure.

This allows recovery of partial content for UI display or caching.

**Returns**

the partial output, or null if nothing was received

---

### `bytesReceived`

```java
public long bytesReceived()
```

Returns the total bytes received before failure.

**Returns**

bytes received

