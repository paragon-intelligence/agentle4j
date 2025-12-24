package com.paragon.responses.spec;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * A tool call to a computer use tool. See the <a
 * href="https://platform.openai.com/docs/guides/tools-computer-use">computer use guide</a> for more
 * information.
 */
public final class ComputerToolCall extends ToolCall implements Item, ResponseOutput {
  private final @NonNull ComputerUseAction action;
  private final @NonNull String callId;
  private final @NonNull List<PendingSafetyCheck> pendingSafetyChecks;
  private final @NonNull ComputerToolCallStatus status;

  /**
   * @param action action to perform in the computer being used
   * @param callId An identifier used when responding to the tool call with output.
   * @param pendingSafetyChecks The pending safety checks for the computer call.
   * @param status The status of the item. One of in_progress, completed, or incomplete. Populated
   *     when items are returned via API.
   */
  public ComputerToolCall(
      @NonNull ComputerUseAction action,
      @NonNull String id,
      @NonNull String callId,
      @NonNull List<PendingSafetyCheck> pendingSafetyChecks,
      @NonNull ComputerToolCallStatus status) {
    super(id);
    this.action = action;
    this.callId = callId;
    this.pendingSafetyChecks = pendingSafetyChecks;
    this.status = status;
  }

  @Override
  public @NonNull String toString() {
    return String.format(
        """
        <computer_tool_call>
            <call_id>%s</call_id>
            <id>%s</id>
            <status>%s</status>
            <action>%s</action>
            <pending_safety_checks>%s</pending_safety_checks>
        </computer_tool_call>
        """,
        callId, id, status, action, pendingSafetyChecks);
  }

  public @NonNull ComputerUseAction action() {
    return action;
  }

  public @NonNull String callId() {
    return callId;
  }

  @Override
  public @NonNull String id() {
    return id;
  }

  public @NonNull List<PendingSafetyCheck> pendingSafetyChecks() {
    return pendingSafetyChecks;
  }

  public @NonNull ComputerToolCallStatus status() {
    return status;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (ComputerToolCall) obj;
    return Objects.equals(this.action, that.action)
        && Objects.equals(this.callId, that.callId)
        && Objects.equals(this.id, that.id)
        && Objects.equals(this.pendingSafetyChecks, that.pendingSafetyChecks)
        && Objects.equals(this.status, that.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(action, callId, id, pendingSafetyChecks, status);
  }
}
