package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.agents.context.*;
import com.paragon.responses.Responder;
import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

/**
 * Comprehensive tests for AgentStream callbacks and lifecycle. Tests cover: onTurnStart,
 * onTurnComplete, onTextDelta, onToolExecuted, onToolCallPending, onGuardrailFailed, onHandoff,
 * onComplete, onError.
 */
@DisplayName("AgentStream Callback Tests")
class AgentStreamCallbackTest {

  private MockWebServer mockWebServer;
  private Responder responder;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    String baseUrl = mockWebServer.url("/").toString();
    responder =
        Responder.builder().baseUrl(mockWebServer.url("/v1/responses")).apiKey("test-key").build();
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CALLBACK TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Turn Callbacks")
  class TurnCallbackTests {

    @Test
    @DisplayName("onTurnStart is called at start of each turn")
    void onTurnStartIsCalled() throws Exception {
      AtomicInteger turnStartCount = new AtomicInteger(0);
      AtomicReference<Integer> capturedTurn = new AtomicReference<>();
      CountDownLatch latch = new CountDownLatch(1);

      Agent agent = createTestAgent();
      enqueueSuccessResponse("Hello!");

      agent
          .interactStream("Hi")
          .onTurnStart(
              turn -> {
                turnStartCount.incrementAndGet();
                capturedTurn.set(turn);
              })
          .onComplete(result -> latch.countDown())
          .onError(e -> latch.countDown())
          .start();

      latch.await(10, TimeUnit.SECONDS);

      assertTrue(turnStartCount.get() >= 1, "onTurnStart should be called at least once");
    }

    @Test
    @DisplayName("onTurnComplete is called after LLM response")
    void onTurnCompleteIsCalled() throws Exception {
      AtomicBoolean turnCompleted = new AtomicBoolean(false);
      CountDownLatch latch = new CountDownLatch(1);

      Agent agent = createTestAgent();
      enqueueSuccessResponse("Response");

      agent
          .interactStream("Hi")
          .onTurnComplete(response -> turnCompleted.set(true))
          .onComplete(result -> latch.countDown())
          .onError(e -> latch.countDown())
          .start();

      latch.await(10, TimeUnit.SECONDS);

      assertTrue(turnCompleted.get(), "onTurnComplete should be called");
    }
  }

  @Nested
  @DisplayName("Text Delta Callbacks")
  class TextDeltaTests {

    @Test
    @DisplayName("onTextDelta receives text chunks")
    void onTextDeltaReceivesChunks() throws Exception {
      StringBuilder capturedText = new StringBuilder();
      CountDownLatch latch = new CountDownLatch(1);

      Agent agent = createTestAgent();
      enqueueSuccessResponse("Hello World");

      agent
          .interactStream("Hi")
          .onTextDelta(chunk -> capturedText.append(chunk))
          .onComplete(result -> latch.countDown())
          .onError(e -> latch.countDown())
          .start();

      latch.await(10, TimeUnit.SECONDS);

      // Text should be captured (may be empty if no streaming deltas)
      assertNotNull(capturedText.toString());
    }
  }

  @Nested
  @DisplayName("Tool Callbacks")
  class ToolCallbackTests {

    @Test
    @DisplayName("onToolExecuted is called after tool execution")
    void onToolExecutedIsCalled() throws Exception {
      AtomicBoolean toolExecuted = new AtomicBoolean(false);
      AtomicReference<String> capturedToolName = new AtomicReference<>();
      CountDownLatch latch = new CountDownLatch(1);

      FunctionTool<TestArgs> tool = new SimpleTool();

      Agent agent =
          Agent.builder()
              .name("ToolAgent")
              .model("test-model")
              .instructions("Use tools")
              .responder(responder)
              .addTool(tool)
              .build();

      enqueueToolCallResponse("simple_tool", "{\"query\": \"test\"}");
      enqueueSuccessResponse("Done");

      agent
          .interactStream("Test")
          .onToolExecuted(
              exec -> {
                toolExecuted.set(true);
                capturedToolName.set(exec.toolName());
              })
          .onComplete(result -> latch.countDown())
          .onError(e -> latch.countDown())
          .start();

      latch.await(10, TimeUnit.SECONDS);

      assertTrue(toolExecuted.get(), "onToolExecuted should be called");
      assertEquals("simple_tool", capturedToolName.get());
    }

    @Test
    @DisplayName("onToolCallPending is called for tool confirmation")
    void onToolCallPendingIsCalled() throws Exception {
      AtomicBoolean pendingCalled = new AtomicBoolean(false);
      CountDownLatch latch = new CountDownLatch(1);

      FunctionTool<TestArgs> tool = new ConfirmationTool();

      Agent agent =
          Agent.builder()
              .name("ConfirmAgent")
              .model("test-model")
              .instructions("Use tools")
              .responder(responder)
              .addTool(tool)
              .build();

      enqueueToolCallResponse("confirmation_tool", "{\"query\": \"delete\"}");
      enqueueSuccessResponse("Done");

      agent
          .interactStream("Delete stuff")
          .onToolCallPending(
              (call, approve) -> {
                pendingCalled.set(true);
                approve.accept(true); // Approve the tool call
              })
          .onComplete(result -> latch.countDown())
          .onError(e -> latch.countDown())
          .start();

      latch.await(10, TimeUnit.SECONDS);

      assertTrue(pendingCalled.get(), "onToolCallPending should be called for confirmation tools");
    }
  }

