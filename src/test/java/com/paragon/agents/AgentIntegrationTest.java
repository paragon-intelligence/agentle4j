package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.Responder;
import com.paragon.responses.exception.GuardrailException;
import com.paragon.responses.spec.*;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Agent.java covering builder validation, guardrails, error handling, and
 * different input types.
 */
@DisplayName("Agent Integration Tests")
class AgentIntegrationTest {

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
  // ERROR HANDLING TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  @DisplayName("LLM API error returns AgentResult with error")
  void llmApiErrorReturnsAgentResultWithError() {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(500)
            .setBody("{\"error\": {\"message\": \"Internal server error\"}}"));

    Agent agent =
        Agent.builder()
            .name("ErrorAgent")
            .model("test-model")
            .instructions("Test")
            .responder(responder)
            .build();

    AgentResult result = agent.interact("Hello");

    assertTrue(result.isError());
    assertNotNull(result.error());
  }

  @Test
  @DisplayName("rate limit error is wrapped correctly")
  void rateLimitErrorIsWrappedCorrectly() {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(429)
            .setHeader("Retry-After", "60")
            .setBody("{\"error\": {\"message\": \"Rate limit exceeded\"}}"));

    Agent agent =
        Agent.builder()
            .name("RateLimitedAgent")
            .model("test-model")
            .instructions("Test")
            .responder(responder)
            .build();

    AgentResult result = agent.interact("Hello");

    assertTrue(result.isError());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // GUARDRAIL TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  @DisplayName("input guardrail blocks request before LLM call")
  void inputGuardrailBlocksRequestBeforeLLMCall() {
    Agent agent =
        Agent.builder()
            .name("GuardedAgent")
            .model("test-model")
            .instructions("Test")
            .responder(responder)
            .addInputGuardrail(
                (input, ctx) -> {
                  if (input.contains("forbidden")) {
                    return GuardrailResult.failed("Forbidden content detected");
                  }
                  return GuardrailResult.passed();
                })
            .build();

    AgentResult result = agent.interact("This is forbidden content");

    assertTrue(result.isError());
    assertTrue(result.error() instanceof GuardrailException);
    assertEquals(0, mockWebServer.getRequestCount());
  }

  @Test
  @DisplayName("multiple input guardrails are all executed")
  void multipleInputGuardrailsAreAllExecuted() {
    Agent agent =
        Agent.builder()
            .name("MultiGuardAgent")
            .model("test-model")
            .instructions("Test")
            .responder(responder)
            .addInputGuardrail((input, ctx) -> GuardrailResult.passed())
            .addInputGuardrail(
                (input, ctx) -> {
                  if (input.contains("block")) {
                    return GuardrailResult.failed("Blocked by second guardrail");
                  }
                  return GuardrailResult.passed();
                })
            .build();

    AgentResult result = agent.interact("This should block");

    assertTrue(result.isError());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // DIFFERENT INPUT TYPE TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  @DisplayName("interact with Text input type")
  void interactWithTextInputType() {
    enqueueSuccessResponse("Text received");

    Agent agent = createBasicAgent();
    Text textInput = new Text("Hello from Text");

    AgentResult result = agent.interact(textInput);

    assertTrue(result.isSuccess());
  }

  @Test
  @DisplayName("interact with Message input type")
  void interactWithMessageInputType() {
    enqueueSuccessResponse("Message received");

    Agent agent = createBasicAgent();
    Message message = Message.user("Hello from Message");

    AgentResult result = agent.interact(message);

    assertTrue(result.isSuccess());
  }

  @Test
  @DisplayName("interact with AgentContext")
  void interactWithAgentContext() {
    enqueueSuccessResponse("Context received");

    Agent agent = createBasicAgent();
    AgentContext context = AgentContext.create();
    context.addInput(Message.user("Hello from context"));

    AgentResult result = agent.interact(context);

    assertTrue(result.isSuccess());
  }

  @Test
  @DisplayName("interact with List of ResponseInputItems")
  void interactWithListOfResponseInputItems() {
    enqueueSuccessResponse("Items received");

    Agent agent = createBasicAgent();
    List<ResponseInputItem> items =
        List.of(Message.user("First message"), Message.user("Second message"));

    AgentResult result = agent.interact(items);

    assertTrue(result.isSuccess());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BUILDER CONFIGURATION TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  @DisplayName("builder validates required fields")
  void builderValidatesRequiredFields() {
    assertThrows(
        NullPointerException.class,
        () -> {
          Agent.builder().model("test-model").instructions("Test").responder(responder).build();
        });

    assertThrows(
        NullPointerException.class,
        () -> {
          Agent.builder().name("Test").instructions("Test").responder(responder).build();
        });

    assertThrows(
        NullPointerException.class,
        () -> {
          Agent.builder().name("Test").model("test-model").instructions("Test").build();
        });
  }

  @Test
  @DisplayName("builder rejects invalid maxTurns")
  void builderRejectsInvalidMaxTurns() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          Agent.builder()
              .name("Test")
              .model("test-model")
              .instructions("Test")
              .responder(responder)
              .maxTurns(0)
              .build();
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          Agent.builder()
              .name("Test")
              .model("test-model")
              .instructions("Test")
              .responder(responder)
              .maxTurns(-1)
              .build();
        });
  }

  @Test
  @DisplayName("builder rejects invalid temperature")
  void builderRejectsInvalidTemperature() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          Agent.builder()
              .name("Test")
              .model("test-model")
              .instructions("Test")
              .responder(responder)
              .temperature(-0.1)
              .build();
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          Agent.builder()
              .name("Test")
              .model("test-model")
              .instructions("Test")
              .responder(responder)
              .temperature(2.1)
              .build();
        });
  }

  @Test
  @DisplayName("builder accepts valid configuration options")
  void builderAcceptsValidConfigurationOptions() {
    Agent agent =
        Agent.builder()
            .name("FullyConfigured")
            .model("test-model")
            .instructions("Full configuration")
            .responder(responder)
            .maxTurns(5)
            .temperature(0.7)
            .maxOutputTokens(1000)
            .metadata(Map.of("key", "value"))
            .objectMapper(new ObjectMapper())
            .build();

    assertEquals("FullyConfigured", agent.name());
    assertEquals("test-model", agent.model());
    assertEquals(5, agent.maxTurns());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CONTEXT STATE TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  @DisplayName("context state is preserved across turns")
  void contextStateIsPreservedAcrossTurns() {
    enqueueSuccessResponse("First response");
    enqueueSuccessResponse("Second response");

    Agent agent = createBasicAgent();
    AgentContext context = AgentContext.create();
    context.setState("userId", "user-123");

    context.addInput(Message.user("First message"));
    agent.interact(context);

    context.addInput(Message.user("Second message"));
    AgentResult result = agent.interact(context);

    assertTrue(result.isSuccess());
    assertEquals("user-123", context.getState("userId", String.class).orElse(null));
  }

  @Test
  @DisplayName("conversation history grows with each turn")
  void conversationHistoryGrowsWithEachTurn() {
    enqueueSuccessResponse("Response 1");
    enqueueSuccessResponse("Response 2");

    Agent agent = createBasicAgent();
    AgentContext context = AgentContext.create();

    context.addInput(Message.user("Message 1"));
    agent.interact(context);

    int historyAfterFirst = context.getHistory().size();

    context.addInput(Message.user("Message 2"));
    agent.interact(context);

    int historyAfterSecond = context.getHistory().size();

    assertTrue(historyAfterSecond > historyAfterFirst);
  }

  @Test
  @DisplayName("output guardrail blocks response after LLM call")
  void outputGuardrailBlocksResponseAfterLLMCall() {
    enqueueSuccessResponse("This response contains BAD content");

    Agent agent =
        Agent.builder()
            .name("GuardedAgent")
            .model("test-model")
            .instructions("Test")
            .responder(responder)
            .addOutputGuardrail(
                (output, ctx) -> {
                  if (output.contains("BAD")) {
                    return GuardrailResult.failed("Bad content in output");
                  }
                  return GuardrailResult.passed();
                })
            .build();

    AgentResult result = agent.interact("Generate something");

    assertTrue(result.isError());
    assertTrue(result.error() instanceof GuardrailException);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPER METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  private Agent createBasicAgent() {
    return Agent.builder()
        .name("BasicAgent")
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
            .addHeader("Content-Type", "application/json")
            .setBody(json));
  }
}
