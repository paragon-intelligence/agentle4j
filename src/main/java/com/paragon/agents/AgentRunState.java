package com.paragon.agents;

import com.paragon.responses.spec.FunctionToolCall;
import com.paragon.responses.spec.Response;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Serializable state of a paused agent run.
 *
 * <p>When an agent run is paused (e.g., waiting for human approval of a tool call), this state
 * captures everything needed to resume the run later - even days later.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * // Pause when tool needs approval
 * AgentRunState state = agent.interact("Do something")
 *     .onToolCallPending((call, pause) -> {
 *         AgentRunState pausedState = pause.pauseForApproval(call);
 *         saveToDatabase(pausedState);  // Persist for later
 *     })
 *     .start().join();
 *
 * // Resume days later
 * AgentRunState savedState = loadFromDatabase();
 * savedState.approveToolCall(toolCallOutput);  // Or rejectToolCall()
 * AgentResult result = agent.resume(savedState);
 * }</pre>
 *
 * @since 1.0
 */
public final class AgentRunState implements Serializable {

  private static final long serialVersionUID = 1L;
  private final @NonNull String agentName;
  private final @NonNull AgenticContext context;
  private final @NonNull Status status;
  private final @Nullable FunctionToolCall pendingToolCall;
  private final @Nullable Response lastResponse;
  private final @NonNull List<ToolExecution> toolExecutions;
  private final int currentTurn;
  // For resuming after approval
  private transient @Nullable ToolApprovalResult approvalResult;

  private AgentRunState(
          @NonNull String agentName,
          @NonNull AgenticContext context,
          @NonNull Status status,
          @Nullable FunctionToolCall pendingToolCall,
          @Nullable Response lastResponse,
          @NonNull List<ToolExecution> toolExecutions,
          int currentTurn) {
    this.agentName = agentName;
    this.context = context;
    this.status = status;
    this.pendingToolCall = pendingToolCall;
    this.lastResponse = lastResponse;
    this.toolExecutions = new ArrayList<>(toolExecutions);
    this.currentTurn = currentTurn;
  }

  /**
   * Creates a state for a run pending tool approval.
   */
  static AgentRunState pendingApproval(
          @NonNull String agentName,
          @NonNull AgenticContext context,
          @NonNull FunctionToolCall pendingToolCall,
          @Nullable Response lastResponse,
          @NonNull List<ToolExecution> toolExecutions,
          int currentTurn) {
    return new AgentRunState(
            agentName,
            context,
            Status.PENDING_TOOL_APPROVAL,
            pendingToolCall,
            lastResponse,
            toolExecutions,
            currentTurn);
  }

  // ===== Factory Methods =====

  /**
   * Creates a state for a completed run.
   */
  static AgentRunState completed(
          @NonNull String agentName,
          @NonNull AgenticContext context,
          @Nullable Response lastResponse,
          @NonNull List<ToolExecution> toolExecutions,
          int currentTurn) {
    return new AgentRunState(
            agentName, context, Status.COMPLETED, null, lastResponse, toolExecutions, currentTurn);
  }

  /**
   * Creates a state for a failed run.
   */
  static AgentRunState failed(
          @NonNull String agentName, @NonNull AgenticContext context, int currentTurn) {
    return new AgentRunState(agentName, context, Status.FAILED, null, null, List.of(), currentTurn);
  }

  /**
   * Approves the pending tool call with the given output.
   *
   * <p>Call this after getting user approval, then pass the state to {@code Agent.resume()}.
   *
   * @param output the tool output to use
   * @throws IllegalStateException if not pending approval
   */
  public void approveToolCall(@NonNull String output) {
    if (status != Status.PENDING_TOOL_APPROVAL || pendingToolCall == null) {
      throw new IllegalStateException("No pending tool call to approve");
    }
    this.approvalResult = new ToolApprovalResult(true, output);
  }

  // ===== Approval Methods =====

  /**
   * Rejects the pending tool call.
   *
   * <p>Call this if the user denies the tool execution, then pass the state to {@code
   * Agent.resume()}.
   *
   * @throws IllegalStateException if not pending approval
   */
  public void rejectToolCall() {
    if (status != Status.PENDING_TOOL_APPROVAL || pendingToolCall == null) {
      throw new IllegalStateException("No pending tool call to reject");
    }
    this.approvalResult = new ToolApprovalResult(false, null);
  }

  /**
   * Rejects the pending tool call with a reason.
   *
   * @param reason the rejection reason (shown to the model)
   * @throws IllegalStateException if not pending approval
   */
  public void rejectToolCall(@NonNull String reason) {
    if (status != Status.PENDING_TOOL_APPROVAL || pendingToolCall == null) {
      throw new IllegalStateException("No pending tool call to reject");
    }
    this.approvalResult = new ToolApprovalResult(false, reason);
  }

  public @NonNull String agentName() {
    return agentName;
  }

  // ===== Getters =====

  public @NonNull AgenticContext context() {
    return context;
  }

  public @NonNull Status status() {
    return status;
  }

  public @Nullable FunctionToolCall pendingToolCall() {
    return pendingToolCall;
  }

  public @Nullable Response lastResponse() {
    return lastResponse;
  }

  public @NonNull List<ToolExecution> toolExecutions() {
    return List.copyOf(toolExecutions);
  }

  public int currentTurn() {
    return currentTurn;
  }

  @Nullable
  ToolApprovalResult approvalResult() {
    return approvalResult;
  }

  public boolean isPendingApproval() {
    return status == Status.PENDING_TOOL_APPROVAL;
  }

  public boolean isCompleted() {
    return status == Status.COMPLETED;
  }

  public boolean isFailed() {
    return status == Status.FAILED;
  }

  /**
   * The current status of the agent run.
   */
  public enum Status {
    /**
     * Run is in progress.
     */
    RUNNING,
    /**
     * Run is paused waiting for tool approval.
     */
    PENDING_TOOL_APPROVAL,
    /**
     * Run completed successfully.
     */
    COMPLETED,
    /**
     * Run failed with an error.
     */
    FAILED
  }

  // ===== Internal Classes =====

  record ToolApprovalResult(boolean approved, @Nullable String outputOrReason) {
  }
}
