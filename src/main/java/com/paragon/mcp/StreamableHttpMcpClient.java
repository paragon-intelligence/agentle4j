package com.paragon.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.mcp.dto.JsonRpcRequest;
import com.paragon.mcp.dto.JsonRpcResponse;
import com.paragon.mcp.dto.McpToolDefinition;
import com.paragon.mcp.dto.McpToolResult;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MCP client that communicates over HTTP with optional SSE streaming.
 *
 * <p>This client implements the Streamable HTTP transport as defined in the MCP specification. It
 * supports:
 *
 * <ul>
 *   <li>JSON responses for simple request-response patterns
 *   <li>SSE streams for streaming responses
 *   <li>Session management via MCP-Session-Id header
 *   <li>Custom headers for authentication
 * </ul>
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var mcp = StreamableHttpMcpClient.builder()
 *     .serverUrl("https://mcp.example.com/api")
 *     .headerProvider(McpHeaderProvider.bearer(() -> authService.getToken()))
 *     .build();
 *
 * mcp.connect();
 * var tools = mcp.asTools();
 * }</pre>
 *
 * @see McpClient
 * @see McpHeaderProvider
 */
public final class StreamableHttpMcpClient extends McpClient {

  private static final Logger log = LoggerFactory.getLogger(StreamableHttpMcpClient.class);
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

  private final String serverUrl;
  private final OkHttpClient httpClient;
  private final McpHeaderProvider headerProvider;

  private @Nullable String sessionId;

  private StreamableHttpMcpClient(
      @NonNull ObjectMapper objectMapper,
      @NonNull String serverUrl,
      @NonNull OkHttpClient httpClient,
      @NonNull McpHeaderProvider headerProvider) {
    super(objectMapper);
    this.serverUrl = serverUrl;
    this.httpClient = httpClient;
    this.headerProvider = headerProvider;
  }

  /**
   * Creates a new builder for StreamableHttpMcpClient.
   *
   * @return a new builder
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  @Override
  protected void doConnect() throws McpException {
    log.info("Connecting to MCP server at: {}", serverUrl);
    // HTTP transport doesn't require explicit connection
  }

  @Override
  protected void sendRequest(@NonNull JsonRpcRequest request) throws McpException {
    // For HTTP transport, sendRequest is handled within the synchronous call
  }

  @Override
  protected @NonNull JsonRpcResponse readResponse() throws McpException {
    throw new McpException("HTTP transport uses synchronous request-response");
  }

  /**
   * Performs initialization handshake.
   */
  @Override
  public void connect() throws McpException {
    if (closed) {
      throw new McpException("Client has been closed");
    }
    if (initialized) {
      log.debug("Already initialized, skipping connection");
      return;
    }

    log.info("Connecting to MCP server at: {}", serverUrl);
    performInitialization();
  }

  private void performInitialization() throws McpException {
    log.debug("Sending initialize request");

    var initParams =
        java.util.Map.of(
            "protocolVersion",
            PROTOCOL_VERSION,
            "capabilities",
            java.util.Map.of(),
            "clientInfo",
            java.util.Map.of("name", CLIENT_NAME, "version", CLIENT_VERSION));

    var initRequest = JsonRpcRequest.create(nextRequestId(), "initialize", initParams);
    JsonRpcResponse initResponse = sendRequestAndGetResponse(initRequest);
    checkForError(initResponse);
    parseInitResult(initResponse);

    // Send initialized notification
    log.debug("Sending initialized notification");
    var notificationRequest =
        JsonRpcRequest.create(nextRequestId(), "notifications/initialized", null);
    sendNotification(notificationRequest);

    initialized = true;
    log.info(
        "MCP initialization complete. Server: {} v{}, Protocol: {}",
        serverName,
        serverVersion,
        serverProtocolVersion);
  }

