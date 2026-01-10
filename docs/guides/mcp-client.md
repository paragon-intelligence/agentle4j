# MCP Client Integration

Agentle4j supports the Model Context Protocol (MCP), enabling agents to call tools from external MCP servers. This allows integration with a rich ecosystem of MCP-compatible tools and servers.

## Overview

MCP is a protocol for connecting AI models to external tools and data sources. Agentle4j provides two transport implementations:

| Transport | Use Case |
|-----------|----------|
| **StdioMcpClient** | Subprocess-based servers (local commands via stdin/stdout) |
| **StreamableHttpMcpClient** | HTTP-based servers with optional SSE streaming |

## Quick Start

### Stdio Transport

Connect to an MCP server that runs as a subprocess:

```java
try (var mcp = StdioMcpClient.builder()
        .command("npx", "-y", "@modelcontextprotocol/server-filesystem", "/tmp")
        .build()) {
    
    mcp.connect();
    
    // Get available tools
    List<McpRemoteTool> tools = mcp.asTools();
    
    // Call a tool directly
    McpToolResult result = mcp.callTool("read_file", Map.of("path", "/tmp/file.txt"));
    System.out.println(result.getTextContent());
}
```

### HTTP Transport

Connect to an HTTP-based MCP server:

```java
var mcp = StreamableHttpMcpClient.builder()
    .serverUrl("https://mcp.example.com/api")
    .headerProvider(McpHeaderProvider.bearer(() -> authService.getToken()))
    .build();

mcp.connect();
var tools = mcp.asTools();
```

## Tool Filtering

Filter which tools are exposed:

```java
// Get only specific tools
var tools = mcp.asTools(Set.of("read_file", "list_directory"));
```

## Header Provider

Inject runtime headers for authentication:

```java
// Static headers
McpHeaderProvider.of(Map.of("X-API-Key", "secret"));

// Bearer token (refreshed per request)
McpHeaderProvider.bearer(() -> authService.getAccessToken());

// Combine providers
var combined = bearerProvider.and(customHeaderProvider);
```

## Using MCP Tools

Each `McpRemoteTool` can be called with a Map or JSON string:

```java
McpRemoteTool tool = mcp.asTools().get(0);

// Call with Map
FunctionToolCallOutput output = tool.call(Map.of("query", "search term"));

// Call with JSON
FunctionToolCallOutput output = tool.callWithJson("{\"query\": \"search term\"}");

// Check result
if (output.output().toString().contains("error")) {
    // Handle error
}
```

## Builder Options

### StdioMcpClient

```java
StdioMcpClient.builder()
    .command("npx", "-y", "@my-mcp-server")  // Required
    .command(List.of("node", "server.js"))   // Alternative: List form
    .environment("API_KEY", "secret")         // Environment variables
    .environment(Map.of("KEY", "value"))      // Bulk environment
    .workingDirectory(Path.of("/app"))        // Working directory
    .objectMapper(customMapper)               // Custom JSON mapper
    .build();
```

### StreamableHttpMcpClient

```java
StreamableHttpMcpClient.builder()
    .serverUrl("https://mcp.example.com/api")  // Required
    .headerProvider(provider)                   // Custom headers
    .httpClient(okHttpClient)                   // Custom OkHttpClient
    .connectTimeout(Duration.ofSeconds(30))     // Connection timeout
    .readTimeout(Duration.ofSeconds(60))        // Read timeout
    .objectMapper(customMapper)                 // Custom JSON mapper
    .build();
```

## Error Handling

MCP operations throw `McpException`:

```java
try {
    mcp.connect();
    mcp.callTool("my_tool", Map.of());
} catch (McpException e) {
    System.err.println("MCP Error: " + e.getMessage());
    
    // JSON-RPC error code if applicable
    Integer code = e.getErrorCode();
    if (code != null) {
        System.err.println("Error code: " + code);
    }
}
```

## Lifecycle

MCP clients implement `AutoCloseable`:

```java
try (var mcp = StdioMcpClient.builder().command("...").build()) {
    mcp.connect();  // Initialize connection
    // Use the client...
}  // Automatically closed
```

The lifecycle is:
1. **Build** - Create client with builder
2. **Connect** - Call `connect()` to initialize
3. **Use** - Call `listTools()`, `callTool()`, `asTools()`
4. **Close** - Client is closed (automatically with try-with-resources)

## Comparison with McpTool

> [!NOTE]
> Agentle4j has two MCP-related features:
> - **`McpTool`** (in `com.paragon.responses.spec`) - For OpenAI's hosted MCP proxy feature
> - **`McpClient`** (in `com.paragon.mcp`) - For local/custom MCP server connections

This guide covers `McpClient` for connecting to your own MCP servers.
