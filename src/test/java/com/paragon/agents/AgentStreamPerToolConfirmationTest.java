package com.paragon.agents;

import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCall;
import com.paragon.responses.spec.FunctionToolCallOutput;
import com.paragon.responses.spec.FunctionToolStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AgentStream per-tool confirmation behavior. Tests the logic that determines which tools
 * require confirmation.
 */
@DisplayName("AgentStream Per-Tool Confirmation")
class AgentStreamPerToolConfirmationTest {

  record SimpleParams(String value) {
  }

  @FunctionMetadata(name = "safe_tool", description = "A safe tool")
  static class SafeTool extends FunctionTool<SimpleParams> {
    @Override
    public FunctionToolCallOutput call(SimpleParams params) {
      return FunctionToolCallOutput.success("safe_call", "Safe result");
    }
  }

  @FunctionMetadata(
          name = "dangerous_tool",
          description = "A dangerous tool",
          requiresConfirmation = true)
  static class DangerousTool extends FunctionTool<SimpleParams> {
    @Override
    public FunctionToolCallOutput call(SimpleParams params) {
      return FunctionToolCallOutput.success("dangerous_call", "Dangerous result");
    }
  }

  @FunctionMetadata(
          name = "send_email",
          description = "Sends an email",
          requiresConfirmation = true)
  static class SendEmailTool extends FunctionTool<SimpleParams> {
    @Override
    public FunctionToolCallOutput call(SimpleParams params) {
      return FunctionToolCallOutput.success("email_call", "Email sent");
    }
  }

  @Nested
  @DisplayName("Tool Store Per-Tool Confirmation")
  class ToolStorePerToolConfirmationTests {

    @Test
    @DisplayName("store correctly identifies tools requiring confirmation")
    void store_identifiesToolsRequiringConfirmation() {
      FunctionToolStore store =
              FunctionToolStore.create()
                      .add(new SafeTool())
                      .add(new DangerousTool())
                      .add(new SendEmailTool());

      // Safe tool - no confirmation
      FunctionTool<?> safe = store.get("safe_tool");
      assertNotNull(safe);
      assertFalse(safe.requiresConfirmation());

      // Dangerous tool - needs confirmation
      FunctionTool<?> dangerous = store.get("dangerous_tool");
      assertNotNull(dangerous);
      assertTrue(dangerous.requiresConfirmation());

      // Email tool - needs confirmation
      FunctionTool<?> email = store.get("send_email");
      assertNotNull(email);
      assertTrue(email.requiresConfirmation());
    }

    @Test
    @DisplayName("unknown tool returns null and check is safe")
    void unknownTool_nullSafeCheck() {
      FunctionToolStore store = FunctionToolStore.create().add(new SafeTool());

      FunctionTool<?> unknown = store.get("unknown_tool");

      // This is the exact logic used in AgentStream
      boolean toolRequiresConfirmation = unknown != null && unknown.requiresConfirmation();

      assertNull(unknown);
      assertFalse(toolRequiresConfirmation);
    }
  }

  @Nested
  @DisplayName("Per-Tool Confirmation Logic (AgentStream simulation)")
  class PerToolConfirmationLogicTests {

    /**
     * Simulates the AgentStream logic for checking if a tool requires confirmation.
     */
    private boolean checkRequiresConfirmation(FunctionToolStore store, String toolName) {
      FunctionTool<?> tool = store.get(toolName);
      return tool != null && tool.requiresConfirmation();
    }

    @Test
    @DisplayName("safe tool does not trigger confirmation")
    void safeTool_noConfirmation() {
      FunctionToolStore store =
              FunctionToolStore.create().add(new SafeTool()).add(new DangerousTool());

      assertFalse(checkRequiresConfirmation(store, "safe_tool"));
    }

    @Test
    @DisplayName("dangerous tool triggers confirmation")
    void dangerousTool_triggersConfirmation() {
      FunctionToolStore store =
              FunctionToolStore.create().add(new SafeTool()).add(new DangerousTool());

      assertTrue(checkRequiresConfirmation(store, "dangerous_tool"));
    }

