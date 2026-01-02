package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.Responder;
import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.json.JacksonJsonSchemaProducer;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import com.paragon.responses.spec.FunctionToolFactory;
import com.paragon.responses.spec.FunctionToolStore;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Agent with function tools.
 *
 * <p>Tests cover: - Tool registration and retrieval - Tool call execution - Tool results handling -
 * Tool errors
 */
@DisplayName("Agent Tool Integration Tests")
class AgentToolIntegrationTest {

  private MockWebServer mockWebServer;
  private Responder responder;
  private ObjectMapper objectMapper;
  private FunctionToolFactory toolFactory;
  private FunctionToolStore toolStore;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    responder =
        Responder.builder().openRouter().apiKey("test-key").baseUrl(mockWebServer.url("/")).build();

    objectMapper = new ObjectMapper();
    toolFactory = FunctionToolFactory.withProducer(new JacksonJsonSchemaProducer(objectMapper));
    toolStore = FunctionToolStore.create(objectMapper);
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TOOL REGISTRATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Tool Registration")
  class ToolRegistration {

    @Test
    @DisplayName("agent with tool is created successfully")
    void agentWithTool_createdSuccessfully() {
      FunctionTool<AddParams> addTool = toolFactory.create(AddTool.class);

      Agent agent =
          Agent.builder()
              .name("Calculator")
              .instructions("You are a calculator.")
              .model("test-model")
              .responder(responder)
              .addTool(addTool)
              .build();

      assertNotNull(agent);
      assertNotNull(agent.toolStore());
    }

