# :material-database: McpToolDefinition

`com.paragon.mcp.dto.McpToolDefinition` &nbsp;·&nbsp; **Record**

---

An MCP tool definition as returned by the tools/list method.

## Methods

### `of`

```java
public static McpToolDefinition of(
      @NonNull String name, @NonNull Map<String, Object> inputSchema)
```

Creates a tool definition with only required fields.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the tool name |
| `inputSchema` | the input schema |

**Returns**

a new McpToolDefinition

---

### `of`

```java
public static McpToolDefinition of(
      @NonNull String name, @NonNull String description, @NonNull Map<String, Object> inputSchema)
```

Creates a tool definition with description.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the tool name |
| `description` | the tool description |
| `inputSchema` | the input schema |

**Returns**

a new McpToolDefinition

