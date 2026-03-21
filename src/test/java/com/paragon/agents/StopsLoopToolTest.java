package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.Responder;
import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCall;
import com.paragon.responses.spec.FunctionToolCallOutput;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

/**
 * Tests for {@code stopsLoop = true} client-side tool support.
 *
 * <p>Verifies that when a tool annotated with {@code @FunctionMetadata(stopsLoop = true)} is called
 * by the model:
 *
 * <ol>
 *   <li>The agentic loop exits immediately with {@code AgentResult.isClientSideTool() == true}
 *   <li>The {@code FunctionToolCall} is NOT persisted to conversation history
 *   <li>The tool's {@code call()} method is never invoked
 * </ol>
 */
@DisplayName("StopsLoop Tool Support")
class StopsLoopToolTest {

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

  // ═══════════════════════════════════════════════════════════════════════════
  // Test fixtures
  // ═══════════════════════════════════════════════════════════════════════════

  record AskUserParams(String question) {}

  @FunctionMetadata(
      name = "ask_user",
      description = "Ask the user a clarifying question",
      stopsLoop = true)
  static class AskUserTool extends FunctionTool<AskUserParams> {
    boolean executed = false;

    @Override
    public FunctionToolCallOutput call(AskUserParams params) {
      executed = true; // must never be reached
      return FunctionToolCallOutput.success("ask_user_call", "shouldn't happen");
    }
  }

  record SimpleParams(String value) {}

  @FunctionMetadata(name = "normal_tool", description = "A normal tool")
  static class NormalTool extends FunctionTool<SimpleParams> {
    boolean executed = false;

    @Override
    public FunctionToolCallOutput call(SimpleParams params) {
      executed = true;
      return FunctionToolCallOutput.success("normal_call", "result: " + params.value());
    }
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
          "arguments": "{\\"question\\": \\"What color do you prefer?\\"}"
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

  // ═══════════════════════════════════════════════════════════════════════════
  // Unit: FunctionTool.stopsLoop()
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("FunctionTool.stopsLoop()")
  class FunctionToolStopsLoopTests {

    @Test
    @DisplayName("returns true when annotated with stopsLoop = true")
    void returnsTrueWhenAnnotated() {
      AskUserTool tool = new AskUserTool();
      assertTrue(tool.stopsLoop());
    }

