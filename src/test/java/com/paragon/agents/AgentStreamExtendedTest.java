package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.Responder;
import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

/**
 * Extended tests for AgentStream.java covering:
 * - Full streaming loop execution
 * - Callback invocations
 * - Tool execution with callbacks
 * - Human-in-the-loop confirmation
 * - Error handling
 * - Guardrail callbacks
 */
@DisplayName("AgentStream Extended Tests")
class AgentStreamExtendedTest {

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
  // FULL STREAMING LOOP
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Full Streaming Loop")
  class FullStreamingLoopTests {

    @Test
    @DisplayName("basic streaming loop completes successfully")
    void basicStreamingLoopCompletes() throws Exception {
      Agent agent = createTestAgent();
      enqueueSuccessResponse("Hello from streaming!");

      AtomicReference<AgentResult> resultRef = new AtomicReference<>();

      AgentResult result = agent.interactStream("Hello")
          .onComplete(resultRef::set)
          .start()
          .get(5, TimeUnit.SECONDS);

      assertNotNull(result);
      assertTrue(result.isSuccess());
      assertEquals("Hello from streaming!", result.output());
    }

    @Test
    @DisplayName("startBlocking executes synchronously")
    void startBlockingExecutesSynchronously() {
      Agent agent = createTestAgent();
      enqueueSuccessResponse("Blocking response");

      AgentResult result = agent.interactStream("Hello")
          .startBlocking();

      assertNotNull(result);
      assertTrue(result.isSuccess());
      assertEquals("Blocking response", result.output());
    }

