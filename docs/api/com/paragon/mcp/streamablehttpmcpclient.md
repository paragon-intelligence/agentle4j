# :material-code-braces: StreamableHttpMcpClient

> This docs was updated at: 2026-02-23

`com.paragon.mcp.StreamableHttpMcpClient` &nbsp;·&nbsp; **Class**

Extends `McpClient`

---

MCP client that communicates over HTTP with optional SSE streaming.

This client implements the Streamable HTTP transport as defined in the MCP specification. It
supports:

  
- JSON responses for simple request-response patterns
- SSE streams for streaming responses
- Session management via MCP-Session-Id header
- Custom headers for authentication

### Example Usage

```java
var mcp = StreamableHttpMcpClient.builder()
    .serverUrl("https://mcp.example.com/api")
    .headerProvider(McpHeaderProvider.bearer(() -> authService.getToken()))
    .build();
mcp.connect();
var tools = mcp.asTools();
```

**See Also**

- `McpClient`
- `McpHeaderProvider`

## Methods

### `builder`

```java
public static @NonNull Builder builder()
```

Creates a new builder for StreamableHttpMcpClient.

**Returns**

a new builder

---

### `connect`

```java
public void connect() throws McpException
```

Performs initialization handshake.

---

### `handleSseResponse`

```java
private @NonNull JsonRpcResponse handleSseResponse(@NonNull Response httpResponse)
      throws McpException
```

Parses SSE stream manually without external dependency.

SSE format: - Lines starting with "data:" contain the payload - Lines starting with "event:"
contain the event type - Lines starting with "id:" contain the event ID - Empty lines separate
events

---

### `getSessionId`

```java
public @Nullable String getSessionId()
```

Returns the current session ID.

**Returns**

the session ID, or null if not established

---

### `serverUrl`

```java
public @NonNull Builder serverUrl(@NonNull String serverUrl)
```

Sets the MCP server URL.

**Parameters**

| Name | Description |
|------|-------------|
| `serverUrl` | the server URL |

**Returns**

this builder

---

### `httpClient`

```java
public @NonNull Builder httpClient(@NonNull OkHttpClient httpClient)
```

Sets the OkHttpClient to use.

**Parameters**

| Name | Description |
|------|-------------|
| `httpClient` | the HTTP client |

**Returns**

this builder

---

### `headerProvider`

```java
public @NonNull Builder headerProvider(@NonNull McpHeaderProvider headerProvider)
```

Sets the header provider for custom headers.

**Parameters**

| Name | Description |
|------|-------------|
| `headerProvider` | the header provider |

**Returns**

this builder

---

### `connectTimeout`

```java
public @NonNull Builder connectTimeout(@NonNull Duration timeout)
```

Sets the connection timeout.

**Parameters**

| Name | Description |
|------|-------------|
| `timeout` | the timeout duration |

**Returns**

this builder

---

### `readTimeout`

```java
public @NonNull Builder readTimeout(@NonNull Duration timeout)
```

Sets the read timeout.

**Parameters**

| Name | Description |
|------|-------------|
| `timeout` | the timeout duration |

**Returns**

this builder

---

### `objectMapper`

```java
public @NonNull Builder objectMapper(@NonNull ObjectMapper objectMapper)
```

Sets the ObjectMapper for JSON serialization.

**Parameters**

| Name | Description |
|------|-------------|
| `objectMapper` | the object mapper |

**Returns**

this builder

---

### `build`

```java
public @NonNull StreamableHttpMcpClient build()
```

Builds the StreamableHttpMcpClient.

**Returns**

a new StreamableHttpMcpClient

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if serverUrl is not set |