  private void parseInitResult(JsonRpcResponse response) throws McpException {
    if (response.result() == null) {
      throw McpException.protocolError("Initialize response is empty");
    }

    try {
      var node = objectMapper.valueToTree(response.result());
      serverProtocolVersion =
          node.has("protocolVersion") ? node.get("protocolVersion").asText() : null;

      if (node.has("serverInfo")) {
        var serverInfo = node.get("serverInfo");
        serverName = serverInfo.has("name") ? serverInfo.get("name").asText() : "unknown";
        serverVersion = serverInfo.has("version") ? serverInfo.get("version").asText() : "unknown";
      }
    } catch (Exception e) {
      throw McpException.protocolError("Failed to parse initialize result: " + e.getMessage());
    }
  }

  @Override
  public @NonNull List<McpToolDefinition> listTools() throws McpException {
    ensureInitialized();

    log.debug("Listing tools");
    var request = JsonRpcRequest.create(nextRequestId(), "tools/list", null);
    JsonRpcResponse response = sendRequestAndGetResponse(request);
    checkForError(response);

    return parseToolsListResult(response.result());
  }

  @Override
  public @NonNull McpToolResult callTool(
      @NonNull String name, @Nullable Map<String, Object> arguments) throws McpException {
    Objects.requireNonNull(name, "name cannot be null");
    ensureInitialized();

    log.debug("Calling tool: {} with arguments: {}", name, arguments);

    var params = Map.of("name", name, "arguments", arguments != null ? arguments : Map.of());

    var request = JsonRpcRequest.create(nextRequestId(), "tools/call", params);
    JsonRpcResponse response = sendRequestAndGetResponse(request);
    checkForError(response);

    return parseToolResultFromResponse(response.result());
  }

  private @NonNull List<McpToolDefinition> parseToolsListResult(
      @Nullable Object result) throws McpException {
    if (result == null) {
      return List.of();
    }

    try {
      var node = objectMapper.valueToTree(result);
      if (!node.has("tools")) {
        return List.of();
      }

      var toolsNode = node.get("tools");
      return objectMapper.convertValue(
          toolsNode,
          new com.fasterxml.jackson.core.type.TypeReference<List<McpToolDefinition>>() {});
    } catch (Exception e) {
      throw McpException.protocolError("Failed to parse tools list: " + e.getMessage());
    }
  }

  private @NonNull McpToolResult parseToolResultFromResponse(
      @Nullable Object result) throws McpException {
    if (result == null) {
      throw McpException.protocolError("Tool result is empty");
    }

    try {
      return objectMapper.convertValue(result, McpToolResult.class);
    } catch (Exception e) {
      throw McpException.protocolError("Failed to parse tool result: " + e.getMessage());
    }
  }

  private @NonNull JsonRpcResponse sendRequestAndGetResponse(@NonNull JsonRpcRequest request)
      throws McpException {
    String json = toJson(request);
    log.trace("Sending HTTP request: {}", json);

    Request.Builder httpRequestBuilder =
        new Request.Builder()
            .url(serverUrl)
            .post(RequestBody.create(json, JSON))
            .header("Accept", "application/json, text/event-stream")
            .header("MCP-Protocol-Version", PROTOCOL_VERSION);

    // Add session ID if available
    if (sessionId != null) {
      httpRequestBuilder.header("MCP-Session-Id", sessionId);
    }

    // Add custom headers
    headerProvider.getHeaders().forEach(httpRequestBuilder::header);

    Request httpRequest = httpRequestBuilder.build();

    try (Response httpResponse = httpClient.newCall(httpRequest).execute()) {
      // Extract session ID from response if present
      String newSessionId = httpResponse.header("MCP-Session-Id");
      if (newSessionId != null && !newSessionId.isEmpty()) {
        sessionId = newSessionId;
        log.debug("Received session ID: {}", sessionId);
      }

      // Check HTTP status
      if (!httpResponse.isSuccessful()) {
        String body = httpResponse.body() != null ? httpResponse.body().string() : "";
        throw new McpException(
            "HTTP error " + httpResponse.code() + ": " + httpResponse.message() + " - " + body);
      }

      String contentType = httpResponse.header("Content-Type");

      if (contentType != null && contentType.contains("text/event-stream")) {
        // Handle SSE response
        return handleSseResponse(httpResponse);
      } else {
        // Handle JSON response
        String responseBody = httpResponse.body() != null ? httpResponse.body().string() : "";
        log.trace("Received HTTP response: {}", responseBody);
        return parseResponse(responseBody);
      }
    } catch (McpException e) {
      throw e;
    } catch (IOException e) {
      throw McpException.connectionFailed("HTTP request failed", e);
    }
  }

