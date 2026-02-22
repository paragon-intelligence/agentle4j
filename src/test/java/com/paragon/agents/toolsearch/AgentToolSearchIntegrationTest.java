package com.paragon.agents.toolsearch;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.agents.Agent;
import com.paragon.agents.AgentResult;
import com.paragon.responses.Responder;
import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;

/**
 * Integration tests verifying that Agent correctly uses ToolRegistry for dynamic tool selection.
 */
@DisplayName("Agent + ToolRegistry Integration")
class AgentToolSearchIntegrationTest {

  private MockWebServer mockWebServer;
  private Responder responder;
  private ObjectMapper objectMapper;

  public record EmptyArgs() {}

  @FunctionMetadata(name = "get_weather", description = "Get weather forecast for a location")
  public static class WeatherTool extends FunctionTool<EmptyArgs> {
    @Override
    public FunctionToolCallOutput call(EmptyArgs params) {
      return FunctionToolCallOutput.success("sunny");
    }
  }

  @FunctionMetadata(name = "send_email", description = "Send an email to a recipient")
  public static class EmailTool extends FunctionTool<EmptyArgs> {
    @Override
    public FunctionToolCallOutput call(EmptyArgs params) {
      return FunctionToolCallOutput.success("sent");
    }
  }

  @FunctionMetadata(name = "search_database", description = "Search records in the database")
  public static class DatabaseTool extends FunctionTool<EmptyArgs> {
    @Override
    public FunctionToolCallOutput call(EmptyArgs params) {
      return FunctionToolCallOutput.success("found");
    }
  }

  @FunctionMetadata(name = "always_present", description = "A critical tool that must always be present")
  public static class AlwaysPresentTool extends FunctionTool<EmptyArgs> {
    @Override
    public FunctionToolCallOutput call(EmptyArgs params) {
      return FunctionToolCallOutput.success("always here");
    }
  }

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

  @Nested
  @DisplayName("Agent Builder")
  class BuilderTests {

    @Test
    @DisplayName("agent can be created with toolRegistry")
    void agentWithToolRegistry() {
      ToolRegistry registry =
          ToolRegistry.builder()
              .strategy(new BM25ToolSearchStrategy(3))
              .eagerTool(new AlwaysPresentTool())
              .deferredTool(new WeatherTool())
              .deferredTool(new EmailTool())
              .build();

      Agent agent =
          Agent.builder()
              .name("TestAgent")
              .model("test-model")
              .instructions("Test")
              .responder(responder)
              .toolRegistry(registry)
              .build();

      assertNotNull(agent);
      assertNotNull(agent.toolRegistry());
    }

    @Test
    @DisplayName("agent without toolRegistry works normally")
    void agentWithoutToolRegistryWorksNormally() {
      Agent agent =
          Agent.builder()
              .name("TestAgent")
              .model("test-model")
              .instructions("Test")
              .responder(responder)
              .addTool(new WeatherTool())
              .build();

      assertNotNull(agent);
      assertNull(agent.toolRegistry());
    }
  }

  @Nested
  @DisplayName("Dynamic Tool Selection")
  class DynamicToolSelectionTests {

    @Test
    @DisplayName("sends only matching tools in payload when using tool registry")
    void sendsOnlyMatchingTools() throws Exception {
      ToolRegistry registry =
          ToolRegistry.builder()
              .strategy(new RegexToolSearchStrategy(5))
              .deferredTool(new WeatherTool())
              .deferredTool(new EmailTool())
              .deferredTool(new DatabaseTool())
              .build();

      Agent agent =
          Agent.builder()
              .name("TestAgent")
              .model("test-model")
              .instructions("Test")
              .responder(responder)
              .toolRegistry(registry)
              .build();

      enqueueSuccessResponse("The weather is sunny");

      agent.interact("What's the weather like?");

      // Inspect the request sent to MockWebServer
      RecordedRequest request = mockWebServer.takeRequest();
      String body = request.getBody().readUtf8();

      // Weather tool should be in the payload
      assertTrue(body.contains("get_weather"), "Weather tool should be in payload");
      // Email tool should NOT be in the payload
      assertFalse(body.contains("send_email"), "Email tool should NOT be in payload");
    }

    @Test
    @DisplayName("always includes eager tools in payload")
    void alwaysIncludesEagerTools() throws Exception {
      ToolRegistry registry =
          ToolRegistry.builder()
              .strategy(new RegexToolSearchStrategy(5))
              .eagerTool(new AlwaysPresentTool())
              .deferredTool(new WeatherTool())
              .deferredTool(new EmailTool())
              .build();

      Agent agent =
          Agent.builder()
              .name("TestAgent")
              .model("test-model")
              .instructions("Test")
              .responder(responder)
              .toolRegistry(registry)
              .build();

      enqueueSuccessResponse("Done");

      agent.interact("What's the weather like?");

      RecordedRequest request = mockWebServer.takeRequest();
      String body = request.getBody().readUtf8();

      // Eager tool is always present
      assertTrue(body.contains("always_present"), "Eager tool should always be in payload");
      // Weather matches
      assertTrue(body.contains("get_weather"), "Weather tool should match");
      // Email doesn't match
      assertFalse(body.contains("send_email"), "Email tool should not match");
    }

    @Test
    @DisplayName("agent without registry sends all tools (backward compatible)")
    void withoutRegistrySendsAllTools() throws Exception {
      Agent agent =
          Agent.builder()
              .name("TestAgent")
              .model("test-model")
              .instructions("Test")
              .responder(responder)
              .addTool(new WeatherTool())
              .addTool(new EmailTool())
              .addTool(new DatabaseTool())
              .build();

      enqueueSuccessResponse("Done");

      agent.interact("Hello");

      RecordedRequest request = mockWebServer.takeRequest();
      String body = request.getBody().readUtf8();

      // All tools should be present
      assertTrue(body.contains("get_weather"));
      assertTrue(body.contains("send_email"));
      assertTrue(body.contains("search_database"));
    }

    @Test
    @DisplayName("tool store contains all tools for execution")
    void toolStoreContainsAllToolsForExecution() {
      ToolRegistry registry =
          ToolRegistry.builder()
              .strategy(new BM25ToolSearchStrategy(3))
              .eagerTool(new AlwaysPresentTool())
              .deferredTool(new WeatherTool())
              .deferredTool(new EmailTool())
              .build();

      Agent agent =
          Agent.builder()
              .name("TestAgent")
              .model("test-model")
              .instructions("Test")
              .responder(responder)
              .toolRegistry(registry)
              .build();

      // All tools should be registered via the tool registry
      List<String> allToolNames = agent.toolRegistry().allTools().stream()
          .map(FunctionTool::getName)
          .toList();
      assertTrue(allToolNames.contains("always_present"));
      assertTrue(allToolNames.contains("get_weather"));
      assertTrue(allToolNames.contains("send_email"));
    }
  }
}
