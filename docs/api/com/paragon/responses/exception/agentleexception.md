# :material-code-braces: AgentleException

`com.paragon.responses.exception.AgentleException` &nbsp;·&nbsp; **Class**

Extends `RuntimeException`

---

Base exception for all Agentle4j errors.

All exceptions in the Agentle hierarchy extend this class, providing:

  
- `.code()` - Machine-readable error code for programmatic handling
- `.suggestion()` - Optional hint for resolution
- `.isRetryable()` - Whether this error is safe to retry

Example usage:

```java
responder.respond(payload)
    .exceptionally(error -> {
        if (error.getCause() instanceof AgentleException e && e.isRetryable()) {
            // Retry logic
        }
        return null;
    });
```

## Methods

### `AgentleException`

```java
public AgentleException(
      @NonNull ErrorCode code,
      @NonNull String message,
      @Nullable String suggestion,
      boolean retryable)
```

Creates a new AgentleException.

**Parameters**

| Name | Description |
|------|-------------|
| `code` | the error code |
| `message` | the error message |
| `suggestion` | optional resolution hint |
| `retryable` | whether the error is retryable |

---

### `AgentleException`

```java
public AgentleException(
      @NonNull ErrorCode code,
      @NonNull String message,
      @NonNull Throwable cause,
      @Nullable String suggestion,
      boolean retryable)
```

Creates a new AgentleException with a cause.

**Parameters**

| Name | Description |
|------|-------------|
| `code` | the error code |
| `message` | the error message |
| `cause` | the underlying cause |
| `suggestion` | optional resolution hint |
| `retryable` | whether the error is retryable |

---

### `code`

```java
public @NonNull ErrorCode code()
```

Returns the machine-readable error code.

**Returns**

the error code

---

### `suggestion`

```java
public @Nullable String suggestion()
```

Returns an optional hint for resolving this error.

**Returns**

the suggestion, or null if none

---

### `isRetryable`

```java
public boolean isRetryable()
```

Returns whether this error is safe to retry.

**Returns**

true if retryable