  private void sendNotification(@NonNull JsonRpcRequest notification) throws McpException {
    String json = toJson(notification);
    log.trace("Sending notification: {}", json);

    Request.Builder httpRequestBuilder =
        new Request.Builder()
            .url(serverUrl)
            .post(RequestBody.create(json, JSON))
            .header("Accept", "application/json")
            .header("MCP-Protocol-Version", PROTOCOL_VERSION);

    if (sessionId != null) {
      httpRequestBuilder.header("MCP-Session-Id", sessionId);
    }

    headerProvider.getHeaders().forEach(httpRequestBuilder::header);

    Request httpRequest = httpRequestBuilder.build();

    try (Response httpResponse = httpClient.newCall(httpRequest).execute()) {
      // Extract session ID if present
      String newSessionId = httpResponse.header("MCP-Session-Id");
      if (newSessionId != null && !newSessionId.isEmpty()) {
        sessionId = newSessionId;
      }

      // 202 Accepted is expected for notifications
      if (httpResponse.code() != 202 && !httpResponse.isSuccessful()) {
        String body = httpResponse.body() != null ? httpResponse.body().string() : "";
        log.warn("Notification returned unexpected status {}: {}", httpResponse.code(), body);
      }
    } catch (IOException e) {
      throw McpException.connectionFailed("Failed to send notification", e);
    }
  }