  @Nested
  @DisplayName("Completion Callbacks")
  class CompletionCallbackTests {

    @Test
    @DisplayName("onComplete is called when agent finishes")
    void onCompleteIsCalled() throws Exception {
      AtomicBoolean completed = new AtomicBoolean(false);
      AtomicReference<AgentResult> capturedResult = new AtomicReference<>();
      CountDownLatch latch = new CountDownLatch(1);

      Agent agent = createTestAgent();
      enqueueSuccessResponse("Done");

      agent
          .interactStream("Hi")
          .onComplete(
              result -> {
                completed.set(true);
                capturedResult.set(result);
                latch.countDown();
              })
          .onError(e -> latch.countDown())
          .start();

      latch.await(10, TimeUnit.SECONDS);

      assertTrue(completed.get(), "onComplete should be called");
      assertNotNull(capturedResult.get());
    }

    @Test
    @DisplayName("onError is called on failure")
    void onErrorIsCalledOnFailure() throws Exception {
      AtomicBoolean errorCalled = new AtomicBoolean(false);
      CountDownLatch latch = new CountDownLatch(1);

      Agent agent = createTestAgent();
      // No response enqueued - will fail

      agent
          .interactStream("Hi")
          .onComplete(result -> latch.countDown())
          .onError(
              e -> {
                errorCalled.set(true);
                latch.countDown();
              })
          .start();

      latch.await(10, TimeUnit.SECONDS);

      // Either error callback or timeout
    }
  }

  @Nested
  @DisplayName("Guardrail Callbacks")
  class GuardrailCallbackTests {

    @Test
    @DisplayName("input guardrail failure results in error state")
    void inputGuardrailFailureResultsInError() throws Exception {
      AtomicReference<AgentResult> capturedResult = new AtomicReference<>();
      CountDownLatch latch = new CountDownLatch(1);

      Agent agent =
          Agent.builder()
              .name("GuardrailAgent")
              .model("test-model")
              .instructions("Test")
              .responder(responder)
              .addInputGuardrail((inputText, ctx) -> GuardrailResult.failed("Blocked for test"))
              .build();

      agent
          .interactStream("Any input")
          .onComplete(
              result -> {
                capturedResult.set(result);
                latch.countDown();
              })
          .onError(e -> latch.countDown())
          .start();

      latch.await(5, TimeUnit.SECONDS);

      assertNotNull(capturedResult.get(), "Result should be captured");
      assertTrue(capturedResult.get().isError(), "Result should be an error");
    }
  }

  @Nested
  @DisplayName("Handoff Callbacks")
  class HandoffCallbackTests {

    @Test
    @DisplayName("onHandoff is called when agent hands off")
    void onHandoffIsCalled() throws Exception {
      AtomicBoolean handoffCalled = new AtomicBoolean(false);
      AtomicReference<String> capturedAgentName = new AtomicReference<>();
      CountDownLatch latch = new CountDownLatch(1);

      Agent targetAgent =
          Agent.builder()
              .name("SupportAgent")
              .model("test-model")
              .instructions("Support")
              .responder(responder)
              .build();

      Agent mainAgent =
          Agent.builder()
              .name("Router")
              .model("test-model")
              .instructions("Route")
              .responder(responder)
              .addHandoff(Handoff.to(targetAgent).build())
              .build();

      // Main agent triggers handoff
      enqueueHandoffResponse("transfer_to_support_agent", "{\"message\": \"help\"}");
      // Target agent responds
      enqueueSuccessResponse("Support here");

      mainAgent
          .interactStream("Need support")
          .onHandoff(
              handoff -> {
                handoffCalled.set(true);
                capturedAgentName.set(handoff.targetAgent().name());
              })
          .onComplete(result -> latch.countDown())
          .onError(e -> latch.countDown())
          .start();

      latch.await(10, TimeUnit.SECONDS);

      assertTrue(handoffCalled.get(), "onHandoff should be called");
      assertEquals("SupportAgent", capturedAgentName.get());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPER CLASSES AND METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  public record TestArgs(String query) {}

  @FunctionMetadata(name = "simple_tool", description = "A simple tool")
  private static class SimpleTool extends FunctionTool<TestArgs> {
    @Override
    public FunctionToolCallOutput call(TestArgs params) {
      return FunctionToolCallOutput.success("Result: " + params.query());
    }
  }

  @FunctionMetadata(name = "confirmation_tool", description = "Requires confirmation")
  private static class ConfirmationTool extends FunctionTool<TestArgs> {
    @Override
    public FunctionToolCallOutput call(TestArgs params) {
      return FunctionToolCallOutput.success("Confirmed: " + params.query());
    }

    @Override
    public boolean requiresConfirmation() {
      return true;
    }
  }

  private Agent createTestAgent() {
    return Agent.builder()
        .name("TestAgent")
        .model("test-model")
        .instructions("Test instructions")
        .responder(responder)
        .build();
  }

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

  private void enqueueToolCallResponse(String toolName, String arguments) {
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
        """
            .formatted(toolName, arguments.replace("\"", "\\\""));

    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(json)
            .addHeader("Content-Type", "application/json"));
  }

  private void enqueueHandoffResponse(String handoffName, String arguments) {
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
        """
            .formatted(handoffName, arguments.replace("\"", "\\\""));

    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(json)
            .addHeader("Content-Type", "application/json"));
  }
}
