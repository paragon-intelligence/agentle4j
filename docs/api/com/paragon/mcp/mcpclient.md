# :material-code-braces: McpClient

`com.paragon.mcp.McpClient` &nbsp;·&nbsp; **Class**

Implements `AutoCloseable`

---

Abstract base class for MCP client implementations.

This class handles the MCP protocol lifecycle and provides common functionality for both Stdio
and HTTP transports.

### Lifecycle

  
- `.connect()` - Establishes connection and performs initialization handshake
- `.listTools()` / `Map)` - Perform operations
- `.close()` - Closes connection and cleans up resources

**See Also**

- `StdioMcpClient`
- `StreamableHttpMcpClient`

## Methods

### `McpClient`

```java
protected McpClient(@NonNull ObjectMapper objectMapper)
```

Creates a new MCP client.

**Parameters**

| Name | Description |
|------|-------------|
| `objectMapper` | the object mapper for JSON serialization |

---

### `doConnect`

```java
protected abstract void doConnect() throws McpException
```

Establishes the underlying transport connection.

**Throws**

| Type | Condition |
|------|-----------|
| `McpException` | if connection fails |

---

### `sendRequest`

```java
protected abstract void sendRequest(@NonNull JsonRpcRequest request) throws McpException
```

Sends a JSON-RPC request over the transport.

**Parameters**

| Name | Description |
|------|-------------|
| `request` | the request to send |

**Throws**

| Type | Condition |
|------|-----------|
| `McpException` | if sending fails |

---

### `readResponse`

```java
protected abstract @NonNull JsonRpcResponse readResponse() throws McpException
```

Reads a JSON-RPC response from the transport.

**Returns**

the response

**Throws**

| Type | Condition |
|------|-----------|
| `McpException` | if reading fails |

---

### `doClose`

```java
protected abstract void doClose() throws McpException
```

Closes the underlying transport connection.

**Throws**

| Type | Condition |
|------|-----------|
| `McpException` | if closing fails |

---

### `connect`

```java
public void connect() throws McpException
```

Connects to the MCP server and performs initialization.

This method establishes the transport connection, sends the initialize request, and sends
the initialized notification.

**Throws**

| Type | Condition |
|------|-----------|
| `McpException` | if connection or initialization fails |

---

### `listTools`

```java
public @NonNull List<McpToolDefinition> listTools() throws McpException
```

Lists all tools available on the MCP server.

**Returns**

the list of tool definitions

**Throws**

| Type | Condition |
|------|-----------|
| `McpException` | if listing fails or client not initialized |

---

### `callTool`

```java
public @NonNull McpToolResult callTool(
      @NonNull String name, @Nullable Map<String, Object> arguments) throws McpException
```

Calls a tool on the MCP server.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the tool name |
| `arguments` | the tool arguments |

**Returns**

the tool result

**Throws**

| Type | Condition |
|------|-----------|
| `McpException` | if the call fails or client not initialized |

---

### `asTools`

```java
public @NonNull List<McpRemoteTool> asTools() throws McpException
```

Returns all MCP tools as McpRemoteTool instances that can be used with agents.

**Returns**

list of MCP remote tools

**Throws**

| Type | Condition |
|------|-----------|
| `McpException` | if listing fails |

---

### `asTools`

```java
public @NonNull List<McpRemoteTool> asTools(@Nullable Set<String> allowedToolNames)
      throws McpException
```

Returns filtered MCP tools as McpRemoteTool instances.

**Parameters**

| Name | Description |
|------|-------------|
| `allowedToolNames` | optional set of tool names to include (null for all) |

**Returns**

list of MCP remote tools

**Throws**

| Type | Condition |
|------|-----------|
| `McpException` | if listing fails |

---

### `toJson`

```java
protected @NonNull String toJson(@NonNull Object obj) throws McpException
```

Serializes an object to JSON string.

**Parameters**

| Name | Description |
|------|-------------|
| `obj` | the object to serialize |

**Returns**

the JSON string

**Throws**

| Type | Condition |
|------|-----------|
| `McpException` | if serialization fails |

---

### `parseResponse`

```java
protected @NonNull JsonRpcResponse parseResponse(@NonNull String json) throws McpException
```

Deserializes a JSON string to JsonRpcResponse.

**Parameters**

| Name | Description |
|------|-------------|
| `json` | the JSON string |

**Returns**

the parsed response

**Throws**

| Type | Condition |
|------|-----------|
| `McpException` | if parsing fails |