    @Test
    @DisplayName("multiple tools can be registered")
    void multipleTools_canBeRegistered() {
      FunctionTool<AddParams> addTool = toolFactory.create(AddTool.class);
      FunctionTool<MultiplyParams> multiplyTool = toolFactory.create(MultiplyTool.class);

      Agent agent =
          Agent.builder()
              .name("Calculator")
              .instructions("You are a calculator.")
              .model("test-model")
              .responder(responder)
              .addTool(addTool)
              .addTool(multiplyTool)
              .build();

      assertNotNull(agent.toolStore());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TOOL EXECUTION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Tool Execution")
  class ToolExecution {

    @Test
    @DisplayName("agent responds without tool call")
    void agent_respondsWithoutToolCall() {
      FunctionTool<AddParams> addTool = toolFactory.create(AddTool.class);

      Agent agent =
          Agent.builder()
              .name("Calculator")
              .instructions("You are a calculator. Only use tools when math is needed.")
              .model("test-model")
              .responder(responder)
              .addTool(addTool)
              .build();

      enqueueSuccessResponse("Hello! I'm a calculator. How can I help?");

      AgentResult result = agent.interact("Hi there!").join();

      assertNotNull(result.output());
      assertFalse(result.isError());
    }

    @Test
    @DisplayName("agent executes tool when LLM calls it")
    void agent_executesToolWhenCalled() {
      FunctionTool<AddParams> addTool = toolFactory.create(AddTool.class);
      toolStore.add(addTool);

      Agent agent =
          Agent.builder()
              .name("Calculator")
              .instructions("You are a calculator.")
              .model("test-model")
              .responder(responder)
              .addTool(addTool)
              .build();

      // First response: LLM wants to call the tool
      enqueueToolCallResponse("add", "{\"a\": 2, \"b\": 3}");
      // Second response: LLM provides final answer after tool result
      enqueueSuccessResponse("The result is 5.");

      AgentResult result = agent.interact("What is 2 + 3?").join();

      assertNotNull(result.output());
      assertTrue(result.output().contains("5"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TOOL RESULTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Tool Results")
  class ToolResults {

    @Test
    @DisplayName("tool executions are tracked in result")
    void toolExecutions_trackedInResult() {
      FunctionTool<AddParams> addTool = toolFactory.create(AddTool.class);

      Agent agent =
          Agent.builder()
              .name("Calculator")
              .instructions("You are a calculator.")
              .model("test-model")
              .responder(responder)
              .addTool(addTool)
              .build();

      enqueueToolCallResponse("add", "{\"a\": 1, \"b\": 2}");
      enqueueSuccessResponse("The answer is 3.");

      AgentResult result = agent.interact("What is 1 + 2?").join();

      // Tool executions should be tracked
      assertNotNull(result.toolExecutions());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MULTI-TURN TOOL EXECUTION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Multi-Turn Tool Execution")
  class MultiTurnToolExecution {

    @Test
    @DisplayName("agent handles multiple sequential tool calls")
    void agent_handlesMultipleSequentialToolCalls() {
      FunctionTool<AddParams> addTool = toolFactory.create(AddTool.class);

      Agent agent =
          Agent.builder()
              .name("Calculator")
              .instructions("You are a calculator.")
              .model("test-model")
              .responder(responder)
              .addTool(addTool)
              .build();

      // First tool call
      enqueueToolCallResponse("add", "{\"a\": 10, \"b\": 5}");
      // Second tool call
      enqueueToolCallResponse("add", "{\"a\": 15, \"b\": 7}");
      // Final response
      enqueueSuccessResponse("Results: 15 and 22");

      AgentResult result = agent.interact("Calculate 10+5 and 15+7").join();

      assertTrue(result.isSuccess());
      assertNotNull(result.toolExecutions());
      assertEquals(2, result.toolExecutions().size());
    }

    @Test
    @DisplayName("agent uses different tools in sequence")
    void agent_usesDifferentToolsInSequence() {
      FunctionTool<AddParams> addTool = toolFactory.create(AddTool.class);
      FunctionTool<MultiplyParams> multiplyTool = toolFactory.create(MultiplyTool.class);

      Agent agent =
          Agent.builder()
              .name("Calculator")
              .instructions("You are a calculator.")
              .model("test-model")
              .responder(responder)
              .addTool(addTool)
              .addTool(multiplyTool)
              .build();

      // First: add
      enqueueToolCallResponse("add", "{\"a\": 2, \"b\": 3}");
      // Second: multiply
      enqueueToolCallResponse("multiply", "{\"a\": 5, \"b\": 4}");
      // Final response
      enqueueSuccessResponse("2+3=5, then 5*4=20");

      AgentResult result = agent.interact("Add 2+3, then multiply by 4").join();

      assertTrue(result.isSuccess());
      assertEquals(2, result.toolExecutions().size());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MAX TURNS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Max Turns")
  class MaxTurns {

    @Test
    @DisplayName("agent stops after max turns exceeded")
    void agent_stopsAfterMaxTurnsExceeded() {
      FunctionTool<AddParams> addTool = toolFactory.create(AddTool.class);

      Agent agent =
          Agent.builder()
              .name("LoopingAgent")
              .instructions("Keep calculating.")
              .model("test-model")
              .responder(responder)
              .addTool(addTool)
              .maxTurns(3)
              .build();

      // Keep calling tools to exceed max turns
      for (int i = 0; i < 5; i++) {
        enqueueToolCallResponse("add", "{\"a\": 1, \"b\": 1}");
      }

      AgentResult result = agent.interact("Keep adding forever").join();

      assertTrue(result.isError());
      assertInstanceOf(com.paragon.responses.exception.AgentExecutionException.class, result.error());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TOOL ERRORS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Tool Errors")
  class ToolErrors {

    @Test
    @DisplayName("tool returning error is passed back to LLM")
    void tool_errorIsPassedBackToLLM() {
      FunctionTool<FailingParams> failingTool = toolFactory.create(FailingTool.class);

      Agent agent =
          Agent.builder()
              .name("ErrorHandler")
              .instructions("Handle errors gracefully.")
              .model("test-model")
              .responder(responder)
              .addTool(failingTool)
              .build();

      // Tool call that will fail
      enqueueToolCallResponse("failing_tool", "{}");
      // LLM responds after receiving error
      enqueueSuccessResponse("I received an error from the tool.");

      AgentResult result = agent.interact("Use the failing tool").join();

      assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("tool with invalid arguments is handled")
    void tool_invalidArgumentsHandled() {
      FunctionTool<AddParams> addTool = toolFactory.create(AddTool.class);

      Agent agent =
          Agent.builder()
              .name("Calculator")
              .instructions("Handle errors.")
              .model("test-model")
              .responder(responder)
              .addTool(addTool)
              .build();

      // Tool call with invalid JSON arguments
      enqueueToolCallResponse("add", "{invalid}");
      // LLM responds after receiving error
      enqueueSuccessResponse("The arguments were invalid.");

      AgentResult result = agent.interact("Add something").join();

      // Should still complete, error passed back to LLM
      assertFalse(result.isError());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPERS
  // ═══════════════════════════════════════════════════════════════════════════

  private void enqueueSuccessResponse(String text) {
    String responseJson =
        """
        {
          "id": "resp_%d",
          "object": "response",
          "created_at": 1234567890,
          "status": "completed",
          "output": [
            {
              "type": "message",
              "id": "msg_1",
              "status": "completed",
              "role": "assistant",
              "content": [
                {
                  "type": "output_text",
                  "text": "%s"
                }
              ]
            }
          ],
          "model": "test-model",
          "usage": {
            "input_tokens": 10,
            "output_tokens": 20,
            "total_tokens": 30
          }
        }
        """
            .formatted(System.nanoTime(), text);

    mockWebServer.enqueue(
        new MockResponse()
            .setBody(responseJson)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));
  }

  private void enqueueToolCallResponse(String toolName, String arguments) {
    String responseJson =
        """
        {
          "id": "resp_%d",
          "object": "response",
          "created_at": 1234567890,
          "status": "completed",
          "output": [
            {
              "type": "function_call",
              "id": "call_1",
              "call_id": "call_123",
              "name": "%s",
              "arguments": "%s"
            }
          ],
          "model": "test-model",
          "usage": {
            "input_tokens": 10,
            "output_tokens": 20,
            "total_tokens": 30
          }
        }
        """
            .formatted(System.nanoTime(), toolName, arguments.replace("\"", "\\\""));

    mockWebServer.enqueue(
        new MockResponse()
            .setBody(responseJson)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TOOL DEFINITIONS
  // ═══════════════════════════════════════════════════════════════════════════

  public record AddParams(int a, int b) {}

  public record MultiplyParams(int a, int b) {}

  @FunctionMetadata(name = "add", description = "Adds two numbers")
  public static class AddTool extends FunctionTool<AddParams> {
    @Override
    public @NonNull FunctionToolCallOutput call(@Nullable AddParams params) {
      if (params == null) return FunctionToolCallOutput.error("No params");
      return FunctionToolCallOutput.success(String.valueOf(params.a() + params.b()));
    }
  }

  @FunctionMetadata(name = "multiply", description = "Multiplies two numbers")
  public static class MultiplyTool extends FunctionTool<MultiplyParams> {
    @Override
    public @NonNull FunctionToolCallOutput call(@Nullable MultiplyParams params) {
      if (params == null) return FunctionToolCallOutput.error("No params");
      return FunctionToolCallOutput.success(String.valueOf(params.a() * params.b()));
    }
  }

  public record FailingParams() {}

  @FunctionMetadata(name = "failing_tool", description = "A tool that always fails")
  public static class FailingTool extends FunctionTool<FailingParams> {
    @Override
    public @NonNull FunctionToolCallOutput call(@Nullable FailingParams params) {
      return FunctionToolCallOutput.error("Tool intentionally failed");
    }
  }
}
