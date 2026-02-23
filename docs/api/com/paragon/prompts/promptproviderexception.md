# :material-code-braces: PromptProviderException

> This docs was updated at: 2026-02-23

`com.paragon.prompts.PromptProviderException` &nbsp;·&nbsp; **Class**

Extends `RuntimeException`

---

Exception thrown when a prompt cannot be retrieved from a `PromptProvider`.

This exception wraps underlying failures such as IO errors, network failures, or API errors,
providing a consistent exception type for prompt retrieval operations.

*Since: 1.0*

## Methods

### `PromptProviderException`

```java
public PromptProviderException(@NonNull String message, @Nullable String promptId)
```

Creates a new exception for a prompt retrieval failure.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the error message |
| `promptId` | the ID of the prompt that failed to load |

---

### `PromptProviderException`

```java
public PromptProviderException(
      @NonNull String message, @Nullable String promptId, @Nullable Throwable cause)
```

Creates a new exception with a cause.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the error message |
| `promptId` | the ID of the prompt that failed to load |
| `cause` | the underlying cause |

---

### `PromptProviderException`

```java
public PromptProviderException(
      @NonNull String message,
      @Nullable String promptId,
      @Nullable Throwable cause,
      boolean retryable)
```

Creates a new exception with retryability information.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the error message |
| `promptId` | the ID of the prompt that failed to load |
| `cause` | the underlying cause |
| `retryable` | whether the operation can be retried |

---

### `promptId`

```java
public Optional<String> promptId()
```

Returns the ID of the prompt that failed to load.

**Returns**

an Optional containing the prompt ID, or empty if not available

---

### `isRetryable`

```java
public boolean isRetryable()
```

Returns whether the operation can be retried.

This is typically true for transient failures such as network timeouts or rate limiting, and
false for permanent failures like missing prompts.

**Returns**

true if retryable, false otherwise

