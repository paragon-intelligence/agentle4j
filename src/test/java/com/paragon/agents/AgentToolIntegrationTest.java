package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.Responder;
import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.json.JacksonJsonSchemaProducer;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import com.paragon.responses.spec.FunctionToolFactory;
import com.paragon.responses.spec.FunctionToolStore;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Integration tests for Agent with function tools.
 *
 * <p>Tests cover:
 * - Tool registration and retrieval
 * - Tool call execution
 * - Tool results handling
 * - Tool errors
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

    responder = Responder.builder()
        .openRouter()
        .apiKey("test-key")
        .baseUrl(mockWebServer.url("/"))
        .build();

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

      Agent agent = Agent.builder()
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

      Agent agent = Agent.builder()
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
      
      Agent agent = Agent.builder()
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
      
      Agent agent = Agent.builder()
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
      
      Agent agent = Agent.builder()
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
  // HELPERS
  // ═══════════════════════════════════════════════════════════════════════════

  private void enqueueSuccessResponse(String text) {
    String responseJson = """
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
        """.formatted(System.nanoTime(), text);

    mockWebServer.enqueue(new MockResponse()
        .setBody(responseJson)
        .setHeader("Content-Type", "application/json")
        .setResponseCode(200));
  }

  private void enqueueToolCallResponse(String toolName, String arguments) {
    String responseJson = """
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
        """.formatted(System.nanoTime(), toolName, arguments.replace("\"", "\\\""));

    mockWebServer.enqueue(new MockResponse()
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
}