    @Test
    @DisplayName("multiple dangerous tools each trigger confirmation")
    void multipleDangerousTools_eachTriggersConfirmation() {
      FunctionToolStore store =
              FunctionToolStore.create()
                      .add(new SafeTool())
                      .add(new DangerousTool())
                      .add(new SendEmailTool());

      assertFalse(checkRequiresConfirmation(store, "safe_tool"));
      assertTrue(checkRequiresConfirmation(store, "dangerous_tool"));
      assertTrue(checkRequiresConfirmation(store, "send_email"));
    }
  }

  @Nested
  @DisplayName("ToolConfirmationHandler Interface")
  class ToolConfirmationHandlerTests {

    @Test
    @DisplayName("handler receives tool call and can approve")
    void handler_canApprove() {
      // Constructor order: arguments, callId, name, id, status
      FunctionToolCall mockCall =
              new FunctionToolCall("{\"value\": \"test\"}", "call-123", "dangerous_tool", null, null);

      AtomicBoolean result = new AtomicBoolean(false);

      AgentStream.ToolConfirmationHandler handler =
              (call, approve) -> {
                assertEquals("dangerous_tool", call.name());
                assertEquals("{\"value\": \"test\"}", call.arguments());
                assertEquals("call-123", call.callId());
                approve.accept(true);
              };

      handler.handle(mockCall, result::set);

      assertTrue(result.get());
    }

    @Test
    @DisplayName("handler can reject")
    void handler_canReject() {
      // Constructor order: arguments, callId, name, id, status
      FunctionToolCall mockCall =
              new FunctionToolCall("{}", "call-123", "dangerous_tool", null, null);

      AtomicBoolean result = new AtomicBoolean(true);

      AgentStream.ToolConfirmationHandler handler =
              (call, approve) -> {
                approve.accept(false); // Reject
              };

      handler.handle(mockCall, result::set);

      assertFalse(result.get());
    }
  }

  @Nested
  @DisplayName("AgentRunState for pause/resume")
  class AgentRunStateTests {

    @Test
    @DisplayName("can approve with tool output")
    void approveToolCall_withOutput() {
      // Constructor order: arguments, callId, name, id, status
      FunctionToolCall pendingCall =
              new FunctionToolCall("{\"table\": \"users\"}", "call-123", "delete_records", null, null);

      AgentRunState state =
              AgentRunState.pendingApproval(
                      "TestAgent", AgenticContext.create(), pendingCall, null, java.util.List.of(), 1);

      assertTrue(state.isPendingApproval());
      assertEquals("delete_records", state.pendingToolCall().name());
      assertEquals("call-123", state.pendingToolCall().callId());

      // Approve with output
      state.approveToolCall("Deleted 50 records from users table");

      assertNotNull(state.approvalResult());
      assertTrue(state.approvalResult().approved());
      assertEquals("Deleted 50 records from users table", state.approvalResult().outputOrReason());
    }

    @Test
    @DisplayName("can reject with reason")
    void rejectToolCall_withReason() {
      // Constructor order: arguments, callId, name, id, status
      FunctionToolCall pendingCall =
              new FunctionToolCall("{}", "call-123", "delete_records", null, null);

      AgentRunState state =
              AgentRunState.pendingApproval(
                      "TestAgent", AgenticContext.create(), pendingCall, null, java.util.List.of(), 1);

      state.rejectToolCall("Manager denied: too risky");

      assertNotNull(state.approvalResult());
      assertFalse(state.approvalResult().approved());
      assertEquals("Manager denied: too risky", state.approvalResult().outputOrReason());
    }

    @Test
    @DisplayName("throws if approving when not pending")
    void approveToolCall_throwsIfNotPending() {
      AgentRunState completedState =
              AgentRunState.completed("TestAgent", AgenticContext.create(), null, java.util.List.of(), 1);

      assertThrows(
              IllegalStateException.class,
              () -> {
                completedState.approveToolCall("output");
              });
    }
  }
}
