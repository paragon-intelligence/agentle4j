package com.paragon.agents;

import com.paragon.responses.Responder;
import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCall;
import com.paragon.responses.spec.FunctionToolCallOutput;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for non-streaming human-in-the-loop (HITL) support.
 *
 * <p>Verifies that {@code Agent.interact()} auto-pauses when encountering a tool with {@code
 * requiresConfirmation=true} and can be resumed with {@code Agent.resume()}.
 */
@DisplayName("Agent Non-Streaming Human-in-the-Loop")
class AgentNonStreamingHitlTest {

  private MockWebServer mockWebServer;
  private Responder responder;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    responder =
            Responder.builder().baseUrl(mockWebServer.url("/v1/responses")).apiKey("test-key").build();
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  private String buildToolCallResponse(String toolName) {
    return """
            {
              "id": "resp_123",
              "object": "response",
              "status": "completed",
              "output": [
                {
                  "type": "function_call",
                  "id": "fc_123",
                  "call_id": "call_123",
                  "name": "%s",
                  "arguments": "{\\"value\\": \\"test\\"}"
                }
              ],
              "usage": {"input_tokens": 10, "output_tokens": 20, "total_tokens": 30}
            }
            """
            .formatted(toolName);
  }

  private String buildTextResponse(String text) {
    return """
            {
              "id": "resp_456",
              "object": "response",
              "status": "completed",
              "output": [
                {
                  "type": "message",
                  "id": "msg_123",
                  "role": "assistant",
                  "content": [{"type": "output_text", "text": "%s"}]
                }
              ],
              "usage": {"input_tokens": 10, "output_tokens": 20, "total_tokens": 30}
            }
            """
            .formatted(text);
  }

  record SimpleParams(String value) {
  }

  @FunctionMetadata(name = "safe_tool", description = "A safe tool that auto-executes")
  static class SafeTool extends FunctionTool<SimpleParams> {
    boolean executed = false;

    @Override
    public FunctionToolCallOutput call(SimpleParams params) {
      executed = true;
      return FunctionToolCallOutput.success("safe_call", "Safe result: " + params.value());
    }
  }

  @FunctionMetadata(
          name = "dangerous_tool",
          description = "A dangerous tool requiring confirmation",
          requiresConfirmation = true)
  static class DangerousTool extends FunctionTool<SimpleParams> {
    boolean executed = false;

    @Override
    public FunctionToolCallOutput call(SimpleParams params) {
      executed = true;
      return FunctionToolCallOutput.success(
              "dangerous_call", "Dangerous result: " + params.value());
    }
  }

  @Nested
  @DisplayName("Auto-Pause Behavior")
  class AutoPauseBehaviorTests {

    @Test
    @DisplayName("interact() pauses for tool requiring confirmation")
    void interactPausesForToolRequiringConfirmation() {
      DangerousTool dangerousTool = new DangerousTool();

      Agent agent =
              Agent.builder()
                      .name("TestAgent")
                      .instructions("Test instructions")
                      .model("test-model")
                      .responder(responder)
                      .addTool(dangerousTool)
                      .build();

      // LLM responds with a call to dangerous_tool
      mockWebServer.enqueue(
              new MockResponse().setResponseCode(200).setBody(buildToolCallResponse("dangerous_tool")));

      AgentResult result = agent.interact("Do something dangerous");

      // Should be paused, not completed
      assertTrue(result.isPaused(), "Agent should be paused");
      assertFalse(result.isError(), "Agent should not have error");
      assertFalse(dangerousTool.executed, "Tool should NOT have been executed");

      // Verify paused state
      AgentRunState state = result.pausedState();
      assertNotNull(state, "Paused state should not be null");
      assertTrue(state.isPendingApproval(), "State should be pending approval");
      assertEquals("dangerous_tool", state.pendingToolCall().name());
      assertEquals("call_123", state.pendingToolCall().callId());
    }

    @Test
    @DisplayName("interact() does not pause for safe tools")
    void interactDoesNotPauseForSafeTools() {
      SafeTool safeTool = new SafeTool();

      Agent agent =
              Agent.builder()
                      .name("TestAgent")
                      .instructions("Test instructions")
                      .model("test-model")
                      .responder(responder)
                      .addTool(safeTool)
                      .build();

      // LLM responds with a call to safe_tool
      mockWebServer.enqueue(
              new MockResponse().setResponseCode(200).setBody(buildToolCallResponse("safe_tool")));

      // After tool execution, LLM gives final answer
      mockWebServer.enqueue(
              new MockResponse().setResponseCode(200).setBody(buildTextResponse("Done!")));

      AgentResult result = agent.interact("Do something safe");

      // Should complete, not pause
      assertFalse(result.isPaused(), "Agent should not be paused");
      assertFalse(result.isError(), "Agent should not have error");
      assertTrue(safeTool.executed, "Safe tool should have been executed");
      assertEquals("Done!", result.output());
    }
  }

  @Nested
  @DisplayName("Resume After Approval")
  class ResumeAfterApprovalTests {

