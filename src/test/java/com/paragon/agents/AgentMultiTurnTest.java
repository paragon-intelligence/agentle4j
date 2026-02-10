package com.paragon.agents;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.Message;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Agent multi-turn conversation behavior.
 *
 * <p>Tests cover: - Context reuse across multiple interact() calls - History accumulation
 * verification - Turn count tracking - State preservation
 */
@DisplayName("Agent Multi-Turn Tests")
class AgentMultiTurnTest {

  private MockWebServer mockWebServer;
  private Responder responder;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    responder =
            Responder.builder().openRouter().apiKey("test-key").baseUrl(mockWebServer.url("/")).build();
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CONTEXT REUSE
  // ═══════════════════════════════════════════════════════════════════════════

  private Agent createTestAgent(String name) {
    return Agent.builder()
            .name(name)
            .instructions("You are a helpful assistant.")
            .model("test-model")
            .responder(responder)
            .build();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TURN COUNTING
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

  // ═══════════════════════════════════════════════════════════════════════════
  // HISTORY VERIFICATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Context Reuse")
  class ContextReuse {

    @Test
    @DisplayName("same context accumulates history across calls")
    void sameContext_accumulatesHistory() {
      Agent agent = createTestAgent("TestAgent");
      AgenticContext context = AgenticContext.create();

      // Enqueue responses for two turns
      enqueueSuccessResponse("Response 1");
      enqueueSuccessResponse("Response 2");

      // First call
      assertEquals(0, context.historySize());
      context.addInput(Message.user("First message"));
      agent.interact(context);
      int historyAfterFirst = context.historySize();
      assertTrue(historyAfterFirst > 0);

      // Second call with same context
      context.addInput(Message.user("Second message"));
      agent.interact(context);
      int historyAfterSecond = context.historySize();
      assertTrue(historyAfterSecond > historyAfterFirst);
    }

    @Test
    @DisplayName("new context starts fresh")
    void newContext_startsFresh() {
      Agent agent = createTestAgent("TestAgent");

      enqueueSuccessResponse("Response 1");
      enqueueSuccessResponse("Response 2");

      AgenticContext context1 = AgenticContext.create();
      context1.addInput(Message.user("Message 1"));
      agent.interact(context1);

      AgenticContext context2 = AgenticContext.create();
      assertEquals(0, context2.historySize());
      context2.addInput(Message.user("Message 2"));
      agent.interact(context2);

      // context1 and context2 should have independent histories
      assertTrue(context1.historySize() > 0);
      assertTrue(context2.historySize() > 0);
    }

    @Test
    @DisplayName("context state persists across calls")
    void contextState_persistsAcrossCalls() {
      Agent agent = createTestAgent("TestAgent");
      AgenticContext context = AgenticContext.create();

      enqueueSuccessResponse("Response 1");
      enqueueSuccessResponse("Response 2");

      context.setState("userId", "user-123");
      context.addInput(Message.user("First"));
      agent.interact(context);

      // State should still be there
      assertEquals("user-123", context.getState("userId").orElse(null));

      context.addInput(Message.user("Second"));
      agent.interact(context);
      assertEquals("user-123", context.getState("userId").orElse(null));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CONVERSATION FLOW
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Turn Counting")
  class TurnCounting {

    @Test
    @DisplayName("turnsUsed reflects LLM calls in single interaction")
    void turnsUsed_reflectsLLMCalls() {
      Agent agent = createTestAgent("TestAgent");

      enqueueSuccessResponse("Simple response");

      AgentResult result = agent.interact("Hello");

      assertEquals(1, result.turnsUsed());
    }

    @Test
    @DisplayName("turnsUsed reflects LLM calls in interaction")
    void turnsUsed_reflectsLLMCallsInInteraction() {
      Agent agent = createTestAgent("TestAgent");
      AgenticContext context = AgenticContext.create();

      enqueueSuccessResponse("Response 1");
      enqueueSuccessResponse("Response 2");

      context.addInput(Message.user("First"));
      AgentResult result1 = agent.interact(context);
      assertTrue(result1.turnsUsed() >= 1);

      context.addInput(Message.user("Second"));
      AgentResult result2 = agent.interact(context);
      assertTrue(result2.turnsUsed() >= 1);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPERS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("History Verification")
  class HistoryVerification {

    @Test
    @DisplayName("history contains user messages")
    void history_containsUserMessages() {
      Agent agent = createTestAgent("TestAgent");
      AgenticContext context = AgenticContext.create();

      enqueueSuccessResponse("Response");

      context.addInput(Message.user("User said this"));
      agent.interact(context);

      // History should have at least the user message
      assertTrue(context.historySize() > 0);
    }

    @Test
    @DisplayName("history is accessible from result")
    void history_accessibleFromResult() {
      Agent agent = createTestAgent("TestAgent");

      enqueueSuccessResponse("Response text");

      AgentResult result = agent.interact("Hello");

      assertNotNull(result.history());
      assertFalse(result.history().isEmpty());
    }

    @Test
    @DisplayName("context copy creates independent history")
    void contextCopy_createsIndependentHistory() {
      Agent agent = createTestAgent("TestAgent");
      AgenticContext original = AgenticContext.create();

      enqueueSuccessResponse("Response 1");
      original.addInput(Message.user("Original message"));
      agent.interact(original);

      AgenticContext copy = original.copy();
      assertEquals(original.historySize(), copy.historySize());

      enqueueSuccessResponse("Response 2");
      original.addInput(Message.user("New message"));
      agent.interact(original);

      // Copy should not have the new message
      assertTrue(original.historySize() > copy.historySize());
    }
  }

  @Nested
  @DisplayName("Conversation Flow")
  class ConversationFlow {

    @Test
    @DisplayName("multiple interactions with same context succeed")
    void multipleInteractions_succeed() {
      Agent agent = createTestAgent("TestAgent");
      AgenticContext context = AgenticContext.create();

      for (int i = 0; i < 5; i++) {
        enqueueSuccessResponse("Response " + i);
      }

      for (int i = 0; i < 5; i++) {
        context.addInput(Message.user("Message " + i));
        AgentResult result = agent.interact(context);
        assertNotNull(result.output());
        assertFalse(result.isError());
      }

      assertTrue(context.historySize() >= 5);
    }

    @Test
    @DisplayName("cleared context starts fresh conversation")
    void clearedContext_startsFresh() {
      Agent agent = createTestAgent("TestAgent");
      AgenticContext context = AgenticContext.create();

      enqueueSuccessResponse("First response");
      context.addInput(Message.user("First message"));
      agent.interact(context);
      int historyBefore = context.historySize();
      assertTrue(historyBefore > 0);

      context.clear();
      assertEquals(0, context.historySize());

      enqueueSuccessResponse("After clear");
      context.addInput(Message.user("After clear"));
      agent.interact(context);
      // History started fresh
      assertTrue(context.historySize() < historyBefore * 2);
    }
  }
}
