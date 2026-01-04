package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.Responder;
import com.paragon.responses.exception.GuardrailException;
import com.paragon.responses.spec.Message;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for Agent agentic loop internals - guardrails, result methods. */
@DisplayName("Agent Agentic Loop")
class AgentAgenticLoopTest {

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
  // INPUT GUARDRAIL TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Input Guardrails")
  class InputGuardrailTests {

    @Test
    @DisplayName("input guardrail failure returns error result")
    void inputGuardrailFailureReturnsError() throws Exception {
      Agent agent =
          Agent.builder()
              .name("GuardrailAgent")
              .model("test-model")
              .instructions("Test")
              .responder(responder)
              .addInputGuardrail(
                  (input, ctx) -> {
                    if (input.contains("blocked")) {
                      return GuardrailResult.failed("Blocked content detected");
                    }
                    return GuardrailResult.passed();
                  })
              .build();

      AgentResult result = agent.interact("This contains blocked content").get(5, TimeUnit.SECONDS);

      assertFalse(result.isSuccess());
      assertTrue(result.isError());
      assertNotNull(result.error());
      assertInstanceOf(GuardrailException.class, result.error());
    }

    @Test
    @DisplayName("input guardrail passing allows execution")
    void inputGuardrailPassingAllowsExecution() throws Exception {
      Agent agent =
          Agent.builder()
              .name("GuardrailAgent")
              .model("test-model")
              .instructions("Test")
              .responder(responder)
              .addInputGuardrail((input, ctx) -> GuardrailResult.passed())
              .build();

      enqueueSuccessResponse("Hello!");

      AgentResult result = agent.interact("Safe input").get(5, TimeUnit.SECONDS);

      assertTrue(result.isSuccess());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // OUTPUT GUARDRAIL TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Output Guardrails")
  class OutputGuardrailTests {

    @Test
    @DisplayName("output guardrail failure returns error")
    void outputGuardrailFailureReturnsError() throws Exception {
      Agent agent =
          Agent.builder()
              .name("OutputGuard")
              .model("test-model")
              .instructions("Test")
              .responder(responder)
              .addOutputGuardrail(
                  (output, ctx) -> {
                    if (output.contains("forbidden")) {
                      return GuardrailResult.failed("Forbidden output detected");
                    }
                    return GuardrailResult.passed();
                  })
              .build();

      enqueueSuccessResponse("This is forbidden output");

      AgentResult result = agent.interact("Give me response").get(5, TimeUnit.SECONDS);

      assertFalse(result.isSuccess());
      assertTrue(result.isError());
      assertInstanceOf(GuardrailException.class, result.error());
    }

    @Test
    @DisplayName("output guardrail passing returns success")
    void outputGuardrailPassingReturnsSuccess() throws Exception {
      Agent agent =
          Agent.builder()
              .name("OutputGuard")
              .model("test-model")
              .instructions("Test")
              .responder(responder)
              .addOutputGuardrail((output, ctx) -> GuardrailResult.passed())
              .build();

      enqueueSuccessResponse("Safe output");

      AgentResult result = agent.interact("Test").get(5, TimeUnit.SECONDS);

      assertTrue(result.isSuccess());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // AGENT RESULT METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("AgentResult Methods")
  class AgentResultTests {

    @Test
    @DisplayName("isSuccess returns true for successful result")
    void isSuccessReturnsTrueForSuccess() throws Exception {
      Agent agent = createSimpleAgent();
      enqueueSuccessResponse("Hello!");

      AgentResult result = agent.interact("Hi").get(5, TimeUnit.SECONDS);

      assertTrue(result.isSuccess());
      assertFalse(result.isError());
      assertFalse(result.isPaused());
      assertFalse(result.isHandoff());
    }

    @Test
    @DisplayName("output returns response text")
    void outputReturnsResponseText() throws Exception {
      Agent agent = createSimpleAgent();
      enqueueSuccessResponse("Test response text");

      AgentResult result = agent.interact("Input").get(5, TimeUnit.SECONDS);

      assertTrue(result.output().contains("Test response text"));
    }

    @Test
    @DisplayName("turnsUsed returns turn count")
    void turnsUsedReturnsTurnCount() throws Exception {
      Agent agent = createSimpleAgent();
      enqueueSuccessResponse("Response");

      AgentResult result = agent.interact("Input").get(5, TimeUnit.SECONDS);

      assertEquals(1, result.turnsUsed());
    }

    @Test
    @DisplayName("toolExecutions returns empty list when no tools called")
    void toolExecutionsReturnsEmptyList() throws Exception {
      Agent agent = createSimpleAgent();
      enqueueSuccessResponse("Response");

      AgentResult result = agent.interact("Input").get(5, TimeUnit.SECONDS);

      assertNotNull(result.toolExecutions());
      assertTrue(result.toolExecutions().isEmpty());
    }

    @Test
    @DisplayName("history returns conversation history")
    void historyReturnsConversationHistory() throws Exception {
      Agent agent = createSimpleAgent();
      enqueueSuccessResponse("Response");

      AgentResult result = agent.interact("Input").get(5, TimeUnit.SECONDS);

      assertNotNull(result.history());
      assertFalse(result.history().isEmpty());
    }

    @Test
    @DisplayName("finalResponse returns the last response")
    void finalResponseReturnsLastResponse() throws Exception {
      Agent agent = createSimpleAgent();
      enqueueSuccessResponse("Response");

      AgentResult result = agent.interact("Input").get(5, TimeUnit.SECONDS);

      assertNotNull(result.finalResponse());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TRACE CONTEXT
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Trace Context")
  class TraceContextTests {

    @Test
    @DisplayName("uses existing trace context if set")
    void usesExistingTraceContext() throws Exception {
      Agent agent = createSimpleAgent();
      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Hello"));
      context.withTraceContext("custom-trace-id", "custom-span-id");

      assertTrue(context.hasTraceContext());

      enqueueSuccessResponse("Response");
      agent.interact(context).get(5, TimeUnit.SECONDS);

      // Context should still have the trace we set
      assertEquals("custom-trace-id", context.parentTraceId().orElse(null));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPER METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  private Agent createSimpleAgent() {
    return Agent.builder()
        .name("TestAgent")
        .model("test-model")
        .instructions("You are a test agent")
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
