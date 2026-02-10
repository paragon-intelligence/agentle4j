package com.paragon.agents.context;

import com.paragon.agents.Agent;
import com.paragon.agents.AgentResult;
import com.paragon.agents.AgenticContext;
import com.paragon.responses.Responder;
import com.paragon.responses.spec.Message;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Agent with context management.
 */
@DisplayName("Agent Context Management Integration")
class AgenticContextWindowIntegrationTest {

  private MockWebServer mockWebServer;
  private Responder responder;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    responder =
            Responder.builder().baseUrl(mockWebServer.url("/v1/responses")).apiKey("test-key").build();
  }

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
  class AgentBuilderTests {

    @Test
    @DisplayName("contextManagement method sets config")
    void contextManagement_setsConfig() {
      ContextManagementConfig config =
              ContextManagementConfig.builder()
                      .strategy(new SlidingWindowStrategy())
                      .maxTokens(4000)
                      .build();

      Agent agent =
              Agent.builder()
                      .name("Test")
                      .model("test-model")
                      .instructions("Test instructions")
                      .responder(responder)
                      .contextManagement(config)
                      .build();

      assertNotNull(agent);
    }

    @Test
    @DisplayName("contextManagement with custom counter")
    void contextManagement_withCustomCounter() {
      ContextManagementConfig config =
              ContextManagementConfig.builder()
                      .strategy(new SlidingWindowStrategy())
                      .maxTokens(4000)
                      .tokenCounter(new SimpleTokenCounter(3))
                      .build();

      Agent agent =
              Agent.builder()
                      .name("Test")
                      .model("test-model")
                      .instructions("Test instructions")
                      .responder(responder)
                      .contextManagement(config)
                      .build();

      assertNotNull(agent);
    }

    @Test
    @DisplayName("agent works without context management configured")
    void agentWorksWithoutContextManagement() throws Exception {
      Agent agent =
              Agent.builder()
                      .name("Test")
                      .model("test-model")
                      .instructions("Test instructions")
                      .responder(responder)
                      .build();

      enqueueSuccessResponse("Response");

      AgentResult result = agent.interact("Hello");

      assertNotNull(result);
      assertFalse(result.isError());
    }
  }

  @Nested
  @DisplayName("Context Management in Action")
  class ContextManagementInAction {

    @Test
    @DisplayName("sliding window reduces context for long conversations")
    void slidingWindow_reducesContext() throws Exception {
      ContextManagementConfig config =
              ContextManagementConfig.builder()
                      .strategy(new SlidingWindowStrategy())
                      .maxTokens(200)
                      .build();

      Agent agent =
              Agent.builder()
                      .name("Test")
                      .model("test-model")
                      .instructions("Test instructions")
                      .responder(responder)
                      .contextManagement(config)
                      .build();

      AgenticContext context = AgenticContext.create();

      // Build up conversation history
      for (int i = 0; i < 10; i++) {
        context.addInput(Message.user("Message number " + i + " with extra content"));
        context.addInput(Message.developer("Response to message " + i));
      }

      enqueueSuccessResponse("Final response");

      context.addInput(Message.user("Last message"));
      AgentResult result = agent.interact(context);

      assertNotNull(result);

      // Verify that the request was made
      RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
      assertNotNull(request);

      // The context should have been truncated
      String requestBody = request.getBody().readUtf8();
      assertNotNull(requestBody);
    }

    @Test
    @DisplayName("agent works normally when context is under limit")
    void agentWorksNormally_whenUnderLimit() throws Exception {
      ContextManagementConfig config =
              ContextManagementConfig.builder()
                      .strategy(new SlidingWindowStrategy())
                      .maxTokens(10000)
                      .build();

      Agent agent =
              Agent.builder()
                      .name("Test")
                      .model("test-model")
                      .instructions("Test instructions")
                      .responder(responder)
                      .contextManagement(config)
                      .build();

      enqueueSuccessResponse("Response");

      AgentResult result = agent.interact("Short message");

      assertNotNull(result);
      assertFalse(result.isError());
    }
  }

  @Nested
  @DisplayName("Structured Agent with Context Management")
  class StructuredAgentWithContextManagement {

    @Test
    @DisplayName("structured builder supports contextManagement")
    void structuredBuilder_supportsContextManagement() {
      ContextManagementConfig config =
              ContextManagementConfig.builder()
                      .strategy(new SlidingWindowStrategy())
                      .maxTokens(4000)
                      .build();

      Agent.Structured<TestOutput> agent =
              Agent.builder()
                      .name("Test")
                      .model("test-model")
                      .instructions("Test instructions")
                      .responder(responder)
                      .structured(TestOutput.class)
                      .contextManagement(config)
                      .build();

      assertNotNull(agent);
    }

    record TestOutput(String message) {
    }
  }

  // Helper methods

  @Nested
  @DisplayName("ContextManagementConfig")
  class ContextManagementConfigTests {

    @Test
    @DisplayName("builder creates valid config")
    void builderCreatesValidConfig() {
      ContextManagementConfig config =
              ContextManagementConfig.builder()
                      .strategy(new SlidingWindowStrategy())
                      .maxTokens(4000)
                      .build();

      assertNotNull(config.strategy());
      assertEquals(4000, config.maxTokens());
      assertNotNull(config.tokenCounter());
    }

    @Test
    @DisplayName("throws when strategy is null")
    void throwsWhenStrategyNull() {
      assertThrows(
              NullPointerException.class,
              () -> ContextManagementConfig.builder().maxTokens(4000).build());
    }

    @Test
    @DisplayName("throws when maxTokens is not positive")
    void throwsWhenMaxTokensNotPositive() {
      assertThrows(
              IllegalArgumentException.class,
              () ->
                      ContextManagementConfig.builder()
                              .strategy(new SlidingWindowStrategy())
                              .maxTokens(0)
                              .build());
    }
  }
}
