package com.paragon.agents;

import java.time.Duration;
import org.jspecify.annotations.NonNull;

import com.paragon.responses.spec.FunctionToolCallOutput;
import com.paragon.responses.spec.FunctionToolCallOutputStatus;

/**
 * Records the execution details of a single tool call during an agent run.
 *
 * <p>ToolExecution captures metadata about each tool invocation, which is useful for:
 *
 * <ul>
 *   <li>Debugging and logging agent behavior
 *   <li>Monitoring tool performance
 *   <li>Auditing tool usage
 * </ul>
 *
 * @param toolName the name of the tool that was executed
 * @param callId the unique identifier for this tool call
 * @param arguments the JSON arguments passed to the tool
 * @param output the result returned by the tool
 * @param duration how long the tool execution took
 * @see AgentResult
 * @since 1.0
 */
public record ToolExecution(
    @NonNull String toolName,
    @NonNull String callId,
    @NonNull String arguments,
    @NonNull FunctionToolCallOutput output,
    @NonNull Duration duration) {

  public ToolExecution {
    if (toolName == null || toolName.isBlank()) {
      throw new IllegalArgumentException("toolName cannot be null or blank");
    }
    if (callId == null || callId.isBlank()) {
      throw new IllegalArgumentException("callId cannot be null or blank");
    }
    if (arguments == null) {
      throw new IllegalArgumentException("arguments cannot be null");
    }
    if (output == null) {
      throw new IllegalArgumentException("output cannot be null");
    }
    if (duration == null) {
      throw new IllegalArgumentException("duration cannot be null");
    }
  }

  /**
   * Returns true if the tool execution succeeded.
   *
   * @return true if output status is COMPLETED
   */
  public boolean isSuccess() {
    return output.status() == FunctionToolCallOutputStatus.COMPLETED;
  }
}
