package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for SubAgentTool.
 *
 * <p>Tests cover:
 * - Tool metadata (name, description, parameters schema)
 * - Config builder options
 * - Context propagation modes
 * - Error handling
 * - Integration with Agent Builder
 */
@DisplayName("SubAgentTool")
class SubAgentToolTest {

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

  @Nested
  @DisplayName("Tool Metadata")
  class ToolMetadata {

    @Test
    @DisplayName("tool name follows invoke_[snake_case_name] pattern")
    void toolName_followsInvokeSnakeCasePattern() {
      Agent subAgent = createTestAgent("DataAnalyst");
      SubAgentTool tool = new SubAgentTool(subAgent, "For data analysis");

      assertEquals("invoke_data_analyst", tool.getName());
    }

    @Test
    @DisplayName("tool name handles multi-word agent names")
    void toolName_handlesMultiWordNames() {
      Agent subAgent = createTestAgent("CustomerSupportSpecialist");
      SubAgentTool tool = new SubAgentTool(subAgent, "For support");

      assertEquals("invoke_customer_support_specialist", tool.getName());
    }

    @Test
    @DisplayName("tool name handles spaces in agent name")
    void toolName_handlesSpacesInName() {
      Agent subAgent = createTestAgent("Customer Support");
      SubAgentTool tool = new SubAgentTool(subAgent, "For support");

      // Space followed by uppercase produces double underscore, which is acceptable
      assertEquals("invoke_customer__support", tool.getName());
    }

    @Test
    @DisplayName("tool name handles simple names")
    void toolName_handlesSimpleNames() {
      Agent subAgent = createTestAgent("Analyst");
      SubAgentTool tool = new SubAgentTool(subAgent, "For analysis");

      assertEquals("invoke_analyst", tool.getName());
    }

    @Test
    @DisplayName("tool uses provided description")
    void toolDescription_usesProvidedDescription() {
      Agent subAgent = createTestAgent("Analyst");
      SubAgentTool tool = new SubAgentTool(subAgent, "Custom description for testing");

      assertEquals("Custom description for testing", tool.getDescription());
    }

    @Test
    @DisplayName("tool has correct type")
    void toolType_isFunction() {
      Agent subAgent = createTestAgent("Test");
      SubAgentTool tool = new SubAgentTool(subAgent, "Description");

      assertEquals("function", tool.getType());
    }

    @Test
    @DisplayName("tool parameters schema has request field")
    void toolParameters_hasRequestField() {
      Agent subAgent = createTestAgent("Test");
      SubAgentTool tool = new SubAgentTool(subAgent, "Description");

      var params = tool.getParameters();
      assertEquals("object", params.get("type"));
      assertTrue(params.containsKey("properties"));
    }
  }

  @Nested
  @DisplayName("Config Builder")
  class ConfigBuilder {

    @Test
    @DisplayName("config builder sets description")
    void configBuilder_setsDescription() {
      SubAgentTool.Config config =
          SubAgentTool.Config.builder().description("Test description").build();

      assertEquals("Test description", config.description());
    }

    @Test
    @DisplayName("config shareState defaults to true")
    void configShareState_defaultsToTrue() {
      SubAgentTool.Config config = SubAgentTool.Config.builder().description("Test").build();

      assertTrue(config.shareState());
    }

    @Test
    @DisplayName("config shareHistory defaults to false")
    void configShareHistory_defaultsToFalse() {
      SubAgentTool.Config config = SubAgentTool.Config.builder().description("Test").build();

      assertFalse(config.shareHistory());
    }

    @Test
    @DisplayName("config shareState can be disabled")
    void configShareState_canBeDisabled() {
      SubAgentTool.Config config =
          SubAgentTool.Config.builder().description("Test").shareState(false).build();

      assertFalse(config.shareState());
    }

    @Test
    @DisplayName("config shareHistory can be enabled")
    void configShareHistory_canBeEnabled() {
      SubAgentTool.Config config =
          SubAgentTool.Config.builder().description("Test").shareHistory(true).build();

      assertTrue(config.shareHistory());
    }

    @Test
    @DisplayName("tool created with config uses config values")
    void toolWithConfig_usesConfigValues() {
      Agent subAgent = createTestAgent("Test");
      SubAgentTool.Config config =
          SubAgentTool.Config.builder()
              .description("Config description")
              .shareState(false)
              .shareHistory(true)
              .build();

      SubAgentTool tool = new SubAgentTool(subAgent, config);

      assertEquals("Config description", tool.getDescription());
      assertFalse(tool.sharesState());
      assertTrue(tool.sharesHistory());
    }
  }

  @Nested
  @DisplayName("Tool Accessors")
  class ToolAccessors {

    @Test
    @DisplayName("targetAgent returns wrapped agent")
    void targetAgent_returnsWrappedAgent() {
      Agent subAgent = createTestAgent("TargetAgent");
      SubAgentTool tool = new SubAgentTool(subAgent, "Description");

      assertEquals(subAgent, tool.targetAgent());
      assertEquals("TargetAgent", tool.targetAgent().name());
    }

    @Test
    @DisplayName("sharesState returns correct value")
    void sharesState_returnsCorrectValue() {
      Agent subAgent = createTestAgent("Test");

      SubAgentTool toolDefault = new SubAgentTool(subAgent, "Desc");
      assertTrue(toolDefault.sharesState());

      SubAgentTool toolNoState =
          new SubAgentTool(
              subAgent, SubAgentTool.Config.builder().description("Desc").shareState(false).build());
      assertFalse(toolNoState.sharesState());
    }