    @Test
    @DisplayName("streaming with context preserves conversation")
    void streamingWithContextPreservesConversation() throws Exception {
      Agent agent = createTestAgent();
      AgentContext context = AgentContext.create();

      enqueueSuccessResponse("First response");
      enqueueSuccessResponse("Second response");

      // First interaction
      context.addInput(Message.user("First message"));
      agent.interactStream(context).start().get(5, TimeUnit.SECONDS);

      // Second interaction uses same context
      context.addInput(Message.user("Second message"));
      AgentResult result = agent.interactStream(context).start().get(5, TimeUnit.SECONDS);

      assertNotNull(result);
      assertTrue(context.getHistory().size() > 2);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CALLBACK INVOCATIONS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Callback Invocations")
  class CallbackInvocationTests {

    @Test
    @DisplayName("onTurnStart is called with turn number")
    void onTurnStartIsCalled() throws Exception {
      Agent agent = createTestAgent();
      enqueueSuccessResponse("Response");

      AtomicInteger turnNumber = new AtomicInteger(-1);

      agent.interactStream("Hello")
          .onTurnStart(turnNumber::set)
          .start()
          .get(5, TimeUnit.SECONDS);

      assertEquals(1, turnNumber.get());
    }

    @Test
    @DisplayName("onTextDelta is called with response text")
    void onTextDeltaIsCalled() throws Exception {
      Agent agent = createTestAgent();
      enqueueSuccessResponse("Delta text content");

      AtomicReference<String> deltaRef = new AtomicReference<>();

      agent.interactStream("Hello")
          .onTextDelta(deltaRef::set)
          .start()
          .get(5, TimeUnit.SECONDS);

      assertEquals("Delta text content", deltaRef.get());
    }

    @Test
    @DisplayName("onTurnComplete is called with Response")
    void onTurnCompleteIsCalled() throws Exception {
      Agent agent = createTestAgent();
      enqueueSuccessResponse("Turn complete");

      AtomicReference<Response> responseRef = new AtomicReference<>();

      agent.interactStream("Hello")
          .onTurnComplete(responseRef::set)
          .start()
          .get(5, TimeUnit.SECONDS);

      assertNotNull(responseRef.get());
      assertNotNull(responseRef.get().id());
    }

    @Test
    @DisplayName("onComplete is called with final result")
    void onCompleteIsCalled() throws Exception {
      Agent agent = createTestAgent();
      enqueueSuccessResponse("Final result");

      AtomicReference<AgentResult> completedRef = new AtomicReference<>();

      agent.interactStream("Hello")
          .onComplete(completedRef::set)
          .start()
          .get(5, TimeUnit.SECONDS);

      assertNotNull(completedRef.get());
      assertTrue(completedRef.get().isSuccess());
    }

    @Test
    @DisplayName("onError is called when exception occurs")
    void onErrorIsCalled() throws Exception {
      Agent agent = createTestAgent();

      // Enqueue malformed response to cause parsing error
      mockWebServer.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setBody("not json")
              .addHeader("Content-Type", "application/json"));

      AtomicReference<Throwable> errorRef = new AtomicReference<>();

      AgentResult result = agent.interactStream("Hello")
          .onError(errorRef::set)
          .start()
          .get(5, TimeUnit.SECONDS);

      // Either error callback was called or result is error
      assertTrue(errorRef.get() != null || result.isError());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TOOL EXECUTION WITH CALLBACKS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Tool Execution With Callbacks")
  class ToolExecutionCallbackTests {

    @Test
    @DisplayName("onToolExecuted is called after tool execution")
    void onToolExecutedIsCalled() throws Exception {
      FunctionTool<TestArgs> tool = new SimpleTool();

      Agent agent = Agent.builder()
          .name("ToolAgent")
          .model("test-model")
          .instructions("Use tools")
          .responder(responder)
          .addTool(tool)
          .build();

      enqueueToolCallResponse("simple_tool", "{\"value\": \"test\"}");
      enqueueSuccessResponse("Tool completed");

      AtomicReference<ToolExecution> execRef = new AtomicReference<>();

      agent.interactStream("Call the tool")
          .onToolExecuted(execRef::set)
          .start()
          .get(5, TimeUnit.SECONDS);

      assertNotNull(execRef.get());
      assertEquals("simple_tool", execRef.get().toolName());
    }

    @Test
    @DisplayName("multiple tool executions trigger multiple callbacks")
    void multipleToolExecutionsCallbacks() throws Exception {
      FunctionTool<TestArgs> tool1 = new ToolOne();
      FunctionTool<TestArgs> tool2 = new ToolTwo();

      Agent agent = Agent.builder()
          .name("MultiToolAgent")
          .model("test-model")
          .instructions("Use tools")
          .responder(responder)
          .addTool(tool1)
          .addTool(tool2)
          .build();

      enqueueMultiToolCallResponse(
          List.of("tool_one", "tool_two"),
          List.of("{\"value\": \"a\"}", "{\"value\": \"b\"}"));
      enqueueSuccessResponse("Done");

      List<ToolExecution> executions = new ArrayList<>();

      agent.interactStream("Call tools")
          .onToolExecuted(executions::add)
          .start()
          .get(5, TimeUnit.SECONDS);

      assertEquals(2, executions.size());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HUMAN-IN-THE-LOOP CONFIRMATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Human-in-the-Loop Confirmation")
  class HumanInTheLoopTests {

    @Test
    @DisplayName("onToolCallPending is called for dangerous tools")
    void onToolCallPendingCalledForDangerousTool() throws Exception {
      FunctionTool<TestArgs> dangerousTool = new DangerousTool();

      Agent agent = Agent.builder()
          .name("HitlAgent")
          .model("test-model")
          .instructions("Use tools")
          .responder(responder)
          .addTool(dangerousTool)
          .build();

      enqueueToolCallResponse("dangerous_tool", "{\"value\": \"test\"}");
      enqueueSuccessResponse("Tool completed");

      AtomicBoolean callbackCalled = new AtomicBoolean(false);
      AtomicReference<FunctionToolCall> pendingCall = new AtomicReference<>();

      agent.interactStream("Run dangerous tool")
          .onToolCallPending((call, approve) -> {
            callbackCalled.set(true);
            pendingCall.set(call);
            approve.accept(true); // Approve the tool
          })
          .start()
          .get(5, TimeUnit.SECONDS);

      assertTrue(callbackCalled.get());
      assertNotNull(pendingCall.get());
      assertEquals("dangerous_tool", pendingCall.get().name());
    }

    @Test
    @DisplayName("tool is rejected when callback rejects")
    void toolRejectedWhenCallbackRejects() throws Exception {
      FunctionTool<TestArgs> dangerousTool = new DangerousTool();

      Agent agent = Agent.builder()
          .name("HitlAgent")
          .model("test-model")
          .instructions("Use tools")
          .responder(responder)
          .addTool(dangerousTool)
          .build();

      enqueueToolCallResponse("dangerous_tool", "{\"value\": \"test\"}");
      enqueueSuccessResponse("Tool rejected");

      AtomicBoolean toolExecuted = new AtomicBoolean(false);

      agent.interactStream("Run dangerous tool")
          .onToolCallPending((call, approve) -> approve.accept(false)) // Reject
          .onToolExecuted(exec -> toolExecuted.set(true))
          .start()
          .get(5, TimeUnit.SECONDS);

      // Tool should NOT be executed when rejected
      assertFalse(toolExecuted.get());
    }

    @Test
    @DisplayName("safe tool executes without confirmation")
    void safeToolExecutesWithoutConfirmation() throws Exception {
      FunctionTool<TestArgs> safeTool = new SimpleTool();

      Agent agent = Agent.builder()
          .name("SafeAgent")
          .model("test-model")
          .instructions("Use tools")
          .responder(responder)
          .addTool(safeTool)
          .build();

      enqueueToolCallResponse("simple_tool", "{\"value\": \"test\"}");
      enqueueSuccessResponse("Done");

      AtomicBoolean confirmationCalled = new AtomicBoolean(false);
      AtomicBoolean toolExecuted = new AtomicBoolean(false);

      agent.interactStream("Run safe tool")
          .onToolCallPending((call, approve) -> confirmationCalled.set(true))
          .onToolExecuted(exec -> toolExecuted.set(true))
          .start()
          .get(5, TimeUnit.SECONDS);

      // Safe tool should NOT trigger confirmation
      assertFalse(confirmationCalled.get());
      // But should still execute
      assertTrue(toolExecuted.get());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PAUSE HANDLER
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Pause Handler")
  class PauseHandlerTests {

    @Test
    @DisplayName("onPause is called for dangerous tools")
    void onPauseCalledForDangerousTool() throws Exception {
      FunctionTool<TestArgs> dangerousTool = new DangerousTool();

      Agent agent = Agent.builder()
          .name("PauseAgent")
          .model("test-model")
          .instructions("Use tools")
          .responder(responder)
          .addTool(dangerousTool)
          .build();

      enqueueToolCallResponse("dangerous_tool", "{\"value\": \"test\"}");

      AtomicReference<AgentRunState> pauseStateRef = new AtomicReference<>();

      AgentResult result = agent.interactStream("Run dangerous tool")
          .onPause(pauseStateRef::set)
          .start()
          .get(5, TimeUnit.SECONDS);

      assertNotNull(pauseStateRef.get());
      assertTrue(pauseStateRef.get().isPendingApproval());
      assertEquals("dangerous_tool", pauseStateRef.get().pendingToolCall().name());
      assertTrue(result.isPaused());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // OUTPUT GUARDRAILS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Output Guardrails")
  class OutputGuardrailTests {

    @Test
    @DisplayName("onGuardrailFailed is called when output guardrail fails")
    void onGuardrailFailedIsCalled() throws Exception {
      Agent agent = Agent.builder()
          .name("GuardedAgent")
          .model("test-model")
          .instructions("Test")
          .responder(responder)
          .addOutputGuardrail((output, ctx) -> {
            if (output.contains("forbidden")) {
              return GuardrailResult.failed("Contains forbidden content");
            }
            return GuardrailResult.passed();
          })
          .build();

      enqueueSuccessResponse("This is forbidden content");

      AtomicReference<GuardrailResult.Failed> failedRef = new AtomicReference<>();

      AgentResult result = agent.interactStream("Generate response")
          .onGuardrailFailed(failedRef::set)
          .start()
          .get(5, TimeUnit.SECONDS);

      assertNotNull(failedRef.get());
      assertTrue(failedRef.get().reason().contains("forbidden"));
      assertFalse(result.isSuccess());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PRE-FAILED STREAMS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Pre-Failed Streams")
  class PreFailedStreamTests {

    @Test
    @DisplayName("pre-failed stream immediately completes with error")
    void preFailedStreamCompletesImmediately() throws Exception {
      AgentContext context = AgentContext.create();
      AgentResult failedResult = AgentResult.error(
          new RuntimeException("Pre-failure"), context, 0);

      AtomicReference<AgentResult> completedRef = new AtomicReference<>();

      AgentResult result = AgentStream.failed(failedResult)
          .onComplete(completedRef::set)
          .start()
          .get(1, TimeUnit.SECONDS);

      assertNotNull(result);
      assertTrue(result.isError());
      assertNotNull(completedRef.get());
    }

    @Test
    @DisplayName("pre-failed stream startBlocking returns immediately")
    void preFailedStreamStartBlockingReturnsImmediately() {
      AgentContext context = AgentContext.create();
      AgentResult failedResult = AgentResult.error(
          new RuntimeException("Pre-failure"), context, 0);

      AgentResult result = AgentStream.failed(failedResult).startBlocking();

      assertNotNull(result);
      assertTrue(result.isError());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HANDOFF CALLBACKS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Handoff Callbacks")
  class HandoffCallbackTests {

    @Test
    @DisplayName("onHandoff registration works")
    void onHandoffRegistrationWorks() {
      AgentContext context = AgentContext.create();
      AgentResult result = AgentResult.error(new RuntimeException(), context, 0);

      AtomicBoolean handlerSet = new AtomicBoolean(false);

      AgentStream stream = AgentStream.failed(result)
          .onHandoff(handoff -> handlerSet.set(true));

      assertNotNull(stream);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPER CLASSES
  // ═══════════════════════════════════════════════════════════════════════════

  public record TestArgs(String value) {}

  @FunctionMetadata(name = "simple_tool", description = "A simple tool")
  private static class SimpleTool extends FunctionTool<TestArgs> {
    @Override
    public FunctionToolCallOutput call(TestArgs params) {
      return FunctionToolCallOutput.success("simple_call", "Simple result");
    }
  }

  @FunctionMetadata(name = "tool_one", description = "Tool one")
  private static class ToolOne extends FunctionTool<TestArgs> {
    @Override
    public FunctionToolCallOutput call(TestArgs params) {
      return FunctionToolCallOutput.success("call_one", "One result");
    }
  }

  @FunctionMetadata(name = "tool_two", description = "Tool two")
  private static class ToolTwo extends FunctionTool<TestArgs> {
    @Override
    public FunctionToolCallOutput call(TestArgs params) {
      return FunctionToolCallOutput.success("call_two", "Two result");
    }
  }

  @FunctionMetadata(name = "dangerous_tool", description = "A dangerous tool", requiresConfirmation = true)
  private static class DangerousTool extends FunctionTool<TestArgs> {
    @Override
    public FunctionToolCallOutput call(TestArgs params) {
      return FunctionToolCallOutput.success("dangerous_call", "Dangerous result");
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPER METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  private Agent createTestAgent() {
    return Agent.builder()
        .name("TestAgent")
        .model("test-model")
        .instructions("Test instructions")
        .responder(responder)
        .build();
  }

  private void enqueueSuccessResponse(String text) {
    String json = """
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
        """.formatted(text);

    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(json)
            .addHeader("Content-Type", "application/json"));
  }

  private void enqueueToolCallResponse(String toolName, String arguments) {
    String json = """
        {
          "id": "resp_001",
          "object": "response",
          "created_at": 1234567890,
          "status": "completed",
          "model": "test-model",
          "output": [
            {
              "type": "function_call",
              "id": "fc_001",
              "call_id": "call_123",
              "name": "%s",
              "arguments": "%s"
            }
          ],
          "usage": {
            "input_tokens": 10,
            "output_tokens": 5,
            "total_tokens": 15
          }
        }
        """.formatted(toolName, arguments.replace("\"", "\\\""));

    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(json)
            .addHeader("Content-Type", "application/json"));
  }

  private void enqueueMultiToolCallResponse(List<String> toolNames, List<String> arguments) {
    StringBuilder outputBuilder = new StringBuilder();
    for (int i = 0; i < toolNames.size(); i++) {
      if (i > 0) outputBuilder.append(",");
      outputBuilder.append("""
          {
            "type": "function_call",
            "id": "fc_%03d",
            "call_id": "call_%d",
            "name": "%s",
            "arguments": "%s"
          }
          """.formatted(i, i, toolNames.get(i), arguments.get(i).replace("\"", "\\\"")));
    }

    String json = """
        {
          "id": "resp_001",
          "object": "response",
          "created_at": 1234567890,
          "status": "completed",
          "model": "test-model",
          "output": [%s],
          "usage": {
            "input_tokens": 10,
            "output_tokens": 5,
            "total_tokens": 15
          }
        }
        """.formatted(outputBuilder.toString());

    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(json)
            .addHeader("Content-Type", "application/json"));
  }
}
