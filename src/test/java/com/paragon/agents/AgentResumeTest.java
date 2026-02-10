package com.paragon.agents;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.FunctionToolCall;
import com.paragon.responses.spec.Message;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Agent resume functionality and AgentRunState.
 */
@DisplayName("Agent Resume and Run State")
class AgentResumeTest {

  private MockWebServer mockWebServer;
  private Responder responder;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    responder =
            Responder.builder().baseUrl(mockWebServer.url("/v1/responses")).apiKey("test-key").build();
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // AGENT RUN STATE FACTORY METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  private Agent createTestAgent() {
    return Agent.builder()
            .name("TestAgent")
            .model("test-model")
            .instructions("Test instructions")
            .responder(responder)
            .build();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // AGENT RUN STATE APPROVAL METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  private FunctionToolCall createTestToolCall() {
    return new FunctionToolCall(
            "{\"param\":\"value\"}", // arguments
            "call_123", // callId
            "test_function", // name
            "id_456", // id
            null // status
    );
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // AGENT RESUME VALIDATION
  // ═══════════════════════════════════════════════════════════════════════════

  private void enqueueSuccessResponse(String text) {
    String json =
            """
                    {
                      "id": "resp_001",
                      "object": "response",
                      "created_at": 1234567890,
                      "status": "completed",
                      "model": "test-model",
                      "output": [
                        {
                          "type": "message",
                          "id": "msg_001",
                          "role": "assistant",
                          "content": [
                            {
                              "type": "output_text",
                              "text": "%s"
                            }
                          ]
                        }
                      ],
                      "usage": {
                        "input_tokens": 10,
                        "output_tokens": 5,
                        "total_tokens": 15
                      }
                    }
                    """
                    .formatted(text);

    mockWebServer.enqueue(
            new MockResponse()
                    .setResponseCode(200)
                    .setBody(json)
                    .addHeader("Content-Type", "application/json"));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // AGENT RESUME WITH APPROVAL
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("AgentRunState Factory Methods")
  class AgentRunStateFactoryMethods {

    @Test
    @DisplayName("pendingApproval creates state with PENDING status")
    void pendingApprovalCreatesCorrectState() {
      AgenticContext context = AgenticContext.create();
      FunctionToolCall call = createTestToolCall();

      AgentRunState state =
              AgentRunState.pendingApproval("TestAgent", context, call, null, new ArrayList<>(), 1);

      assertTrue(state.isPendingApproval());
      assertFalse(state.isCompleted());
      assertFalse(state.isFailed());
      assertEquals("TestAgent", state.agentName());
      assertEquals(context, state.context());
      assertEquals(call, state.pendingToolCall());
      assertEquals(1, state.currentTurn());
    }

    @Test
    @DisplayName("completed creates state with COMPLETED status")
    void completedCreatesCorrectState() {
      AgenticContext context = AgenticContext.create();

      AgentRunState state =
              AgentRunState.completed("TestAgent", context, null, new ArrayList<>(), 3);

      assertTrue(state.isCompleted());
      assertFalse(state.isPendingApproval());
      assertFalse(state.isFailed());
      assertEquals(3, state.currentTurn());
      assertNull(state.pendingToolCall());
    }

    @Test
    @DisplayName("failed creates state with FAILED status")
    void failedCreatesCorrectState() {
      AgenticContext context = AgenticContext.create();

      AgentRunState state = AgentRunState.failed("TestAgent", context, 2);

      assertTrue(state.isFailed());
      assertFalse(state.isCompleted());
      assertFalse(state.isPendingApproval());
      assertEquals(2, state.currentTurn());
    }

    @Test
    @DisplayName("toolExecutions returns copy of list")
    void toolExecutionsReturnsDefensiveCopy() {
      AgenticContext context = AgenticContext.create();
      FunctionToolCall call = createTestToolCall();
      List<ToolExecution> executions = new ArrayList<>();

      AgentRunState state =
              AgentRunState.pendingApproval("TestAgent", context, call, null, executions, 1);

      List<ToolExecution> returned = state.toolExecutions();
      assertNotSame(executions, returned);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // AGENT RUN STATE ACCESSORS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("AgentRunState Approval Methods")
  class AgentRunStateApprovalMethods {

    @Test
    @DisplayName("approveToolCall sets approval result")
    void approveToolCallSetsResult() {
      AgenticContext context = AgenticContext.create();
      FunctionToolCall call = createTestToolCall();
      AgentRunState state =
              AgentRunState.pendingApproval("TestAgent", context, call, null, new ArrayList<>(), 1);

      state.approveToolCall("Tool output");

      AgentRunState.ToolApprovalResult result = state.approvalResult();
      assertNotNull(result);
      assertTrue(result.approved());
      assertEquals("Tool output", result.outputOrReason());
    }

    @Test
    @DisplayName("rejectToolCall without reason sets rejection")
    void rejectToolCallWithoutReason() {
      AgenticContext context = AgenticContext.create();
      FunctionToolCall call = createTestToolCall();
      AgentRunState state =
              AgentRunState.pendingApproval("TestAgent", context, call, null, new ArrayList<>(), 1);

      state.rejectToolCall();

      AgentRunState.ToolApprovalResult result = state.approvalResult();
      assertNotNull(result);
      assertFalse(result.approved());
      assertNull(result.outputOrReason());
    }

    @Test
    @DisplayName("rejectToolCall with reason sets rejection reason")
    void rejectToolCallWithReason() {
      AgenticContext context = AgenticContext.create();
      FunctionToolCall call = createTestToolCall();
      AgentRunState state =
              AgentRunState.pendingApproval("TestAgent", context, call, null, new ArrayList<>(), 1);

      state.rejectToolCall("User denied access");

      AgentRunState.ToolApprovalResult result = state.approvalResult();
      assertNotNull(result);
      assertFalse(result.approved());
      assertEquals("User denied access", result.outputOrReason());
    }

    @Test
    @DisplayName("approveToolCall throws if not pending")
    void approveToolCallThrowsIfNotPending() {
      AgenticContext context = AgenticContext.create();
      AgentRunState state =
              AgentRunState.completed("TestAgent", context, null, new ArrayList<>(), 1);

      assertThrows(IllegalStateException.class, () -> state.approveToolCall("output"));
    }

    @Test
    @DisplayName("rejectToolCall throws if not pending")
    void rejectToolCallThrowsIfNotPending() {
      AgenticContext context = AgenticContext.create();
      AgentRunState state = AgentRunState.failed("TestAgent", context, 1);

      assertThrows(IllegalStateException.class, () -> state.rejectToolCall());
    }

    @Test
    @DisplayName("rejectToolCall with reason throws if not pending")
    void rejectToolCallWithReasonThrowsIfNotPending() {
      AgenticContext context = AgenticContext.create();
      AgentRunState state =
              AgentRunState.completed("TestAgent", context, null, new ArrayList<>(), 1);

      assertThrows(IllegalStateException.class, () -> state.rejectToolCall("reason"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TOOL APPROVAL RESULT RECORD
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Agent Resume Validation")
  class AgentResumeValidation {

    @Test
    @DisplayName("resume throws if state is null")
    void resumeThrowsIfStateNull() {
      Agent agent = createTestAgent();

      assertThrows(NullPointerException.class, () -> agent.resume(null));
    }

    @Test
    @DisplayName("resume throws if state is not pending approval")
    void resumeThrowsIfNotPendingApproval() {
      Agent agent = createTestAgent();
      AgenticContext context = AgenticContext.create();
      AgentRunState state =
              AgentRunState.completed("TestAgent", context, null, new ArrayList<>(), 1);

      assertThrows(IllegalStateException.class, () -> agent.resume(state));
    }

    @Test
    @DisplayName("resume throws if no approval decision made")
    void resumeThrowsIfNoApprovalDecision() {
      Agent agent = createTestAgent();
      AgenticContext context = AgenticContext.create();
      FunctionToolCall call = createTestToolCall();
      AgentRunState state =
              AgentRunState.pendingApproval("TestAgent", context, call, null, new ArrayList<>(), 1);

      // No approveToolCall() or rejectToolCall() called
      assertThrows(IllegalStateException.class, () -> agent.resume(state));
    }

    @Test
    @DisplayName("resumeStream throws if state is null")
    void resumeStreamThrowsIfStateNull() {
      Agent agent = createTestAgent();

      assertThrows(NullPointerException.class, () -> agent.resumeStream(null));
    }

    @Test
    @DisplayName("resumeStream throws if not pending approval")
    void resumeStreamThrowsIfNotPendingApproval() {
      Agent agent = createTestAgent();
      AgenticContext context = AgenticContext.create();
      AgentRunState state = AgentRunState.failed("TestAgent", context, 1);

      assertThrows(IllegalStateException.class, () -> agent.resumeStream(state));
    }

    @Test
    @DisplayName("resumeStream throws if no approval decision made")
    void resumeStreamThrowsIfNoApprovalDecision() {
      Agent agent = createTestAgent();
      AgenticContext context = AgenticContext.create();
      FunctionToolCall call = createTestToolCall();
      AgentRunState state =
              AgentRunState.pendingApproval("TestAgent", context, call, null, new ArrayList<>(), 1);

      // No approveToolCall() or rejectToolCall() called
      assertThrows(IllegalStateException.class, () -> agent.resumeStream(state));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPER METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Agent Resume with Approval")
  class AgentResumeWithApproval {

    @Test
    @DisplayName("resume with approval continues agentic loop")
    void resumeWithApprovalContinuesLoop() throws Exception {
      Agent agent = createTestAgent();
      AgenticContext context = AgenticContext.create();
      context.addInput(Message.user("Hello"));
      FunctionToolCall call = createTestToolCall();

      AgentRunState state =
              AgentRunState.pendingApproval("TestAgent", context, call, null, new ArrayList<>(), 1);
      state.approveToolCall("Tool executed successfully");

      enqueueSuccessResponse("Resume response");

      AgentResult result = agent.resume(state);

      assertNotNull(result);
    }

    @Test
    @DisplayName("resume with rejection continues with error output")
    void resumeWithRejectionContinuesWithErrorOutput() throws Exception {
      Agent agent = createTestAgent();
      AgenticContext context = AgenticContext.create();
      context.addInput(Message.user("Hello"));
      FunctionToolCall call = createTestToolCall();

      AgentRunState state =
              AgentRunState.pendingApproval("TestAgent", context, call, null, new ArrayList<>(), 1);
      state.rejectToolCall("User denied");

      enqueueSuccessResponse("After rejection");

      AgentResult result = agent.resume(state);

      assertNotNull(result);
    }

    @Test
    @DisplayName("resume with rejection (no reason) uses default message")
    void resumeWithRejectionNoReasonUsesDefault() throws Exception {
      Agent agent = createTestAgent();
      AgenticContext context = AgenticContext.create();
      context.addInput(Message.user("Hello"));
      FunctionToolCall call = createTestToolCall();

      AgentRunState state =
              AgentRunState.pendingApproval("TestAgent", context, call, null, new ArrayList<>(), 1);
      state.rejectToolCall();

      enqueueSuccessResponse("After rejection with default");

      AgentResult result = agent.resume(state);

      assertNotNull(result);
    }

    @Test
    @DisplayName("resumeStream with approval returns AgentStream")
    void resumeStreamWithApprovalReturnsStream() {
      Agent agent = createTestAgent();
      AgenticContext context = AgenticContext.create();
      context.addInput(Message.user("Hello"));
      FunctionToolCall call = createTestToolCall();

      AgentRunState state =
              AgentRunState.pendingApproval("TestAgent", context, call, null, new ArrayList<>(), 1);
      state.approveToolCall("Approved output");

      AgentStream stream = agent.resumeStream(state);

      assertNotNull(stream);
    }

    @Test
    @DisplayName("resumeStream with rejection returns AgentStream")
    void resumeStreamWithRejectionReturnsStream() {
      Agent agent = createTestAgent();
      AgenticContext context = AgenticContext.create();
      context.addInput(Message.user("Hello"));
      FunctionToolCall call = createTestToolCall();

      AgentRunState state =
              AgentRunState.pendingApproval("TestAgent", context, call, null, new ArrayList<>(), 1);
      state.rejectToolCall("Rejected");

      AgentStream stream = agent.resumeStream(state);

      assertNotNull(stream);
    }
  }

  @Nested
  @DisplayName("AgentRunState Accessors")
  class AgentRunStateAccessors {

    @Test
    @DisplayName("status returns correct enum value")
    void statusReturnsCorrectValue() {
      AgenticContext context = AgenticContext.create();
      FunctionToolCall call = createTestToolCall();

      AgentRunState pending =
              AgentRunState.pendingApproval("Test", context, call, null, new ArrayList<>(), 1);
      AgentRunState completed =
              AgentRunState.completed("Test", context, null, new ArrayList<>(), 1);
      AgentRunState failed = AgentRunState.failed("Test", context, 1);

      assertEquals(AgentRunState.Status.PENDING_TOOL_APPROVAL, pending.status());
      assertEquals(AgentRunState.Status.COMPLETED, completed.status());
      assertEquals(AgentRunState.Status.FAILED, failed.status());
    }

    @Test
    @DisplayName("lastResponse can be null")
    void lastResponseCanBeNull() {
      AgenticContext context = AgenticContext.create();

      AgentRunState state = AgentRunState.completed("Test", context, null, new ArrayList<>(), 1);

      assertNull(state.lastResponse());
    }
  }

  @Nested
  @DisplayName("ToolApprovalResult Record")
  class ToolApprovalResultTests {

    @Test
    @DisplayName("tool approval result stores approved status")
    void toolApprovalResultStoresApprovedStatus() {
      AgenticContext context = AgenticContext.create();
      FunctionToolCall call = createTestToolCall();
      AgentRunState state =
              AgentRunState.pendingApproval("Test", context, call, null, new ArrayList<>(), 1);

      state.approveToolCall("output");
      AgentRunState.ToolApprovalResult result = state.approvalResult();

      assertTrue(result.approved());
      assertEquals("output", result.outputOrReason());
    }

    @Test
    @DisplayName("tool approval result stores rejected status with null reason")
    void toolApprovalResultStoresRejectedWithNullReason() {
      AgenticContext context = AgenticContext.create();
      FunctionToolCall call = createTestToolCall();
      AgentRunState state =
              AgentRunState.pendingApproval("Test", context, call, null, new ArrayList<>(), 1);

      state.rejectToolCall();
      AgentRunState.ToolApprovalResult result = state.approvalResult();

      assertFalse(result.approved());
      assertNull(result.outputOrReason());
    }
  }
}