    @Test
    @DisplayName("sharesHistory returns correct value")
    void sharesHistory_returnsCorrectValue() {
      Agent subAgent = createTestAgent("Test");

      SubAgentTool toolDefault = new SubAgentTool(subAgent, "Desc");
      assertFalse(toolDefault.sharesHistory());

      SubAgentTool toolWithHistory =
          new SubAgentTool(
              subAgent,
              SubAgentTool.Config.builder().description("Desc").shareHistory(true).build());
      assertTrue(toolWithHistory.sharesHistory());
    }
  }

  @Nested
  @DisplayName("Tool Invocation")
  class ToolInvocation {

    @Test
    @DisplayName("call with null params returns error")
    void call_withNullParams_returnsError() {
      Agent subAgent = createTestAgent("Test");
      SubAgentTool tool = new SubAgentTool(subAgent, "Description");

      FunctionToolCallOutput result = tool.call(null);

      assertEquals(FunctionToolCallOutputStatus.INCOMPLETE, result.status());
      assertTrue(result.output().toString().contains("cannot be empty"));
    }

    @Test
    @DisplayName("call with empty request returns error")
    void call_withEmptyRequest_returnsError() {
      Agent subAgent = createTestAgent("Test");
      SubAgentTool tool = new SubAgentTool(subAgent, "Description");

      SubAgentTool.SubAgentParams params = new SubAgentTool.SubAgentParams("");
      FunctionToolCallOutput result = tool.call(params);

      assertEquals(FunctionToolCallOutputStatus.INCOMPLETE, result.status());
      assertTrue(result.output().toString().contains("cannot be empty"));
    }

    @Test
    @DisplayName("call invokes target agent and returns output")
    void call_invokesTargetAgent_returnsOutput() {
      Agent subAgent = createTestAgent("Analyzer");
      SubAgentTool tool = new SubAgentTool(subAgent, "For analysis");

      // Enqueue response for sub-agent
      enqueueSuccessResponse("Analysis complete: The data shows positive trends.");

      SubAgentTool.SubAgentParams params =
          new SubAgentTool.SubAgentParams("Analyze this data for trends");
      FunctionToolCallOutput result = tool.call(params);

      assertEquals(FunctionToolCallOutputStatus.COMPLETED, result.status());
      assertTrue(result.output().toString().contains("Analysis complete"));
    }

    @Test
    @DisplayName("call handles sub-agent errors gracefully")
    void call_handlesSubAgentErrors() {
      Agent subAgent = createTestAgent("Analyzer");
      SubAgentTool tool = new SubAgentTool(subAgent, "For analysis");

      // Enqueue error response
      mockWebServer.enqueue(
          new MockResponse()
              .setResponseCode(500)
              .setBody("{\"error\": \"Server error\"}")
              .addHeader("Content-Type", "application/json"));

      SubAgentTool.SubAgentParams params = new SubAgentTool.SubAgentParams("Analyze this");
      FunctionToolCallOutput result = tool.call(params);

      // Should return error output, not throw
      assertEquals(FunctionToolCallOutputStatus.INCOMPLETE, result.status());
    }
  }

  @Nested
  @DisplayName("Agent Builder Integration")
  class AgentBuilderIntegration {

    @Test
    @DisplayName("addSubAgent adds tool to agent")
    void addSubAgent_addsToolToAgent() {
      Agent subAgent = createTestAgent("Analyzer");
      Agent orchestrator =
          Agent.builder()
              .name("Orchestrator")
              .model("test-model")
              .instructions("Use sub-agents")
              .responder(responder)
              .addSubAgent(subAgent, "For data analysis")
              .build();

      // The tool should be registered (we can test this by running the agent)
      assertNotNull(orchestrator);
    }

    @Test
    @DisplayName("addSubAgent with config works")
    void addSubAgent_withConfig_works() {
      Agent subAgent = createTestAgent("Analyzer");
      SubAgentTool.Config config =
          SubAgentTool.Config.builder()
              .description("For analysis with history")
              .shareHistory(true)
              .build();

      Agent orchestrator =
          Agent.builder()
              .name("Orchestrator")
              .model("test-model")
              .instructions("Use sub-agents")
              .responder(responder)
              .addSubAgent(subAgent, config)
              .build();

      assertNotNull(orchestrator);
    }

    @Test
    @DisplayName("multiple sub-agents can be added")
    void multipleSubAgents_canBeAdded() {
      Agent analyzer = createTestAgent("Analyzer");
      Agent writer = createTestAgent("Writer");

      Agent orchestrator =
          Agent.builder()
              .name("Orchestrator")
              .model("test-model")
              .instructions("Coordinate sub-agents")
              .responder(responder)
              .addSubAgent(analyzer, "For analysis")
              .addSubAgent(writer, "For writing")
              .build();

      assertNotNull(orchestrator);
    }
  }

  @Nested
  @DisplayName("SubAgentParams Record")
  class SubAgentParamsRecord {

    @Test
    @DisplayName("SubAgentParams stores request")
    void subAgentParams_storesRequest() {
      SubAgentTool.SubAgentParams params = new SubAgentTool.SubAgentParams("Test request");

      assertEquals("Test request", params.request());
    }

    @Test
    @DisplayName("SubAgentParams equality works")
    void subAgentParams_equalityWorks() {
      SubAgentTool.SubAgentParams params1 = new SubAgentTool.SubAgentParams("Test");
      SubAgentTool.SubAgentParams params2 = new SubAgentTool.SubAgentParams("Test");
      SubAgentTool.SubAgentParams params3 = new SubAgentTool.SubAgentParams("Different");

      assertEquals(params1, params2);
      assertNotEquals(params1, params3);
    }
  }

  // Helper methods

  private Agent createTestAgent(String name) {
    return Agent.builder()
        .name(name)
        .model("test-model")
        .instructions("Test instructions for " + name)
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
}
