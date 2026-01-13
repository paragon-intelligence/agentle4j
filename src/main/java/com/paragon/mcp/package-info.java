/**
 * MCP (Model Context Protocol) client support for Agentle4j.
 *
 * <p>This package provides local MCP client implementations for connecting to MCP servers and
 * accessing their tools. Two transport types are supported:
 *
 * <ul>
 *   <li>{@link com.paragon.mcp.StdioMcpClient} - for subprocess-based MCP servers
 *   <li>{@link com.paragon.mcp.StreamableHttpMcpClient} - for HTTP-based servers with SSE support
 * </ul>
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * // Stdio transport
 * try (var mcp = StdioMcpClient.builder()
 *         .command("npx", "-y", "@modelcontextprotocol/server-filesystem", "/tmp")
 *         .build()) {
 *     mcp.connect();
 *
 *     Agent agent = Agent.builder()
 *         .name("FileAssistant")
 *         .tools(mcp.asTools())
 *         .build();
 *
 *     var result = agent.interact("List files in the directory");
 * }
 *
 * // HTTP transport with authentication
 * var mcp = StreamableHttpMcpClient.builder()
 *     .serverUrl("https://mcp.example.com/api")
 *     .headerProvider(McpHeaderProvider.bearer(() -> authService.getToken()))
 *     .build();
 * }</pre>
 *
 * @see com.paragon.mcp.McpClient
 * @see com.paragon.mcp.StdioMcpClient
 * @see com.paragon.mcp.StreamableHttpMcpClient
 */
@org.jspecify.annotations.NullMarked
package com.paragon.mcp;
