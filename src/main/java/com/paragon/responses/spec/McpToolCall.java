package com.paragon.responses.spec;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** An invocation of a tool on an MCP server. */
public final class McpToolCall extends ToolCall implements Item, ResponseOutput {
  private final @NonNull String arguments;
  private final @NonNull String name;
  private final @NonNull String serverLabel;
  private final @Nullable String approvalRequestId;
  private final @Nullable String error;
  private final @Nullable String output;
  private final @Nullable McpToolCallStatus status;

  /**
   * @param arguments A JSON string of the arguments passed to the tool.
   * @param id The unique ID of the tool call.
   * @param name The name of the tool that was run.
   * @param serverLabel The label of the MCP server running the tool.
   * @param approvalRequestId Unique identifier for the MCP tool call approval request. Include this
   *     value in a subsequent mcp_approval_response input to approve or reject the corresponding
   *     tool call.
   * @param error The error from the tool call, if any.
   * @param output The output from the tool call.
   * @param status The status of the tool call. One of {@code in_progress}, {@code completed},
   *     {@code incomplete}, {@code calling}, or {@code failed}.
   */
  public McpToolCall(
      @NonNull String arguments,
      @NonNull String id,
      @NonNull String name,
      @NonNull String serverLabel,
      @Nullable String approvalRequestId,
      @Nullable String error,
      @Nullable String output,
      @Nullable McpToolCallStatus status) {
    super(id);
    this.arguments = arguments;
    this.name = name;
    this.serverLabel = serverLabel;
    this.approvalRequestId = approvalRequestId;
    this.error = error;
    this.output = output;
    this.status = status;
  }

  @Override
  public @NonNull String id() {
    return id;
  }

  public @NonNull String name() {
    return name;
  }

  public @Nullable String error() {
    return error;
  }

  public @Nullable String output() {
    return output;
  }

  public @Nullable McpToolCallStatus status() {
    return status;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (McpToolCall) obj;
    return Objects.equals(this.arguments, that.arguments)
        && Objects.equals(this.id, that.id)
        && Objects.equals(this.name, that.name)
        && Objects.equals(this.serverLabel, that.serverLabel)
        && Objects.equals(this.approvalRequestId, that.approvalRequestId)
        && Objects.equals(this.error, that.error)
        && Objects.equals(this.output, that.output)
        && Objects.equals(this.status, that.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(arguments, id, name, serverLabel, approvalRequestId, error, output, status);
  }

  @Override
  public String toString() {
    return "McpToolCall["
        + "arguments="
        + arguments
        + ", "
        + "id="
        + id
        + ", "
        + "name="
        + name
        + ", "
        + "serverLabel="
        + serverLabel
        + ", "
        + "approvalRequestId="
        + approvalRequestId
        + ", "
        + "error="
        + error
        + ", "
        + "output="
        + output
        + ", "
        + "status="
        + status
        + ']';
  }
}