    @Test
    @DisplayName("resume() continues execution after approval")
    void resumeAfterApprovalContinuesExecution() {
      DangerousTool dangerousTool = new DangerousTool();

      Agent agent =
              Agent.builder()
                      .name("TestAgent")
                      .instructions("Test instructions")
                      .model("test-model")
                      .responder(responder)
                      .addTool(dangerousTool)
                      .build();

      // First call: LLM requests dangerous tool
      mockWebServer.enqueue(
              new MockResponse().setResponseCode(200).setBody(buildToolCallResponse("dangerous_tool")));

      AgentResult pausedResult = agent.interact("Do something dangerous");
      assertTrue(pausedResult.isPaused());

      // Get paused state and approve
      AgentRunState state = pausedResult.pausedState();
      state.approveToolCall("Manager approved - executed successfully");

      // Enqueue response for after resume (final answer)
      mockWebServer.enqueue(
              new MockResponse()
                      .setResponseCode(200)
                      .setBody(buildTextResponse("Operation completed successfully!")));

      // Resume
      AgentResult finalResult = agent.resume(state);

      assertFalse(finalResult.isPaused(), "Should not be paused after resume");
      assertFalse(finalResult.isError(), "Should not have error");
      assertEquals("Operation completed successfully!", finalResult.output());
    }

    @Test
    @DisplayName("resume() continues execution after rejection")
    void resumeAfterRejectionContinuesWithError() {
      DangerousTool dangerousTool = new DangerousTool();

      Agent agent =
              Agent.builder()
                      .name("TestAgent")
                      .instructions("Test instructions")
                      .model("test-model")
                      .responder(responder)
                      .addTool(dangerousTool)
                      .build();

      // First call: LLM requests dangerous tool
      mockWebServer.enqueue(
              new MockResponse().setResponseCode(200).setBody(buildToolCallResponse("dangerous_tool")));

      AgentResult pausedResult = agent.interact("Do something dangerous");
      assertTrue(pausedResult.isPaused());

      // Get paused state and reject
      AgentRunState state = pausedResult.pausedState();
      state.rejectToolCall("User denied: too risky");

      // Enqueue response for after resume (LLM acknowledges rejection)
      mockWebServer.enqueue(
              new MockResponse()
                      .setResponseCode(200)
                      .setBody(buildTextResponse("I understand, the operation was cancelled.")));

      // Resume
      AgentResult finalResult = agent.resume(state);

      assertFalse(finalResult.isPaused(), "Should not be paused after resume");
      assertFalse(dangerousTool.executed, "Tool should NOT have been executed after rejection");
      assertEquals("I understand, the operation was cancelled.", finalResult.output());
    }
  }

  @Nested
  @DisplayName("Paused State Accessors")
  class PausedStateAccessorsTests {

    @Test
    @DisplayName("pausedState contains correct tool call info")
    void pausedStateContainsToolCallInfo() {
      DangerousTool dangerousTool = new DangerousTool();

      Agent agent =
              Agent.builder()
                      .name("TestAgent")
                      .instructions("Test instructions")
                      .model("test-model")
                      .responder(responder)
                      .addTool(dangerousTool)
                      .build();

      mockWebServer.enqueue(
              new MockResponse().setResponseCode(200).setBody(buildToolCallResponse("dangerous_tool")));

      AgentResult result = agent.interact("Do something");

      AgentRunState state = result.pausedState();
      assertNotNull(state);
      assertEquals("TestAgent", state.agentName());
      assertEquals(1, state.currentTurn());
      assertNotNull(state.context());

      FunctionToolCall pendingCall = state.pendingToolCall();
      assertNotNull(pendingCall);
      assertEquals("dangerous_tool", pendingCall.name());
      assertEquals("{\"value\": \"test\"}", pendingCall.arguments());
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTests {

    @Test
    @DisplayName("mixed safe and dangerous tools - pauses at first dangerous")
    void mixedToolsPausesAtFirstDangerous() {
      SafeTool safeTool = new SafeTool();
      DangerousTool dangerousTool = new DangerousTool();

      Agent agent =
              Agent.builder()
                      .name("TestAgent")
                      .instructions("Test instructions")
                      .model("test-model")
                      .responder(responder)
                      .addTool(safeTool)
                      .addTool(dangerousTool)
                      .build();

      // LLM responds with dangerous_tool call
      mockWebServer.enqueue(
              new MockResponse().setResponseCode(200).setBody(buildToolCallResponse("dangerous_tool")));

      AgentResult result = agent.interact("Do something");

      assertTrue(result.isPaused());
      assertFalse(dangerousTool.executed);
    }

    @Test
    @DisplayName("resume throws if state is not pending approval")
    void resumeThrowsIfNotPending() {
      Agent agent =
              Agent.builder()
                      .name("TestAgent")
                      .instructions("Test instructions")
                      .model("test-model")
                      .responder(responder)
                      .build();

      AgentRunState completedState =
              AgentRunState.completed("TestAgent", AgenticContext.create(), null, List.of(), 1);

      assertThrows(IllegalStateException.class, () -> agent.resume(completedState));
    }

    @Test
    @DisplayName("resume throws if no approval decision was made")
    void resumeThrowsIfNoDecision() {
      DangerousTool dangerousTool = new DangerousTool();

      Agent agent =
              Agent.builder()
                      .name("TestAgent")
                      .instructions("Test instructions")
                      .model("test-model")
                      .responder(responder)
                      .addTool(dangerousTool)
                      .build();

      mockWebServer.enqueue(
              new MockResponse().setResponseCode(200).setBody(buildToolCallResponse("dangerous_tool")));

      AgentResult result = agent.interact("Do something");
      AgentRunState state = result.pausedState();

      // Don't call approveToolCall or rejectToolCall
      assertThrows(IllegalStateException.class, () -> agent.resume(state));
    }
  }
}
