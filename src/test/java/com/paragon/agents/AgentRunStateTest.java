package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.FunctionToolCall;

import okhttp3.mockwebserver.MockWebServer;

/**
 * Tests for AgentRunState and its approval workflow.
 */
@DisplayName("AgentRunState Tests")
class AgentRunStateTest {

  private MockWebServer mockWebServer;
  private Responder responder;
  private Agent agent;
  private AgentContext context;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    responder = Responder.builder()
        .baseUrl(mockWebServer.url("/v1/responses"))
        .apiKey("test-key")
        .build();
    agent = Agent.builder()
        .name("TestAgent")
        .instructions("Test instructions")
        .model("test-model")
        .responder(responder)
        .build();
    context = AgentContext.create();
  }

  @Nested
  @DisplayName("Status Enum")
  class StatusEnumTests {

    @Test
    @DisplayName("has all status values")
    void hasAllStatusValues() {
      assertNotNull(AgentRunState.Status.valueOf("RUNNING"));
      assertNotNull(AgentRunState.Status.valueOf("PENDING_TOOL_APPROVAL"));
      assertNotNull(AgentRunState.Status.valueOf("COMPLETED"));
      assertNotNull(AgentRunState.Status.valueOf("FAILED"));
    }
  }

  @Nested
  @DisplayName("Status Predicates")
  class StatusPredicates {

    @Test
    @DisplayName("completed state reports isCompleted true")
    void completedStateReportsIsCompleted() {
      AgentRunState state = AgentRunState.completed(
          "TestAgent", context, null, List.of(), 1);
      
      assertTrue(state.isCompleted());
      assertFalse(state.isPendingApproval());
      assertFalse(state.isFailed());
    }

    @Test
    @DisplayName("failed state reports isFailed true")
    void failedStateReportsIsFailed() {
      AgentRunState state = AgentRunState.failed("TestAgent", context, 1);
      
      assertTrue(state.isFailed());
      assertFalse(state.isCompleted());
      assertFalse(state.isPendingApproval());
    }

    @Test
    @DisplayName("pending approval state reports isPendingApproval true")
    void pendingApprovalStateReportsIsPendingApproval() {
      FunctionToolCall toolCall = new FunctionToolCall(
          "{}", "call_123", "test_tool", null, null);
      AgentRunState state = AgentRunState.pendingApproval(
          "TestAgent", context, toolCall, null, List.of(), 1);
      
      assertTrue(state.isPendingApproval());
      assertFalse(state.isCompleted());
      assertFalse(state.isFailed());
    }
  }

  @Nested
  @DisplayName("Accessors")
  class Accessors {

    @Test
    @DisplayName("agentName returns agent name")
    void agentNameReturnsName() {
      AgentRunState state = AgentRunState.completed(
          "MyAgent", context, null, List.of(), 1);
      
      assertEquals("MyAgent", state.agentName());
    }

    @Test
    @DisplayName("context returns agent context")
    void contextReturnsContext() {
      AgentRunState state = AgentRunState.completed(
          "TestAgent", context, null, List.of(), 1);
      
      assertSame(context, state.context());
    }

    @Test
    @DisplayName("currentTurn returns turn number")
    void currentTurnReturnsTurn() {
      AgentRunState state = AgentRunState.completed(
          "TestAgent", context, null, List.of(), 5);
      
      assertEquals(5, state.currentTurn());
    }

    @Test
    @DisplayName("toolExecutions returns empty list when none")
    void toolExecutionsReturnsEmptyList() {
      AgentRunState state = AgentRunState.completed(
          "TestAgent", context, null, List.of(), 1);
      
      assertTrue(state.toolExecutions().isEmpty());
    }

    @Test
    @DisplayName("pendingToolCall returns null for completed state")
    void pendingToolCallNullForCompleted() {
      AgentRunState state = AgentRunState.completed(
          "TestAgent", context, null, List.of(), 1);
      
      assertNull(state.pendingToolCall());
    }

    @Test
    @DisplayName("pendingToolCall returns tool call for pending state")
    void pendingToolCallReturnsForPending() {
      FunctionToolCall toolCall = new FunctionToolCall(
          "{}", "call_123", "my_tool", null, null);
      AgentRunState state = AgentRunState.pendingApproval(
          "TestAgent", context, toolCall, null, List.of(), 1);
      
      assertNotNull(state.pendingToolCall());
      assertEquals("my_tool", state.pendingToolCall().name());
    }
  }

  @Nested
  @DisplayName("Approval Workflow")
  class ApprovalWorkflow {

    @Test
    @DisplayName("approveToolCall sets approval result")
    void approveToolCallSetsResult() {
      FunctionToolCall toolCall = new FunctionToolCall(
          "{}", "call_123", "test_tool", null, null);
      AgentRunState state = AgentRunState.pendingApproval(
          "TestAgent", context, toolCall, null, List.of(), 1);
      
      state.approveToolCall("tool output");
      
      assertNotNull(state.approvalResult());
      assertTrue(state.approvalResult().approved());
      assertEquals("tool output", state.approvalResult().outputOrReason());
    }

    @Test
    @DisplayName("rejectToolCall sets rejection without reason")
    void rejectToolCallWithoutReason() {
      FunctionToolCall toolCall = new FunctionToolCall(
          "{}", "call_123", "test_tool", null, null);
      AgentRunState state = AgentRunState.pendingApproval(
          "TestAgent", context, toolCall, null, List.of(), 1);
      
      state.rejectToolCall();
      
      assertNotNull(state.approvalResult());
      assertFalse(state.approvalResult().approved());
      assertNull(state.approvalResult().outputOrReason());
    }

    @Test
    @DisplayName("rejectToolCall sets rejection with reason")
    void rejectToolCallWithReason() {
      FunctionToolCall toolCall = new FunctionToolCall(
          "{}", "call_123", "test_tool", null, null);
      AgentRunState state = AgentRunState.pendingApproval(
          "TestAgent", context, toolCall, null, List.of(), 1);
      
      state.rejectToolCall("User denied");
      
      assertNotNull(state.approvalResult());
      assertFalse(state.approvalResult().approved());
      assertEquals("User denied", state.approvalResult().outputOrReason());
    }

    @Test
    @DisplayName("approveToolCall throws for completed state")
    void approveThrowsForCompleted() {
      AgentRunState state = AgentRunState.completed(
          "TestAgent", context, null, List.of(), 1);
      
      assertThrows(IllegalStateException.class, () ->
          state.approveToolCall("output"));
    }

    @Test
    @DisplayName("rejectToolCall throws for completed state")
    void rejectThrowsForCompleted() {
      AgentRunState state = AgentRunState.completed(
          "TestAgent", context, null, List.of(), 1);
      
      assertThrows(IllegalStateException.class, () ->
          state.rejectToolCall());
    }

    @Test
    @DisplayName("rejectToolCall with reason throws for failed state")
    void rejectWithReasonThrowsForFailed() {
      AgentRunState state = AgentRunState.failed("TestAgent", context, 1);
      
      assertThrows(IllegalStateException.class, () ->
          state.rejectToolCall("reason"));
    }
  }
}
