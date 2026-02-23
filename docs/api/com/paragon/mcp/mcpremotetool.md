# :material-code-braces: McpRemoteTool

> This docs was updated at: 2026-02-23

`com.paragon.mcp.McpRemoteTool` &nbsp;·&nbsp; **Class**

Extends `FunctionTool<McpRemoteTool.McpParams>`

---

A remote MCP tool that extends `FunctionTool` for seamless agent integration.

This class wraps an MCP tool definition and client, allowing MCP tools to be used directly
with agents via `agent.builder().addTool(mcpRemoteTool)`.

### Usage with Agents

```java
// Get tools from MCP server
var mcp = StdioMcpClient.builder()
    .command("npx", "-y", "@modelcontextprotocol/server-filesystem")
    .build();
mcp.connect();
List tools = mcp.asTools();
// Add to agent
Agent agent = Agent.builder()
    .name("FileAgent")
    .addTools(tools)  // Works because McpRemoteTool extends FunctionTool
    .build();
```

**See Also**

- `McpClient#asTools()`
- `FunctionTool`

## Methods

### `McpRemoteTool`

```java
public McpRemoteTool(
      @NonNull McpClient client,
      @NonNull McpToolDefinition definition,
      @NonNull ObjectMapper objectMapper)
```

Creates a new remote MCP tool.

**Parameters**

| Name | Description |
|------|-------------|
| `client` | the MCP client to use for calling the tool |
| `definition` | the tool definition from the MCP server |
| `objectMapper` | the object mapper for JSON conversion |

---

### `call`

```java
public @Nullable FunctionToolCallOutput call(@Nullable McpParams params)
```

Calls the MCP tool.

Note: The params argument is not used because MCP tools have dynamic schemas. The actual
arguments are extracted from the raw JSON provided by the LLM during execution.

---

### `callWithRawArguments`

```java
public @NonNull FunctionToolCallOutput callWithRawArguments(@Nullable String jsonArguments)
```

Calls the MCP tool with raw JSON arguments from the LLM.

**Parameters**

| Name | Description |
|------|-------------|
| `jsonArguments` | the arguments as a JSON string |

**Returns**

the tool call output

---

### `callWithMap`

```java
public @NonNull FunctionToolCallOutput callWithMap(@Nullable Map<String, Object> arguments)
```

Calls the MCP tool with a map of arguments.

**Parameters**

| Name | Description |
|------|-------------|
| `arguments` | the arguments as a map |

**Returns**

the tool call output

---

### `getDefinition`

```java
public @NonNull McpToolDefinition getDefinition()
```

Returns the MCP tool definition.

**Returns**

the tool definition

---

### `getInputSchema`

```java
public @NonNull Map<String, Object> getInputSchema()
```

Returns the input schema for this tool.

**Returns**

the input schema as a map

