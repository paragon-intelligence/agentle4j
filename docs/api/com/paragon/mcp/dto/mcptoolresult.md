# :material-database: McpToolResult

`com.paragon.mcp.dto.McpToolResult` &nbsp;·&nbsp; **Record**

---

The result of an MCP tool call.

## Methods

### `text`

```java
public static McpToolResult text(@NonNull String text)
```

Creates a successful text result.

**Parameters**

| Name | Description |
|------|-------------|
| `text` | the text content |

**Returns**

a new McpToolResult

---

### `error`

```java
public static McpToolResult error(@NonNull String errorMessage)
```

Creates an error result.

**Parameters**

| Name | Description |
|------|-------------|
| `errorMessage` | the error message |

**Returns**

a new McpToolResult

---

### `getTextContent`

```java
public @NonNull String getTextContent()
```

Returns the text content concatenated, or empty string if no text content.

**Returns**

the text content

