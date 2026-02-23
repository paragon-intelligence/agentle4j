# Package `com.paragon.mcp`

> This docs was updated at: 2026-02-23

---

## :material-code-braces: Classs

| Name | Description |
|------|-------------|
| [`McpClient`](mcpclient.md) | Abstract base class for MCP client implementations |
| [`McpException`](mcpexception.md) | Exception thrown when MCP operations fail |
| [`McpRemoteTool`](mcpremotetool.md) | A remote MCP tool that extends `FunctionTool` for seamless agent integration |
| [`StdioMcpClient`](stdiomcpclient.md) | MCP client that communicates with a subprocess via stdio |
| [`StreamableHttpMcpClient`](streamablehttpmcpclient.md) | MCP client that communicates over HTTP with optional SSE streaming |

## :material-approximately-equal: Interfaces

| Name | Description |
|------|-------------|
| [`McpHeaderProvider`](mcpheaderprovider.md) | Provides HTTP headers for MCP HTTP transport |