    @Test
    @DisplayName("returns false by default")
    void returnsFalseByDefault() {
      NormalTool tool = new NormalTool();
      assertFalse(tool.stopsLoop());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Unit: AgentResult.isClientSideTool()
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("AgentResult.isClientSideTool()")
  class AgentResultClientSideToolTests {

    @Test
    @DisplayName("isClientSideTool() returns false on success result")
    void isFalseOnSuccess() {
      AgentResult result = AgentResult.success("Hello");
      assertFalse(result.isClientSideTool());
      assertNull(result.clientSideToolCall());
    }

    @Test
    @DisplayName("isClientSideTool() returns false on error result")
    void isFalseOnError() {
      AgentResult result = AgentResult.error(new RuntimeException("oops"));
      assertFalse(result.isClientSideTool());
    }

    @Test
    @DisplayName("isSuccess() returns true on clientSideTool result (no error field)")
    void isSuccessOnClientSideTool() {
      AgenticContext ctx = AgenticContext.create();
      FunctionToolCall call =
          new FunctionToolCall("{\"question\":\"hi\"}", "call_x", "ask_user", "fc_x", null);
      AgentResult result = AgentResult.clientSideTool(call, ctx, 1);
      assertTrue(result.isClientSideTool());
      assertNotNull(result.clientSideToolCall());
      assertEquals("ask_user", result.clientSideToolCall().name());
      assertTrue(result.isSuccess(), "clientSideTool should not set the error field");
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Integration: Agent.interact() with stopsLoop tool
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Agent.interact() with stopsLoop tool")
  class AgentInteractStopsLoopTests {

    @Test
    @DisplayName("result.isClientSideTool() is true when model calls stopsLoop tool")
    void isClientSideToolTrue() {
      AskUserTool askTool = new AskUserTool();
      mockWebServer.enqueue(
          new MockResponse()
              .setBody(buildToolCallResponse("ask_user"))
              .addHeader("Content-Type", "application/json"));

      Agent agent =
          Agent.builder()
              .name("TestAgent")
              .model("test-model")
              .instructions("You are helpful")
              .responder(responder)
              .addTool(askTool)
              .build();

      AgentResult result = agent.interact("Help me pick a color");

      assertTrue(result.isClientSideTool());
    }

    @Test
    @DisplayName("result.clientSideToolCall().name() equals the tool name")
    void clientSideToolCallNameMatches() {
      AskUserTool askTool = new AskUserTool();
      mockWebServer.enqueue(
          new MockResponse()
              .setBody(buildToolCallResponse("ask_user"))
              .addHeader("Content-Type", "application/json"));

      Agent agent =
          Agent.builder()
              .name("TestAgent")
              .model("test-model")
              .instructions("You are helpful")
              .responder(responder)
              .addTool(askTool)
              .build();

      AgentResult result = agent.interact("Help me pick a color");

      assertNotNull(result.clientSideToolCall());
      assertEquals("ask_user", result.clientSideToolCall().name());
    }

    @Test
    @DisplayName("history contains no FunctionToolCall items after stopsLoop exit")
    void historyHasNoFunctionToolCallItems() {
      AskUserTool askTool = new AskUserTool();
      mockWebServer.enqueue(
          new MockResponse()
              .setBody(buildToolCallResponse("ask_user"))
              .addHeader("Content-Type", "application/json"));

      Agent agent =
          Agent.builder()
              .name("TestAgent")
              .model("test-model")
              .instructions("You are helpful")
              .responder(responder)
              .addTool(askTool)
              .build();

      AgentResult result = agent.interact("Help me pick a color");

      boolean hasFunctionToolCall =
          result.history().stream().anyMatch(item -> item instanceof FunctionToolCall);
      assertFalse(
          hasFunctionToolCall, "FunctionToolCall must NOT appear in history for stopsLoop tools");
    }

    @Test
    @DisplayName("tool call() method is never invoked")
    void toolCallMethodNeverInvoked() {
      AskUserTool askTool = new AskUserTool();
      mockWebServer.enqueue(
          new MockResponse()
              .setBody(buildToolCallResponse("ask_user"))
              .addHeader("Content-Type", "application/json"));

      Agent agent =
          Agent.builder()
              .name("TestAgent")
              .model("test-model")
              .instructions("You are helpful")
              .responder(responder)
              .addTool(askTool)
              .build();

      agent.interact("Help me pick a color");

      assertFalse(askTool.executed, "Tool call() must not be invoked for stopsLoop tools");
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Integration: AgentStream with stopsLoop tool
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("AgentStream with stopsLoop tool")
  class AgentStreamStopsLoopTests {

    @Test
    @DisplayName("onClientSideTool fires and onComplete receives clientSideTool result")
    void onClientSideToolFires() {
      AskUserTool askTool = new AskUserTool();
      mockWebServer.enqueue(
          new MockResponse()
              .setBody(buildToolCallResponse("ask_user"))
              .addHeader("Content-Type", "application/json"));

      Agent agent =
          Agent.builder()
              .name("TestAgent")
              .model("test-model")
              .instructions("You are helpful")
              .responder(responder)
              .addTool(askTool)
              .build();

      AtomicReference<FunctionToolCall> capturedCall = new AtomicReference<>();
      AtomicReference<AgentResult> capturedResult = new AtomicReference<>();

      agent
          .asStreaming()
          .interact("Help me pick a color")
          .onClientSideTool(capturedCall::set)
          .onComplete(capturedResult::set)
          .startBlocking();

      assertNotNull(capturedCall.get(), "onClientSideTool should have fired");
      assertEquals("ask_user", capturedCall.get().name());
      assertNotNull(capturedResult.get());
      assertTrue(capturedResult.get().isClientSideTool());
      assertFalse(askTool.executed, "tool call() must not run for stopsLoop tools");
    }

    @Test
    @DisplayName("streaming history contains no FunctionToolCall for stopsLoop tools")
    void streamingHistoryHasNoFunctionToolCall() {
      AskUserTool askTool = new AskUserTool();
      mockWebServer.enqueue(
          new MockResponse()
              .setBody(buildToolCallResponse("ask_user"))
              .addHeader("Content-Type", "application/json"));

      Agent agent =
          Agent.builder()
              .name("TestAgent")
              .model("test-model")
              .instructions("You are helpful")
              .responder(responder)
              .addTool(askTool)
              .build();

      AtomicReference<AgentResult> capturedResult = new AtomicReference<>();

      agent
          .asStreaming()
          .interact("Help me pick a color")
          .onComplete(capturedResult::set)
          .startBlocking();

      assertNotNull(capturedResult.get());
      boolean hasFunctionToolCall =
          capturedResult.get().history().stream()
              .anyMatch(item -> item instanceof FunctionToolCall);
      assertFalse(hasFunctionToolCall, "FunctionToolCall must NOT appear in streaming history");
    }
  }
}
