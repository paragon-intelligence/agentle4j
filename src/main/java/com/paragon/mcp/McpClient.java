package com.paragon.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.mcp.dto.JsonRpcError;
import com.paragon.mcp.dto.JsonRpcRequest;
import com.paragon.mcp.dto.JsonRpcResponse;
import com.paragon.mcp.dto.McpToolDefinition;
import com.paragon.mcp.dto.McpToolResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for MCP client implementations.
 *
 * <p>This class handles the MCP protocol lifecycle and provides common functionality for both Stdio
 * and HTTP transports.
 *
 * <h2>Lifecycle</h2>
 *
 * <ol>
 *   <li>{@link #connect()} - Establishes connection and performs initialization handshake
 *   <li>{@link #listTools()} / {@link #callTool(String, Map)} - Perform operations
 *   <li>{@link #close()} - Closes connection and cleans up resources
 * </ol>
 *
 * @see StdioMcpClient
 * @see StreamableHttpMcpClient
 */
public abstract class McpClient implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(McpClient.class);

  protected static final String PROTOCOL_VERSION = "2025-11-25";
  protected static final String CLIENT_NAME = "Agentle4j";
  protected static final String CLIENT_VERSION = "0.5.0";

  protected final ObjectMapper objectMapper;
  private final AtomicInteger requestIdCounter = new AtomicInteger(1);
  protected volatile boolean initialized = false;
  protected volatile boolean closed = false;

  // Server information from initialization
  protected @Nullable String serverName;
  protected @Nullable String serverVersion;
  protected @Nullable String serverProtocolVersion;

  /**
   * Creates a new MCP client.
   *
   * @param objectMapper the object mapper for JSON serialization
   */
  protected McpClient(@NonNull ObjectMapper objectMapper) {
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
  }

  // ===== Abstract methods for transport-specific implementation =====

  /**
   * Establishes the underlying transport connection.
   *
   * @throws McpException if connection fails
   */
  protected abstract void doConnect() throws McpException;

  /**
   * Sends a JSON-RPC request over the transport.
   *
   * @param request the request to send
   * @throws McpException if sending fails
   */
  protected abstract void sendRequest(@NonNull JsonRpcRequest request) throws McpException;

  /**
   * Reads a JSON-RPC response from the transport.
   *
   * @return the response
   * @throws McpException if reading fails
   */
  protected abstract @NonNull JsonRpcResponse readResponse() throws McpException;

  /**
   * Closes the underlying transport connection.
   *
   * @throws McpException if closing fails
   */
  protected abstract void doClose() throws McpException;

  // ===== Public API =====

  /**
   * Connects to the MCP server and performs initialization.
   *
   * <p>This method establishes the transport connection, sends the initialize request, and sends
   * the initialized notification.
   *
   * @throws McpException if connection or initialization fails
   */
  public void connect() throws McpException {
    if (closed) {
      throw new McpException("Client has been closed");
    }
    if (initialized) {
      log.debug("Already initialized, skipping connection");
      return;
    }

    log.info("Connecting to MCP server");
    doConnect();
    initialize();
  }

  /**
   * Lists all tools available on the MCP server.
   *
   * @return the list of tool definitions
   * @throws McpException if listing fails or client not initialized
   */
  public @NonNull List<McpToolDefinition> listTools() throws McpException {
    ensureInitialized();

    log.debug("Listing tools");
    var request = JsonRpcRequest.create(nextRequestId(), "tools/list", null);
    sendRequest(request);

    JsonRpcResponse response = readResponse();
    checkForError(response);

    return parseToolsList(response.result());
  }

  /**
   * Calls a tool on the MCP server.
   *
   * @param name the tool name
   * @param arguments the tool arguments
   * @return the tool result
   * @throws McpException if the call fails or client not initialized
   */
  public @NonNull McpToolResult callTool(
      @NonNull String name, @Nullable Map<String, Object> arguments) throws McpException {
    Objects.requireNonNull(name, "name cannot be null");
    ensureInitialized();

    log.debug("Calling tool: {} with arguments: {}", name, arguments);

    Map<String, Object> params =
        Map.of("name", name, "arguments", arguments != null ? arguments : Map.of());

    var request = JsonRpcRequest.create(nextRequestId(), "tools/call", params);
    sendRequest(request);

    JsonRpcResponse response = readResponse();
    checkForError(response);

    return parseToolResult(response.result());
  }

  /**
   * Returns all MCP tools as McpRemoteTool instances that can be used with agents.
   *
   * @return list of MCP remote tools
   * @throws McpException if listing fails
   */
  public @NonNull List<McpRemoteTool> asTools() throws McpException {
    return asTools(null);
  }

  /**
   * Returns filtered MCP tools as McpRemoteTool instances.
   *
   * @param allowedToolNames optional set of tool names to include (null for all)
   * @return list of MCP remote tools
   * @throws McpException if listing fails
   */
  public @NonNull List<McpRemoteTool> asTools(@Nullable Set<String> allowedToolNames)
      throws McpException {
    List<McpToolDefinition> definitions = listTools();
    List<McpRemoteTool> tools = new ArrayList<>();

    for (McpToolDefinition def : definitions) {
      if (allowedToolNames == null || allowedToolNames.contains(def.name())) {
        tools.add(new McpRemoteTool(this, def, objectMapper));
      }
    }

    log.info(
        "Created {} tools from MCP server (filtered: {})", tools.size(), allowedToolNames != null);
    return tools;
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;
    log.info("Closing MCP client");
    try {
      doClose();
    } catch (Exception e) {
      log.warn("Error closing MCP client", e);
    }
  }

  // ===== Initialization =====

  private void initialize() throws McpException {
    log.debug("Sending initialize request");

    Map<String, Object> params =
        Map.of(
            "protocolVersion",
            PROTOCOL_VERSION,
            "capabilities",
            Map.of(),
            "clientInfo",
            Map.of("name", CLIENT_NAME, "version", CLIENT_VERSION));

    var request = JsonRpcRequest.create(nextRequestId(), "initialize", params);
    sendRequest(request);

    JsonRpcResponse response = readResponse();
    checkForError(response);

    parseInitializeResult(response.result());

    // Send initialized notification
    log.debug("Sending initialized notification");
    var notification = JsonRpcRequest.create(nextRequestId(), "notifications/initialized", null);
    sendRequest(notification);

    initialized = true;
    log.info(
        "MCP initialization complete. Server: {} v{}, Protocol: {}",
        serverName,
        serverVersion,
        serverProtocolVersion);
  }

  // ===== Helper methods =====

  protected int nextRequestId() {
    return requestIdCounter.getAndIncrement();
  }

  protected void ensureInitialized() throws McpException {
    if (closed) {
      throw new McpException("Client has been closed");
    }
    if (!initialized) {
      throw new McpException("Client not initialized. Call connect() first.");
    }
  }

  protected void checkForError(@NonNull JsonRpcResponse response) throws McpException {
    if (response.isError()) {
      JsonRpcError error = response.error();
      throw McpException.fromJsonRpcError(error.code(), error.message());
    }
  }

  private void parseInitializeResult(@Nullable Object result) throws McpException {
    if (result == null) {
      throw McpException.protocolError("Initialize response is empty");
    }

    try {
      JsonNode node = objectMapper.valueToTree(result);
      serverProtocolVersion =
          node.has("protocolVersion") ? node.get("protocolVersion").asText() : null;

      if (node.has("serverInfo")) {
        JsonNode serverInfo = node.get("serverInfo");
        serverName = serverInfo.has("name") ? serverInfo.get("name").asText() : "unknown";
        serverVersion = serverInfo.has("version") ? serverInfo.get("version").asText() : "unknown";
      }
    } catch (Exception e) {
      throw McpException.protocolError("Failed to parse initialize result: " + e.getMessage());
    }
  }

  private @NonNull List<McpToolDefinition> parseToolsList(@Nullable Object result)
      throws McpException {
    if (result == null) {
      return List.of();
    }

    try {
      JsonNode node = objectMapper.valueToTree(result);
      if (!node.has("tools")) {
        return List.of();
      }

      JsonNode toolsNode = node.get("tools");
      return objectMapper.convertValue(toolsNode, new TypeReference<List<McpToolDefinition>>() {});
    } catch (Exception e) {
      throw McpException.protocolError("Failed to parse tools list: " + e.getMessage());
    }
  }

  private @NonNull McpToolResult parseToolResult(@Nullable Object result) throws McpException {
    if (result == null) {
      throw McpException.protocolError("Tool result is empty");
    }

    try {
      return objectMapper.convertValue(result, McpToolResult.class);
    } catch (Exception e) {
      throw McpException.protocolError("Failed to parse tool result: " + e.getMessage());
    }
  }

  /**
   * Serializes an object to JSON string.
   *
   * @param obj the object to serialize
   * @return the JSON string
   * @throws McpException if serialization fails
   */
  protected @NonNull String toJson(@NonNull Object obj) throws McpException {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw McpException.protocolError("Failed to serialize to JSON: " + e.getMessage());
    }
  }

  /**
   * Deserializes a JSON string to JsonRpcResponse.
   *
   * @param json the JSON string
   * @return the parsed response
   * @throws McpException if parsing fails
   */
  protected @NonNull JsonRpcResponse parseResponse(@NonNull String json) throws McpException {
    try {
      return objectMapper.readValue(json, JsonRpcResponse.class);
    } catch (JsonProcessingException e) {
      throw McpException.protocolError("Failed to parse JSON response: " + e.getMessage());
    }
  }
}
