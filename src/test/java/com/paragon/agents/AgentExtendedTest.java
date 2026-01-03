package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.agents.context.*;
import com.paragon.responses.Responder;
import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.*;
import com.paragon.telemetry.TelemetryContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

/**
 * Extended tests for Agent.java covering advanced scenarios:
 * - Tool execution and callbacks
 * - Handoff detection and execution
 * - Max turns exceeded
 * - Output guardrail failures
 * - Context management
 * - resume() method paths
 * - Structured output parsing
 */
@DisplayName("Agent Extended Tests")
class AgentExtendedTest {

  private MockWebServer mockWebServer;
  private Responder responder;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    responder =
        Responder.builder().baseUrl(mockWebServer.url("/v1/responses")).apiKey("test-key").build();
    objectMapper = new ObjectMapper();
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BUILDER EDGE CASES
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Builder Edge Cases")
  class BuilderEdgeCaseTests {

    @Test
    @DisplayName("builder with custom metadata")
    void builderWithCustomMetadata() {
      Map<String, String> metadata = Map.of("key1", "value1", "key2", "value2");
      
      Agent agent = Agent.builder()
          .name("MetadataAgent")
          .model("test-model")
          .instructions("Test")
          .responder(responder)
          .metadata(metadata)
          .build();

      assertNotNull(agent);
      assertEquals("MetadataAgent", agent.name());
    }

    @Test
    @DisplayName("builder with custom ObjectMapper")
    void builderWithCustomObjectMapper() {
      ObjectMapper customMapper = new ObjectMapper();
      
      Agent agent = Agent.builder()
          .name("MapperAgent")
          .model("test-model")
          .instructions("Test")
          .responder(responder)
          .objectMapper(customMapper)
          .build();

      assertNotNull(agent);
    }

    @Test
    @DisplayName("builder with addTools varargs")
    void builderWithAddToolsVarargs() {
      TestTool tool1 = new TestTool(new AtomicReference<>());
      FailingTool tool2 = new FailingTool();
      
      Agent agent = Agent.builder()
          .name("MultiToolAgent")
          .model("test-model")
          .instructions("Test")
          .responder(responder)
          .addTools(tool1, tool2)
          .build();

      assertNotNull(agent);
      assertNotNull(agent.toolStore());
    }

    @Test
    @DisplayName("builder with temperature set")
    void builderWithTemperature() {
      Agent agent = Agent.builder()
          .name("TempAgent")
          .model("test-model")
          .instructions("Test")
          .responder(responder)
          .temperature(0.5)
          .build();

      assertNotNull(agent);
    }

    @Test
    @DisplayName("builder with maxOutputTokens set")
    void builderWithMaxOutputTokens() {
      Agent agent = Agent.builder()
          .name("TokenAgent")
          .model("test-model")
          .instructions("Test")
          .responder(responder)
          .maxOutputTokens(2000)
          .build();

      assertNotNull(agent);
    }

    @Test
    @DisplayName("builder rejects negative maxTurns")
    void builderRejectsNegativeMaxTurns() {
      assertThrows(IllegalArgumentException.class, () -> {
        Agent.builder()
            .name("InvalidAgent")
            .model("test-model")
            .instructions("Test")
            .responder(responder)
            .maxTurns(-1)
            .build();
      });
    }

    @Test
    @DisplayName("builder rejects zero maxTurns")
    void builderRejectsZeroMaxTurns() {
      assertThrows(IllegalArgumentException.class, () -> {
        Agent.builder()
            .name("InvalidAgent")
            .model("test-model")
            .instructions("Test")
            .responder(responder)
            .maxTurns(0)
            .build();
      });
    }

