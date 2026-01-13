package com.paragon.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.mcp.dto.JsonRpcRequest;
import com.paragon.mcp.dto.JsonRpcResponse;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MCP client that communicates with a subprocess via stdio.
 *
 * <p>This client launches an MCP server as a subprocess and communicates using stdin/stdout. This
 * is the standard transport for local MCP servers.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
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
 * }</pre>
 *
 * @see McpClient
 */
public final class StdioMcpClient extends McpClient {

  private static final Logger log = LoggerFactory.getLogger(StdioMcpClient.class);

  private final List<String> command;
  private final Map<String, String> environment;
  private final @Nullable Path workingDirectory;

  private @Nullable Process process;
  private @Nullable BufferedWriter stdin;
  private @Nullable BufferedReader stdout;

  private StdioMcpClient(
      @NonNull ObjectMapper objectMapper,
      @NonNull List<String> command,
      @NonNull Map<String, String> environment,
      @Nullable Path workingDirectory) {
    super(objectMapper);
    this.command = List.copyOf(command);
    this.environment = Map.copyOf(environment);
    this.workingDirectory = workingDirectory;
  }

  /**
   * Creates a new builder for StdioMcpClient.
   *
   * @return a new builder
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  @Override
  protected void doConnect() throws McpException {
    log.info("Starting MCP server process: {}", String.join(" ", command));

    try {
      ProcessBuilder pb = new ProcessBuilder(command);

      // Set environment variables
      if (!environment.isEmpty()) {
        pb.environment().putAll(environment);
      }

      // Set working directory if specified
      if (workingDirectory != null) {
        pb.directory(workingDirectory.toFile());
      }

      // Redirect stderr to log
      pb.redirectErrorStream(false);

      process = pb.start();

      stdin =
          new BufferedWriter(
              new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
      stdout =
          new BufferedReader(
              new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

      // Start a thread to log stderr
      Thread stderrLogger =
          Thread.ofVirtual()
              .name("mcp-stderr-logger")
              .start(
                  () -> {
                    try (var reader =
                        new BufferedReader(
                            new InputStreamReader(
                                process.getErrorStream(), StandardCharsets.UTF_8))) {
                      String line;
                      while ((line = reader.readLine()) != null) {
                        log.debug("[MCP Server stderr] {}", line);
                      }
                    } catch (IOException e) {
                      log.trace("MCP stderr reader closed", e);
                    }
                  });

      log.info("MCP server process started successfully");

    } catch (IOException e) {
      throw McpException.connectionFailed("Failed to start MCP server process", e);
    }
  }

  @Override
  protected void sendRequest(@NonNull JsonRpcRequest request) throws McpException {
    if (stdin == null) {
      throw new McpException("Not connected");
    }

    String json = toJson(request);
    log.trace("Sending: {}", json);

    try {
      stdin.write(json);
      stdin.newLine();
      stdin.flush();
    } catch (IOException e) {
      throw McpException.connectionFailed("Failed to send request", e);
    }
  }

  @Override
  protected @NonNull JsonRpcResponse readResponse() throws McpException {
    if (stdout == null) {
      throw new McpException("Not connected");
    }

    try {
      String line = stdout.readLine();
      if (line == null) {
        throw McpException.connectionFailed("Server closed connection", null);
      }

      log.trace("Received: {}", line);
      return parseResponse(line);

    } catch (IOException e) {
      throw McpException.connectionFailed("Failed to read response", e);
    }
  }

  @Override
  protected void doClose() throws McpException {
    log.info("Stopping MCP server process");

    // Close stdin first to signal server to shutdown
    if (stdin != null) {
      try {
        stdin.close();
      } catch (IOException e) {
        log.debug("Error closing stdin", e);
      }
      stdin = null;
    }

    if (process != null) {
      try {
        // Wait for graceful shutdown
        boolean exited = process.waitFor(5, TimeUnit.SECONDS);
        if (!exited) {
          log.debug("Process did not exit gracefully, sending SIGTERM");
          process.destroy();
          exited = process.waitFor(3, TimeUnit.SECONDS);
          if (!exited) {
            log.warn("Process did not respond to SIGTERM, sending SIGKILL");
            process.destroyForcibly();
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        process.destroyForcibly();
      }
      process = null;
    }

    if (stdout != null) {
      try {
        stdout.close();
      } catch (IOException e) {
        log.debug("Error closing stdout", e);
      }
      stdout = null;
    }
  }

  /**
   * Builder for StdioMcpClient.
   */
  public static final class Builder {
    private final List<String> command = new ArrayList<>();
    private final Map<String, String> environment = new HashMap<>();
    private @Nullable Path workingDirectory;
    private @Nullable ObjectMapper objectMapper;

    private Builder() {}

    /**
     * Sets the command to run.
     *
     * @param command the command and arguments
     * @return this builder
     */
    public @NonNull Builder command(@NonNull String... command) {
      this.command.clear();
      this.command.addAll(List.of(command));
      return this;
    }

    /**
     * Sets the command to run.
     *
     * @param command the command and arguments
     * @return this builder
     */
    public @NonNull Builder command(@NonNull List<String> command) {
      this.command.clear();
      this.command.addAll(command);
      return this;
    }

    /**
     * Adds an environment variable.
     *
     * @param name the variable name
     * @param value the variable value
     * @return this builder
     */
    public @NonNull Builder environment(@NonNull String name, @NonNull String value) {
      this.environment.put(name, value);
      return this;
    }

    /**
     * Sets environment variables.
     *
     * @param environment the environment variables
     * @return this builder
     */
    public @NonNull Builder environment(@NonNull Map<String, String> environment) {
      this.environment.clear();
      this.environment.putAll(environment);
      return this;
    }

    /**
     * Sets the working directory for the subprocess.
     *
     * @param directory the working directory
     * @return this builder
     */
    public @NonNull Builder workingDirectory(@NonNull Path directory) {
      this.workingDirectory = directory;
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
     * Builds the StdioMcpClient.
     *
     * @return a new StdioMcpClient
     * @throws IllegalArgumentException if command is empty
     */
    public @NonNull StdioMcpClient build() {
      if (command.isEmpty()) {
        throw new IllegalArgumentException("Command is required");
      }
      ObjectMapper mapper = objectMapper != null ? objectMapper : new ObjectMapper();
      return new StdioMcpClient(mapper, command, environment, workingDirectory);
    }
  }
}