  /**
   * Parses SSE stream manually without external dependency.
   * 
   * SSE format:
   * - Lines starting with "data:" contain the payload
   * - Lines starting with "event:" contain the event type
   * - Lines starting with "id:" contain the event ID
   * - Empty lines separate events
   */
  private @NonNull JsonRpcResponse handleSseResponse(@NonNull Response httpResponse)
      throws McpException {
    log.debug("Handling SSE response");

    ResponseBody body = httpResponse.body();
    if (body == null) {
      throw McpException.protocolError("SSE response body is null");
    }

    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(body.byteStream(), StandardCharsets.UTF_8))) {

      StringBuilder dataBuilder = new StringBuilder();
      String line;

      while ((line = reader.readLine()) != null) {
        log.trace("SSE line: {}", line);

        if (line.isEmpty()) {
          // Empty line marks end of an event
          if (dataBuilder.length() > 0) {
            String data = dataBuilder.toString().trim();
            if (!data.isEmpty()) {
              log.trace("SSE event data: {}", data);
              return parseResponse(data);
            }
            dataBuilder.setLength(0);
          }
        } else if (line.startsWith("data:")) {
          // Extract data after "data:" prefix
          String data = line.substring(5).trim();
          if (!data.isEmpty()) {
            if (dataBuilder.length() > 0) {
              dataBuilder.append("\n");
            }
            dataBuilder.append(data);
          }
        } else if (line.startsWith("id:") || line.startsWith("event:") || line.startsWith("retry:")) {
          // Ignore these for now - we only care about data
          log.trace("SSE metadata: {}", line);
        }
      }

      // Check if we have remaining data
      if (dataBuilder.length() > 0) {
        String data = dataBuilder.toString().trim();
        if (!data.isEmpty()) {
          return parseResponse(data);
        }
      }

      throw McpException.protocolError("No JSON-RPC response found in SSE stream");

    } catch (IOException e) {
      throw McpException.connectionFailed("Failed to read SSE response", e);
    }
  }

  @Override
  protected void doClose() throws McpException {
    log.info("Closing HTTP MCP client");

    // Send DELETE request to terminate session if we have a session ID
    if (sessionId != null) {
      try {
        Request.Builder httpRequestBuilder =
            new Request.Builder()
                .url(serverUrl)
                .delete()
                .header("MCP-Session-Id", sessionId)
                .header("MCP-Protocol-Version", PROTOCOL_VERSION);

        headerProvider.getHeaders().forEach(httpRequestBuilder::header);

        try (Response response = httpClient.newCall(httpRequestBuilder.build()).execute()) {
          if (response.code() == 405) {
            log.debug("Server does not support session termination (405)");
          } else if (!response.isSuccessful()) {
            log.debug("Session termination returned {}", response.code());
          } else {
            log.debug("Session terminated successfully");
          }
        }
      } catch (IOException e) {
        log.debug("Error terminating session", e);
      }
    }
  }

  /**
   * Returns the current session ID.
   *
   * @return the session ID, or null if not established
   */
  public @Nullable String getSessionId() {
    return sessionId;
  }

  /**
   * Builder for StreamableHttpMcpClient.
   */
  public static final class Builder {
    private @Nullable String serverUrl;
    private @Nullable OkHttpClient httpClient;
    private @Nullable McpHeaderProvider headerProvider;
    private @Nullable ObjectMapper objectMapper;
    private Duration connectTimeout = Duration.ofSeconds(30);
    private Duration readTimeout = Duration.ofSeconds(60);

    private Builder() {}

    /**
     * Sets the MCP server URL.
     *
     * @param serverUrl the server URL
     * @return this builder
     */
    public @NonNull Builder serverUrl(@NonNull String serverUrl) {
      this.serverUrl = Objects.requireNonNull(serverUrl, "serverUrl cannot be null");
      return this;
    }

    /**
     * Sets the OkHttpClient to use.
     *
     * @param httpClient the HTTP client
     * @return this builder
     */
    public @NonNull Builder httpClient(@NonNull OkHttpClient httpClient) {
      this.httpClient = httpClient;
      return this;
    }

    /**
     * Sets the header provider for custom headers.
     *
     * @param headerProvider the header provider
     * @return this builder
     */
    public @NonNull Builder headerProvider(@NonNull McpHeaderProvider headerProvider) {
      this.headerProvider = headerProvider;
      return this;
    }

    /**
     * Sets the connection timeout.
     *
     * @param timeout the timeout duration
     * @return this builder
     */
    public @NonNull Builder connectTimeout(@NonNull Duration timeout) {
      this.connectTimeout = timeout;
      return this;
    }

    /**
     * Sets the read timeout.
     *
     * @param timeout the timeout duration
     * @return this builder
     */
    public @NonNull Builder readTimeout(@NonNull Duration timeout) {
      this.readTimeout = timeout;
      return this;
    }

    /**
     * Sets the ObjectMapper for JSON serialization.
     *
     * @param objectMapper the object mapper
     * @return this builder
     */
    public @NonNull Builder objectMapper(@NonNull ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
      return this;
    }

    /**
     * Builds the StreamableHttpMcpClient.
     *
     * @return a new StreamableHttpMcpClient
     * @throws IllegalArgumentException if serverUrl is not set
     */
    public @NonNull StreamableHttpMcpClient build() {
      if (serverUrl == null || serverUrl.isEmpty()) {
        throw new IllegalArgumentException("serverUrl is required");
      }

      OkHttpClient client =
          httpClient != null
              ? httpClient
              : new OkHttpClient.Builder()
                  .connectTimeout(connectTimeout)
                  .readTimeout(readTimeout)
                  .build();

      McpHeaderProvider headers =
          headerProvider != null ? headerProvider : McpHeaderProvider.empty();

      ObjectMapper mapper = objectMapper != null ? objectMapper : new ObjectMapper();

      return new StreamableHttpMcpClient(mapper, serverUrl, client, headers);
    }
  }
}
