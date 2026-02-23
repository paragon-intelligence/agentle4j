# :material-approximately-equal: McpHeaderProvider

> This docs was updated at: 2026-02-23

`com.paragon.mcp.McpHeaderProvider` &nbsp;·&nbsp; **Interface**

---

Provides HTTP headers for MCP HTTP transport.

This functional interface allows headers to be computed at runtime, which is useful for:

  
- Authentication tokens that expire and need refresh
- User context headers that change per request
- Dynamic configuration based on runtime state

### Example Usage

```java
// Static headers
var provider = McpHeaderProvider.of(Map.of("X-API-Key", "secret"));
// Bearer token with refresh
var provider = McpHeaderProvider.bearer(() -> authService.getAccessToken());
// Custom runtime headers
McpHeaderProvider provider = () -> Map.of(
    "Authorization", "Bearer " + getToken(),
    "X-User-Id", getCurrentUserId()
);
```

## Methods

### `getHeaders`

```java
Map<String, String> getHeaders()
```

Returns the headers to include in MCP HTTP requests.

**Returns**

a map of header names to values (never null, may be empty)

---

### `of`

```java
static McpHeaderProvider of(@NonNull Map<String, String> headers)
```

Creates a header provider with static headers.

**Parameters**

| Name | Description |
|------|-------------|
| `headers` | the headers to provide |

**Returns**

a new header provider

---

### `bearer`

```java
static McpHeaderProvider bearer(@NonNull Supplier<String> tokenSupplier)
```

Creates a header provider for Bearer token authentication.

**Parameters**

| Name | Description |
|------|-------------|
| `tokenSupplier` | supplies the bearer token (called on each request) |

**Returns**

a new header provider

---

### `empty`

```java
static McpHeaderProvider empty()
```

Creates an empty header provider (no headers).

**Returns**

a header provider that returns an empty map

---

### `and`

```java
default McpHeaderProvider and(@NonNull McpHeaderProvider other)
```

Combines this header provider with another, merging their headers.

**Parameters**

| Name | Description |
|------|-------------|
| `other` | the other header provider |

**Returns**

a new header provider that combines both

