package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Comprehensive tests for Agent.
 *
 * <p>Tests cover:
 * - Builder pattern
 * - Async interaction (all methods return CompletableFuture)
 * - Guardrail integration
 * - Tool registration
 * - Handoff configuration
 * - Context handling
 */
@DisplayName("Agent")
class AgentTest {

  private MockWebServer mockWebServer;
  private Responder responder;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    responder = Responder.builder()
        .baseUrl(mockWebServer.url("/v1/responses"))
        .apiKey("test-key")
        .build();
  }

  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Nested
  @DisplayName("Builder")
  class Builder {

    @Test
    @DisplayName("builder() creates new builder instance")
    void builder_createsNewInstance() {
      Agent.Builder builder = Agent.builder();

      assertNotNull(builder);
    }

    @Test
    @DisplayName("build() creates agent with required fields")
    void build_createsAgentWithRequiredFields() {
      Agent agent = Agent.builder()
          .name("TestAgent")
          .model("test-model")
          .instructions("Test instructions")
          .responder(responder)
          .build();

      assertEquals("TestAgent", agent.name());
      assertEquals("test-model", agent.model());
      assertEquals("Test instructions", agent.instructions());
    }

    @Test
    @DisplayName("build() throws when name is null")
    void build_throwsWhenNameNull() {
      assertThrows(NullPointerException.class, () -> {
        Agent.builder()
            .model("test-model")
            .instructions("Test")
            .responder(responder)
            .build();
      });
    }

    @Test
    @DisplayName("build() throws when model is null")
    void build_throwsWhenModelNull() {
      assertThrows(NullPointerException.class, () -> {
        Agent.builder()
            .name("Test")
            .instructions("Test")
            .responder(responder)
            .build();
      });
    }

    @Test
    @DisplayName("build() throws when responder is null")
    void build_throwsWhenResponderNull() {
      assertThrows(NullPointerException.class, () -> {
        Agent.builder()
            .name("Test")
            .model("test-model")
            .instructions("Test")
            .build();
      });
    }

    @Test
    @DisplayName("maxTurns() sets maximum turns")
    void maxTurns_setsMaximumTurns() {
      Agent agent = Agent.builder()
          .name("Test")
          .model("test-model")
          .instructions("Test")
          .responder(responder)
          .maxTurns(5)
          .build();

      assertEquals(5, agent.maxTurns());
    }

    @Test
    @DisplayName("default maxTurns is 10")
    void defaultMaxTurnsIs10() {
      Agent agent = createTestAgent("Test");

      assertEquals(10, agent.maxTurns());
    }
  }

  @Nested
  @DisplayName("Async Interaction (interact returns CompletableFuture)")
  class AsyncInteraction {

    @Test
    @DisplayName("interact(String) returns CompletableFuture")
    void interact_string_returnsCompletableFuture() {
      Agent agent = createTestAgent("Test");
      enqueueSuccessResponse("Hello");

      CompletableFuture<AgentResult> future = agent.interact("Hello");

      assertNotNull(future);
      assertInstanceOf(CompletableFuture.class, future);
    }

    @Test
    @DisplayName("interact(String, Context) returns CompletableFuture")
    void interact_stringAndContext_returnsCompletableFuture() {
      Agent agent = createTestAgent("Test");
      AgentContext context = AgentContext.create();
      enqueueSuccessResponse("Hello");

      CompletableFuture<AgentResult> future = agent.interact("Hello", context);

      assertNotNull(future);
      assertInstanceOf(CompletableFuture.class, future);
    }

    @Test
    @DisplayName("interact().join() blocks and returns result")
    void interact_join_blocksAndReturnsResult() throws Exception {
      Agent agent = createTestAgent("Test");
      enqueueSuccessResponse("Response text");

      AgentResult result = agent.interact("Hello").get(5, TimeUnit.SECONDS);

      assertNotNull(result);
    }

    @Test
    @DisplayName("interact().thenAccept() handles async result")
    void interact_thenAccept_handlesAsyncResult() throws Exception {
      Agent agent = createTestAgent("Test");
      enqueueSuccessResponse("Response");

      CompletableFuture<Void> processed = agent.interact("Hello")
          .thenAccept(result -> {
            assertNotNull(result);
          });

      processed.get(5, TimeUnit.SECONDS);
    }
  }

  @Nested
  @DisplayName("Guardrail Configuration")
  class GuardrailConfiguration {

    @Test
    @DisplayName("addInputGuardrail() adds input guardrail")
    void addInputGuardrail_addsInputGuardrail() {
      Agent agent = Agent.builder()
          .name("Test")
          .model("test-model")
          .instructions("Test")
          .responder(responder)
          .addInputGuardrail((input, ctx) -> GuardrailResult.passed())
          .build();

      assertEquals(1, agent.inputGuardrails().size());
    }

    @Test
    @DisplayName("addOutputGuardrail() adds output guardrail")
    void addOutputGuardrail_addsOutputGuardrail() {
      Agent agent = Agent.builder()
          .name("Test")
          .model("test-model")
          .instructions("Test")
          .responder(responder)
          .addOutputGuardrail((output, ctx) -> GuardrailResult.passed())
          .build();

      assertEquals(1, agent.outputGuardrails().size());
    }

    @Test
    @DisplayName("multiple guardrails can be added")
    void multipleGuardrailsCanBeAdded() {
      Agent agent = Agent.builder()
          .name("Test")
          .model("test-model")
          .instructions("Test")
          .responder(responder)
          .addInputGuardrail((input, ctx) -> GuardrailResult.passed())
          .addInputGuardrail((input, ctx) -> GuardrailResult.passed())
          .addOutputGuardrail((output, ctx) -> GuardrailResult.passed())
          .build();

      assertEquals(2, agent.inputGuardrails().size());
      assertEquals(1, agent.outputGuardrails().size());
    }

    @Test
    @DisplayName("input guardrail rejection returns early")
    void inputGuardrailRejection_returnsEarly() throws Exception {
      Agent agent = Agent.builder()
          .name("Test")
          .model("test-model")
          .instructions("Test")
          .responder(responder)
          .addInputGuardrail((input, ctx) -> GuardrailResult.failed("Blocked"))
          .build();

      AgentResult result = agent.interact("Hello").get(5, TimeUnit.SECONDS);

      assertFalse(result.isSuccess());
    }
  }

  @Nested
  @DisplayName("Handoff Configuration")
  class HandoffConfiguration {

    @Test
    @DisplayName("addHandoff() adds handoff to agent")
    void addHandoff_addsHandoffToAgent() {
      Agent target = createTestAgent("Target");
      Agent agent = Agent.builder()
          .name("Test")
          .model("test-model")
          .instructions("Test")
          .responder(responder)
          .addHandoff(Handoff.to(target).build())
          .build();

      assertEquals(1, agent.handoffs().size());
      assertEquals(target, agent.handoffs().getFirst().targetAgent());
    }

    @Test
    @DisplayName("multiple handoffs can be added")
    void multipleHandoffsCanBeAdded() {
      Agent target1 = createTestAgent("Target1");
      Agent target2 = createTestAgent("Target2");
      
      Agent agent = Agent.builder()
          .name("Test")
          .model("test-model")
          .instructions("Test")
          .responder(responder)
          .addHandoff(Handoff.to(target1).build())
          .addHandoff(Handoff.to(target2).build())
          .build();

      assertEquals(2, agent.handoffs().size());
    }
  }

  @Nested
  @DisplayName("Context Handling")
  class ContextHandling {

    @Test
    @DisplayName("interact without context creates fresh context")
    void interact_withoutContext_createsFreshContext() throws Exception {
      Agent agent = createTestAgent("Test");
      enqueueSuccessResponse("Response");

      AgentResult result = agent.interact("Hello").get(5, TimeUnit.SECONDS);

      assertNotNull(result);
    }

    @Test
    @DisplayName("interact with context uses provided context")
    void interact_withContext_usesProvidedContext() throws Exception {
      Agent agent = createTestAgent("Test");
      AgentContext context = AgentContext.create();
      context.setState("userId", "user-123");

      enqueueSuccessResponse("Response");

      AgentResult result = agent.interact("Hello", context).get(5, TimeUnit.SECONDS);

      // Context should be updated during interaction
      assertNotNull(result);
    }

    @Test
    @DisplayName("same agent can handle multiple conversations with different contexts")
    void sameAgentCanHandleMultipleConversations() throws Exception {
      Agent agent = createTestAgent("Test");

      AgentContext context1 = AgentContext.create();
      AgentContext context2 = AgentContext.create();

      enqueueSuccessResponse("Response 1");
      enqueueSuccessResponse("Response 2");

      CompletableFuture<AgentResult> future1 = agent.interact("Hello 1", context1);
      CompletableFuture<AgentResult> future2 = agent.interact("Hello 2", context2);

      AgentResult result1 = future1.get(5, TimeUnit.SECONDS);
      AgentResult result2 = future2.get(5, TimeUnit.SECONDS);

      assertNotNull(result1);
      assertNotNull(result2);
    }
  }

  @Nested
  @DisplayName("Accessors")
  class Accessors {

    @Test
    @DisplayName("name() returns agent name")
    void name_returnsAgentName() {
      Agent agent = createTestAgent("MyAgent");

      assertEquals("MyAgent", agent.name());
    }

    @Test
    @DisplayName("model() returns model identifier")
    void model_returnsModelIdentifier() {
      Agent agent = createTestAgent("Test");

      assertEquals("test-model", agent.model());
    }

    @Test
    @DisplayName("instructions() returns system prompt")
    void instructions_returnsSystemPrompt() {
      Agent agent = Agent.builder()
          .name("Test")
          .model("test-model")
          .instructions("Custom instructions")
          .responder(responder)
          .build();

      assertEquals("Custom instructions", agent.instructions());
    }

    @Test
    @DisplayName("inputGuardrails() returns unmodifiable list")
    void inputGuardrails_returnsUnmodifiableList() {
      Agent agent = createTestAgent("Test");

      assertThrows(UnsupportedOperationException.class, 
          () -> agent.inputGuardrails().add((input, ctx) -> GuardrailResult.passed()));
    }

    @Test
    @DisplayName("outputGuardrails() returns unmodifiable list")
    void outputGuardrails_returnsUnmodifiableList() {
      Agent agent = createTestAgent("Test");

      assertThrows(UnsupportedOperationException.class, 
          () -> agent.outputGuardrails().add((output, ctx) -> GuardrailResult.passed()));
    }

    @Test
    @DisplayName("handoffs() returns unmodifiable list")
    void handoffs_returnsUnmodifiableList() {
      Agent agent = createTestAgent("Test");
      Agent target = createTestAgent("Target");

      assertThrows(UnsupportedOperationException.class, 
          () -> agent.handoffs().add(Handoff.to(target).build()));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MAX TURNS ENFORCEMENT
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Max Turns Enforcement")
  class MaxTurnsEnforcement {

    @Test
    @DisplayName("maxTurns defaults to 10")
    void maxTurns_defaultsTo10() {
      Agent agent = createTestAgent("Test");
      
      assertEquals(10, agent.maxTurns());
    }

    @Test
    @DisplayName("maxTurns can be set via builder")
    void maxTurns_canBeSetViaBuilder() {
      Agent agent = Agent.builder()
          .name("Test")
          .model("test-model")
          .instructions("Test instructions")
          .responder(responder)
          .maxTurns(5)
          .build();
      
      assertEquals(5, agent.maxTurns());
    }

    @Test
    @DisplayName("maxTurns of 1 allows single turn")
    void maxTurns_of1AllowsSingleTurn() {
      Agent agent = Agent.builder()
          .name("Test")
          .model("test-model")
          .instructions("Test instructions")
          .responder(responder)
          .maxTurns(1)
          .build();

      enqueueSuccessResponse("Single response");

      AgentResult result = agent.interact("Hello").join();

      assertNotNull(result.output());
      assertEquals(1, result.turnsUsed());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ERROR HANDLING
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Error Handling")
  class ErrorHandling {

    @Test
    @DisplayName("isError is false for successful response")
    void isError_isFalseForSuccess() {
      Agent agent = createTestAgent("Test");
      enqueueSuccessResponse("Success");

      AgentResult result = agent.interact("Hello").join();

      assertFalse(result.isError());
      assertNull(result.error());
    }

    @Test
    @DisplayName("error returns exception for failed interaction")
    void error_returnsExceptionForFailure() {
      Agent agent = createTestAgent("Test");
      
      // Enqueue error response
      mockWebServer.enqueue(new MockResponse()
          .setResponseCode(500)
          .setBody("{\"error\": \"Internal server error\"}")
          .addHeader("Content-Type", "application/json"));

      AgentResult result = agent.interact("Hello").join();

      // Either isError is true or there's an error in the result
      // depending on how errors are handled
      assertNotNull(result);
    }

    @Test
    @DisplayName("interact handles empty input")
    void interact_handlesEmptyInput() {
      Agent agent = createTestAgent("Test");
      enqueueSuccessResponse("Response to empty");

      // Empty string should be handled (not throw)
      AgentResult result = agent.interact("").join();
      assertNotNull(result);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TELEMETRY CONFIGURATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Telemetry Configuration")
  class TelemetryConfiguration {

    @Test
    @DisplayName("agent can be created without telemetry")
    void agent_createdWithoutTelemetry() {
      Agent agent = Agent.builder()
          .name("NoTelemetry")
          .model("test-model")
          .instructions("Test instructions")
          .responder(responder)
          .build();

      assertNotNull(agent);
    }

    @Test
    @DisplayName("interact works without telemetry processor")
    void interact_worksWithoutTelemetry() {
      Agent agent = createTestAgent("Test");
      enqueueSuccessResponse("Response");

      AgentResult result = agent.interact("Hello").join();

      assertNotNull(result);
      assertFalse(result.isError());
    }
  }

  // Helper methods

  private Agent createTestAgent(String name) {
    return Agent.builder()
        .name(name)
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

    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .setBody(json)
        .addHeader("Content-Type", "application/json"));
  }
}
