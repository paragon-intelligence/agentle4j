# :material-code-braces: ToolExecutionException

`com.paragon.responses.exception.ToolExecutionException` &nbsp;·&nbsp; **Class**

Extends `AgentleException`

---

Exception thrown when a tool execution fails.

This exception is not retryable—the tool implementation or arguments need to be fixed.

Example usage:

```java
if (error instanceof ToolExecutionException e) {
    log.error("Tool {} failed: {}", e.toolName(), e.getMessage());
}
```

## Methods

### `ToolExecutionException`

```java
public ToolExecutionException(
      @NonNull String toolName,
      @Nullable String callId,
      @Nullable String arguments,
      @NonNull String message)
```

Creates a new ToolExecutionException.

**Parameters**

| Name | Description |
|------|-------------|
| `toolName` | the name of the tool that failed |
| `callId` | the tool call ID |
| `arguments` | the tool arguments (as JSON) |
| `message` | the error message |

---

### `ToolExecutionException`

```java
public ToolExecutionException(
      @NonNull String toolName,
      @Nullable String callId,
      @Nullable String arguments,
      @NonNull String message,
      @NonNull Throwable cause)
```

Creates a new ToolExecutionException with a cause.

**Parameters**

| Name | Description |
|------|-------------|
| `toolName` | the name of the tool that failed |
| `callId` | the tool call ID |
| `arguments` | the tool arguments (as JSON) |
| `message` | the error message |
| `cause` | the underlying cause |

---

### `toolName`

```java
public @NonNull String toolName()
```

Returns the name of the tool that failed.

**Returns**

the tool name

---

### `callId`

```java
public @Nullable String callId()
```

Returns the tool call ID.

**Returns**

the call ID, or null if not available

---

### `arguments`

```java
public @Nullable String arguments()
```

Returns the tool arguments as JSON.

**Returns**

the arguments, or null if not available

