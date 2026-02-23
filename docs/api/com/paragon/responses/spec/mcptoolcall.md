# :material-code-braces: McpToolCall

> This docs was updated at: 2026-02-23

`com.paragon.responses.spec.McpToolCall` &nbsp;·&nbsp; **Class**

Extends `ToolCall` &nbsp;·&nbsp; Implements `Item`, `ResponseOutput`

---

An invocation of a tool on an MCP server.

## Methods

### `McpToolCall`

```java
public McpToolCall(
      @NonNull String arguments,
      @NonNull String id,
      @NonNull String name,
      @NonNull String serverLabel,
      @Nullable String approvalRequestId,
      @Nullable String error,
      @Nullable String output,
      @Nullable McpToolCallStatus status)
```

@param arguments A JSON string of the arguments passed to the tool.

**Parameters**

| Name | Description |
|------|-------------|
| `id` | The unique ID of the tool call. |
| `name` | The name of the tool that was run. |
| `serverLabel` | The label of the MCP server running the tool. |
| `approvalRequestId` | Unique identifier for the MCP tool call approval request. Include this value in a subsequent mcp_approval_response input to approve or reject the corresponding     tool call. |
| `error` | The error from the tool call, if any. |
| `output` | The output from the tool call. |
| `status` | The status of the tool call. One of `in_progress`, `completed`, `incomplete`, `calling`, or `failed`. |

