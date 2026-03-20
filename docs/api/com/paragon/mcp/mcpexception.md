# :material-code-braces: McpException

`com.paragon.mcp.McpException` &nbsp;·&nbsp; **Class**

Extends `RuntimeException`

---

Exception thrown when MCP operations fail.

This exception wraps various failure modes including:

  
- Connection failures (server not reachable, process failed to start)
- Protocol errors (invalid JSON-RPC, unexpected response format)
- Tool execution failures (tool returned error, timeout)
- Session errors (session expired, invalid session)

## Methods

### `McpException`

```java
public McpException(@NonNull String message)
```

Creates an MCP exception with a message.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the error message |

---

### `McpException`

```java
public McpException(@NonNull String message, @Nullable Throwable cause)
```

Creates an MCP exception with a message and cause.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the error message |
| `cause` | the underlying cause |

---

### `McpException`

```java
public McpException(@NonNull String message, int errorCode)
```

Creates an MCP exception with a JSON-RPC error code.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the error message |
| `errorCode` | the JSON-RPC error code |

---

### `McpException`

```java
public McpException(@NonNull String message, int errorCode, @Nullable Throwable cause)
```

Creates an MCP exception with a JSON-RPC error code and cause.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the error message |
| `errorCode` | the JSON-RPC error code |
| `cause` | the underlying cause |

---

### `getErrorCode`

```java
public @Nullable Integer getErrorCode()
```

Returns the JSON-RPC error code if available.

**Returns**

the error code, or null if not a JSON-RPC error

---

### `connectionFailed`

```java
public static McpException connectionFailed(@NonNull String message, @Nullable Throwable cause)
```

Creates an exception for connection failures.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the error message |
| `cause` | the underlying cause |

**Returns**

a new McpException

---

### `protocolError`

```java
public static McpException protocolError(@NonNull String message)
```

Creates an exception for protocol errors.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the error message |

**Returns**

a new McpException

---

### `fromJsonRpcError`

```java
public static McpException fromJsonRpcError(int code, @NonNull String message)
```

Creates an exception from a JSON-RPC error response.

**Parameters**

| Name | Description |
|------|-------------|
| `code` | the JSON-RPC error code |
| `message` | the error message |

**Returns**

a new McpException

---

### `toolExecutionFailed`

```java
public static McpException toolExecutionFailed(
      @NonNull String toolName, @NonNull String message)
```

Creates an exception for tool execution failures.

**Parameters**

| Name | Description |
|------|-------------|
| `toolName` | the name of the tool that failed |
| `message` | the error message |

**Returns**

a new McpException