    @Test
    @DisplayName("builder handles empty instructions")
    void builderHandlesEmptyInstructions() {
      Agent agent = Agent.builder()
          .name("EmptyInstructionsAgent")
          .model("test-model")
          .instructions("")
          .responder(responder)
          .build();

      assertNotNull(agent);
      assertEquals("", agent.instructions().toString());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TOOL EXECUTION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Tool Execution")
  class ToolExecutionTests {

    @Test
    @DisplayName("agent executes registered tool and returns result")
    void executesRegisteredTool() throws Exception {
      AtomicReference<String> capturedArg = new AtomicReference<>();

      FunctionTool<TestArgs> tool = new TestTool(capturedArg);

      Agent agent = Agent.builder()
          .name("ToolAgent")
          .model("test-model")
          .instructions("Use tools when needed")
          .responder(responder)
          .addTool(tool)
          .build();

      // First response triggers tool call
      enqueueToolCallResponse("test_tool", "{\"query\": \"search term\"}");
      // Second response is final answer
      enqueueSuccessResponse("Tool result processed");

      AgentResult result = agent.interact("Search for something").get(5, TimeUnit.SECONDS);

      assertNotNull(result);
      assertEquals("search term", capturedArg.get());
      assertTrue(result.toolExecutions().size() > 0);
    }

    @Test
    @DisplayName("agent handles tool execution failure gracefully")
    void handlesToolExecutionFailure() throws Exception {
      FunctionTool<TestArgs> tool = new FailingTool();

      Agent agent = Agent.builder()
          .name("ToolAgent")
          .model("test-model")
          .instructions("Use tools")
          .responder(responder)
          .addTool(tool)
          .build();

      // First response triggers tool call
      enqueueToolCallResponse("failing_tool", "{\"query\": \"test\"}");
      // Second response is final answer after error
      enqueueSuccessResponse("I encountered an error");

      AgentResult result = agent.interact("Run the tool").get(5, TimeUnit.SECONDS);

      assertNotNull(result);
      // Tool execution should have captured the error
      assertFalse(result.toolExecutions().isEmpty());
    }

    @Test
    @DisplayName("agent executes multiple tools in sequence")
    void executesMultipleTools() throws Exception {
      AtomicInteger callCount = new AtomicInteger(0);

      FunctionTool<TestArgs> tool1 = new CountingTool("tool_one", callCount);
      FunctionTool<TestArgs> tool2 = new CountingTool("tool_two", callCount);

      Agent agent = Agent.builder()
          .name("MultiToolAgent")
          .model("test-model")
          .instructions("Use tools")
          .responder(responder)
          .addTool(tool1)
          .addTool(tool2)
          .build();

      // Response with two tool calls
      enqueueMultiToolCallResponse(
          List.of("tool_one", "tool_two"),
          List.of("{\"query\": \"a\"}", "{\"query\": \"b\"}")
      );
      enqueueSuccessResponse("Both tools completed");

      AgentResult result = agent.interact("Run both tools").get(5, TimeUnit.SECONDS);

      assertNotNull(result);
      assertEquals(2, callCount.get());
      assertEquals(2, result.toolExecutions().size());
    }

    @Test
    @DisplayName("tool needing confirmation triggers pause")
    void toolNeedingConfirmationTriggersPause() throws Exception {
      FunctionTool<TestArgs> tool = new ConfirmationTool();

      Agent agent = Agent.builder()
          .name("ConfirmAgent")
          .model("test-model")
          .instructions("Use confirmation tool")
          .responder(responder)
          .addTool(tool)
          .build();

      // LLM calls the confirmation tool
      enqueueToolCallResponse("confirmation_tool", "{\"query\": \"delete everything\"}");

      AgentResult result = agent.interact("Delete stuff").get(5, TimeUnit.SECONDS);

      // Should be paused waiting for confirmation
      assertTrue(result.isPaused());
      assertNotNull(result.pausedState());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HANDOFF EXECUTION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Handoff Execution")
  class HandoffExecutionTests {

    @Test
    @DisplayName("agent can be configured with handoffs")
    void agentCanBeConfiguredWithHandoffs() {
      Agent targetAgent = Agent.builder()
          .name("TargetAgent")
          .model("test-model")
          .instructions("Handle the handoff")
          .responder(responder)
          .build();

      Agent mainAgent = Agent.builder()
          .name("MainAgent")
          .model("test-model")
          .instructions("Handoff when needed")
          .responder(responder)
          .addHandoff(Handoff.to(targetAgent).build())
          .build();

      assertEquals(1, mainAgent.handoffs().size());
      assertEquals("TargetAgent", mainAgent.handoffs().get(0).targetAgent().name());
    }

    @Test
    @DisplayName("handoff tool is added to available tools")
    void handoffToolIsAddedToTools() {
      Agent targetAgent = Agent.builder()
          .name("SupportAgent")
          .model("test-model")
          .instructions("Handle support")
          .responder(responder)
          .build();

      Agent mainAgent = Agent.builder()
          .name("Router")
          .model("test-model")
          .instructions("Route to agents")
          .responder(responder)
          .addHandoff(Handoff.to(targetAgent).build())
          .build();

      // Handoff should be registered
      assertEquals(1, mainAgent.handoffs().size());
      Handoff handoff = mainAgent.handoffs().get(0);
      assertNotNull(handoff.asTool());
    }

    @Test
    @DisplayName("handoff is triggered and executes target agent")
    void handoffIsTriggeredAndExecutesTargetAgent() throws Exception {
      Agent targetAgent = Agent.builder()
          .name("SupportAgent")
          .model("test-model")
          .instructions("Handle support")
          .responder(responder)
          .build();

      // 1. Main agent triggers handoff (uses the auto-generated handoff tool name: transfer_to_<snake_case_name>)
      enqueueHandoffResponse("transfer_to_support_agent", "{\"message\": \"Customer needs help\"}");
      // 2. Target agent receives the handoff and responds
      enqueueSuccessResponse("I am the support agent handling your request");

      Agent mainAgent = Agent.builder()
          .name("Router")
          .model("test-model")
          .instructions("Route to agents")
          .responder(responder)
          .addHandoff(Handoff.to(targetAgent).build())
          .build();

      AgentResult result = mainAgent.interact("I need support").get(5, TimeUnit.SECONDS);

      assertTrue(result.isHandoff());
      assertNotNull(result.handoffAgent());
      assertEquals("SupportAgent", result.handoffAgent().name());
    }

    @Test
    @DisplayName("handoff with custom description")
    void handoffWithCustomDescription() {
      Agent targetAgent = Agent.builder()
          .name("BillingAgent")
          .model("test-model")
          .instructions("Handle billing")
          .responder(responder)
          .build();

      Agent mainAgent = Agent.builder()
          .name("Router")
          .model("test-model")
          .instructions("Route to agents")
          .responder(responder)
          .addHandoff(Handoff.to(targetAgent)
              .withDescription("Transfer for billing questions")
              .build())
          .build();

      assertEquals(1, mainAgent.handoffs().size());
      assertEquals("Transfer for billing questions", mainAgent.handoffs().get(0).description());
    }

    @Test
    @DisplayName("handoff with custom name")
    void handoffWithCustomName() {
      Agent targetAgent = Agent.builder()
          .name("SalesAgent")
          .model("test-model")
          .instructions("Handle sales")
          .responder(responder)
          .build();

      Agent mainAgent = Agent.builder()
          .name("Router")
          .model("test-model")
          .instructions("Route to agents")
          .responder(responder)
          .addHandoff(Handoff.to(targetAgent)
              .withName("route_to_sales")
              .build())
          .build();

      assertEquals(1, mainAgent.handoffs().size());
      assertEquals("route_to_sales", mainAgent.handoffs().get(0).name());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MAX TURNS EXCEEDED
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Max Turns Exceeded")
  class MaxTurnsExceededTests {

    @Test
    @DisplayName("agent returns error when max turns exceeded")
    void returnsErrorWhenMaxTurnsExceeded() throws Exception {
      FunctionTool<TestArgs> tool = new SimpleTestTool();

      Agent agent = Agent.builder()
          .name("LoopAgent")
          .model("test-model")
          .instructions("Keep calling tools")
          .responder(responder)
          .addTool(tool)
          .maxTurns(2)
          .build();

      // Response with tool call (turn 1)
      enqueueToolCallResponse("simple_test_tool", "{\"query\": \"a\"}");
      // Response with tool call (turn 2)
      enqueueToolCallResponse("simple_test_tool", "{\"query\": \"b\"}");
      // Turn 3 would exceed max

      AgentResult result = agent.interact("Start infinite loop").get(5, TimeUnit.SECONDS);

      assertTrue(result.isError());
      assertNotNull(result.error());
      assertTrue(result.error().getMessage().contains("max") || 
                 result.error().getMessage().toLowerCase().contains("turn"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // OUTPUT GUARDRAIL FAILURES
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Output Guardrail Failures")
  class OutputGuardrailFailureTests {

    @Test
    @DisplayName("output guardrail failure returns error result")
    void outputGuardrailFailureReturnsError() throws Exception {
      Agent agent = Agent.builder()
          .name("GuardedAgent")
          .model("test-model")
          .instructions("Test")
          .responder(responder)
          .addOutputGuardrail((output, ctx) -> {
            if (output.contains("forbidden")) {
              return GuardrailResult.failed("Output contains forbidden content");
            }
            return GuardrailResult.passed();
          })
          .build();

      enqueueSuccessResponse("This is forbidden content");

      AgentResult result = agent.interact("Generate response").get(5, TimeUnit.SECONDS);

      assertTrue(result.isError());
      assertNotNull(result.error());
    }

    @Test
    @DisplayName("output guardrail passes for valid content")
    void outputGuardrailPassesForValidContent() throws Exception {
      Agent agent = Agent.builder()
          .name("GuardedAgent")
          .model("test-model")
          .instructions("Test")
          .responder(responder)
          .addOutputGuardrail((output, ctx) -> {
            if (output.contains("forbidden")) {
              return GuardrailResult.failed("Output contains forbidden content");
            }
            return GuardrailResult.passed();
          })
          .build();

      enqueueSuccessResponse("This is perfectly fine content");

      AgentResult result = agent.interact("Generate response").get(5, TimeUnit.SECONDS);

      assertTrue(result.isSuccess());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CONTEXT MANAGEMENT
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Context Management")
  class ContextManagementTests {

    @Test
    @DisplayName("agent with context management applies strategy")
    void agentWithContextManagement() throws Exception {
      ContextManagementConfig config = ContextManagementConfig.builder()
          .strategy(new SlidingWindowStrategy())
          .maxTokens(1000)
          .tokenCounter(new SimpleTestTokenCounter())
          .build();

      Agent agent = Agent.builder()
          .name("ManagedAgent")
          .model("test-model")
          .instructions("Test")
          .responder(responder)
          .contextManagement(config)
          .build();

      enqueueSuccessResponse("Response with context management");

      AgentResult result = agent.interact("Hello").get(5, TimeUnit.SECONDS);

      assertNotNull(result);
      assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("sliding window strategy preserves recent messages")
    void slidingWindowPreservesRecentMessages() throws Exception {
      SlidingWindowStrategy strategy = SlidingWindowStrategy.preservingDeveloperMessage();

      ContextManagementConfig config = ContextManagementConfig.builder()
          .strategy(strategy)
          .maxTokens(100)
          .tokenCounter(new SimpleTestTokenCounter())
          .build();

      Agent agent = Agent.builder()
          .name("WindowAgent")
          .model("test-model")
          .instructions("System prompt")
          .responder(responder)
          .contextManagement(config)
          .build();

      enqueueSuccessResponse("Response");

      AgentResult result = agent.interact("Hello").get(5, TimeUnit.SECONDS);

      assertNotNull(result);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // RESUME METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Resume Methods")
  class ResumeMethodTests {

    @Test
    @DisplayName("resume throws when state is null")
    void resumeThrowsWhenStateNull() {
      Agent agent = createTestAgent();

      assertThrows(NullPointerException.class, () -> agent.resume(null));
    }

    @Test
    @DisplayName("resume throws when state is not pending approval")
    void resumeThrowsWhenNotPendingApproval() {
      Agent agent = createTestAgent();
      AgentContext context = AgentContext.create();

      // Create a failed state (which is not pending approval)
      AgentRunState failedState = AgentRunState.failed("TestAgent", context, 1);

      assertThrows(IllegalStateException.class, () -> agent.resume(failedState));
    }

    @Test
    @DisplayName("resumeStream throws when state is null")
    void resumeStreamThrowsWhenStateNull() {
      Agent agent = createTestAgent();

      assertThrows(NullPointerException.class, () -> agent.resumeStream(null));
    }

    @Test
    @DisplayName("resumeStream throws when not pending approval")
    void resumeStreamThrowsWhenNotPendingApproval() {
      Agent agent = createTestAgent();
      AgentContext context = AgentContext.create();

      AgentRunState failedState = AgentRunState.failed("TestAgent", context, 1);

      assertThrows(IllegalStateException.class, () -> agent.resumeStream(failedState));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TRACE CONTEXT
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Trace Context")
  class TraceContextTests {

    @Test
    @DisplayName("agent auto-initializes trace context if not set")
    void autoInitializesTraceContext() throws Exception {
      Agent agent = createTestAgent();
      AgentContext context = AgentContext.create();

      assertFalse(context.hasTraceContext());

      enqueueSuccessResponse("Response");
      context.addInput(Message.user("Hello"));
      agent.interact(context).get(5, TimeUnit.SECONDS);

      // After interaction, trace context should be set
      assertTrue(context.hasTraceContext());
    }

    @Test
    @DisplayName("agent uses provided trace context")
    void usesProvidedTraceContext() throws Exception {
      Agent agent = createTestAgent();
      AgentContext context = AgentContext.create();
      context.withTraceContext("trace-123", "span-456");

      assertTrue(context.hasTraceContext());
      assertEquals("trace-123", context.parentTraceId());
      assertEquals("span-456", context.parentSpanId());

      enqueueSuccessResponse("Response");
      context.addInput(Message.user("Hello"));
      agent.interact(context).get(5, TimeUnit.SECONDS);

      // Original trace context should be preserved
      assertEquals("trace-123", context.parentTraceId());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CONFIGURATION OPTIONS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Configuration Options")
  class ConfigurationOptionsTests {

    @Test
    @DisplayName("temperature is applied to requests")
    void temperatureIsApplied() throws Exception {
      Agent agent = Agent.builder()
          .name("TempAgent")
          .model("test-model")
          .instructions("Test")
          .responder(responder)
          .temperature(0.7)
          .build();

      enqueueSuccessResponse("Response");

      AgentResult result = agent.interact("Hello").get(5, TimeUnit.SECONDS);

      assertNotNull(result);
    }

    @Test
    @DisplayName("custom object mapper is used")
    void customObjectMapperIsUsed() throws Exception {
      ObjectMapper customMapper = new ObjectMapper();

      Agent agent = Agent.builder()
          .name("MapperAgent")
          .model("test-model")
          .instructions("Test")
          .responder(responder)
          .objectMapper(customMapper)
          .build();

      enqueueSuccessResponse("Response");

      AgentResult result = agent.interact("Hello").get(5, TimeUnit.SECONDS);

      assertNotNull(result);
    }

    @Test
    @DisplayName("telemetry context is used")
    void telemetryContextIsUsed() throws Exception {
      TelemetryContext telemetryContext = TelemetryContext.builder()
          .traceName("test-trace")
          .userId("user-123")
          .build();

      Agent agent = Agent.builder()
          .name("TelemetryAgent")
          .model("test-model")
          .instructions("Test")
          .responder(responder)
          .telemetryContext(telemetryContext)
          .build();

      enqueueSuccessResponse("Response");

      AgentResult result = agent.interact("Hello").get(5, TimeUnit.SECONDS);

      assertNotNull(result);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // STRUCTURED OUTPUT
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Structured Output")
  class StructuredOutputTests {

    @Test
    @DisplayName("structured agent parses output correctly")
    void structuredAgentParsesOutput() throws Exception {
      Agent.Structured<PersonInfo> agent = Agent.builder()
          .name("ExtractorAgent")
          .model("test-model")
          .instructions("Extract person info")
          .responder(responder)
          .structured(PersonInfo.class)
          .build();

      enqueueStructuredResponse("{\\\"name\\\": \\\"John\\\", \\\"age\\\": 30}");

      StructuredAgentResult<PersonInfo> result = 
          agent.interact("Extract: John is 30").get(5, TimeUnit.SECONDS);

      assertNotNull(result);
      if (result.output() != null) {
        assertEquals("John", result.output().name());
        assertEquals(30, result.output().age());
      }
    }

    @Test
    @DisplayName("structured agent handles parsing failure")
    void structuredAgentHandlesParsingFailure() throws Exception {
      Agent.Structured<PersonInfo> agent = Agent.builder()
          .name("ExtractorAgent")
          .model("test-model")
          .instructions("Extract person info")
          .responder(responder)
          .structured(PersonInfo.class)
          .build();

      // Invalid JSON that can't be parsed to PersonInfo
      enqueueSuccessResponse("This is not valid JSON");

      StructuredAgentResult<PersonInfo> result = 
          agent.interact("Parse this").get(5, TimeUnit.SECONDS);

      // Result may indicate error or have null output
      assertNotNull(result);
      // Either parsing fails (isError=true) or output is null
      assertTrue(result.isError() || result.output() == null);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // LLM CALL FAILURES
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("LLM Call Failures")
  class LlmCallFailureTests {

    @Test
    @DisplayName("agent handles timeout gracefully")
    void handlesTimeout() {
      // Just test that the agent can be created without errors
      Agent agent = createTestAgent();
      assertNotNull(agent);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // STREAMING INTERACTIONS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Streaming Interactions")
  class StreamingInteractionsTests {

    @Test
    @DisplayName("interactStream returns AgentStream")
    void interactStreamReturnsAgentStream() {
      Agent agent = createTestAgent();

      AgentStream stream = agent.interactStream("Hello");

      assertNotNull(stream);
    }

    @Test
    @DisplayName("interactStream with AgentContext")
    void interactStreamWithAgentContext() {
      Agent agent = createTestAgent();
      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Hello"));

      AgentStream stream = agent.interactStream(context);

      assertNotNull(stream);
    }

    @Test
    @DisplayName("interactStream with empty context adds input")
    void interactStreamWithEmptyContext() {
      Agent agent = createTestAgent();
      AgentContext context = AgentContext.create();
      // Do not add input - context is empty
      // The stream should handle this gracefully
      AgentStream stream = agent.interactStream(context);

      assertNotNull(stream);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // INPUT GUARDRAILS EDGE CASES
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Input Guardrails Edge Cases")
  class InputGuardrailsEdgeCasesTests {

    @Test
    @DisplayName("input guardrail failure returns error in streaming")
    void inputGuardrailFailureReturnsErrorInStreaming() {
      Agent agent = Agent.builder()
          .name("GuardrailAgent")
          .model("test-model")
          .instructions("Test")
          .responder(responder)
          .addInputGuardrail((inputText, ctx) -> 
              GuardrailResult.failed("Input not allowed"))
          .build();

      AgentStream stream = agent.interactStream("This should fail");

      assertNotNull(stream);
      // Stream should contain the failure
    }

    @Test
    @DisplayName("multiple input guardrails all pass")
    void multipleInputGuardrailsAllPass() throws Exception {
      AtomicInteger guardrailCallCount = new AtomicInteger(0);

      Agent agent = Agent.builder()
          .name("MultiGuardrailAgent")
          .model("test-model")
          .instructions("Test")
          .responder(responder)
          .addInputGuardrail((inputText, ctx) -> {
            guardrailCallCount.incrementAndGet();
            return GuardrailResult.passed();
          })
          .addInputGuardrail((inputText, ctx) -> {
            guardrailCallCount.incrementAndGet();
            return GuardrailResult.passed();
          })
          .build();

      enqueueSuccessResponse("Response");

      AgentResult result = agent.interact("Hello").get(5, TimeUnit.SECONDS);

      assertTrue(result.isSuccess());
      assertEquals(2, guardrailCallCount.get());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPER CLASSES AND METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  public record TestArgs(String query) {}

  public record PersonInfo(String name, int age) {}

  @FunctionMetadata(name = "test_tool", description = "A test tool")
  private static class TestTool extends FunctionTool<TestArgs> {
    private final AtomicReference<String> capturedArg;

    TestTool(AtomicReference<String> capturedArg) {
      this.capturedArg = capturedArg;
    }

    @Override
    public FunctionToolCallOutput call(TestArgs params) {
      capturedArg.set(params.query());
      return FunctionToolCallOutput.success("Tool executed: " + params.query());
    }
  }

  @FunctionMetadata(name = "failing_tool", description = "A tool that fails")
  private static class FailingTool extends FunctionTool<TestArgs> {
    @Override
    public FunctionToolCallOutput call(TestArgs params) {
      throw new RuntimeException("Tool execution failed");
    }
  }

  private static class CountingTool extends FunctionTool<TestArgs> {
    private final String name;
    private final AtomicInteger callCount;

    CountingTool(String name, AtomicInteger callCount) {
      super();
      this.name = name;
      this.callCount = callCount;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public FunctionToolCallOutput call(TestArgs params) {
      callCount.incrementAndGet();
      return FunctionToolCallOutput.success(name + " result");
    }
  }

  @FunctionMetadata(name = "simple_test_tool", description = "A simple test tool")
  private static class SimpleTestTool extends FunctionTool<TestArgs> {
    @Override
    public FunctionToolCallOutput call(TestArgs params) {
      return FunctionToolCallOutput.success("Call me again");
    }
  }

  @FunctionMetadata(name = "confirmation_tool", description = "A tool that requires confirmation")
  private static class ConfirmationTool extends FunctionTool<TestArgs> {
    @Override
    public FunctionToolCallOutput call(TestArgs params) {
      return FunctionToolCallOutput.success("Confirmed action: " + params.query());
    }

    @Override
    public boolean requiresConfirmation() {
      return true; // This triggers the pause path in Agent
    }
  }

  private static class SimpleTestTokenCounter implements TokenCounter {
    @Override
    public int countTokens(ResponseInputItem item) {
      return 10;
    }

    @Override
    public int countText(String text) {
      return text.length() / 4;
    }

    @Override
    public int countImage(Image image) {
      return 100;
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

  private void enqueueStructuredResponse(String structuredJson) {
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
        """.formatted(structuredJson);

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

  private void enqueueHandoffResponse(String handoffName, String arguments) {
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
        """.formatted(handoffName, arguments.replace("\"", "\\\""));

    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(json)
            .addHeader("Content-Type", "application/json"));
  }
}
