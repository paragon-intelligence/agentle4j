package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.Message;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for Agent.Structured and Agent.StructuredBuilder. */
@DisplayName("Agent.Structured")
class AgentStructuredTest {

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

  // ==================== StructuredBuilder Tests ====================

  @Nested
  @DisplayName("StructuredBuilder")
  class StructuredBuilderTests {

    @Test
    @DisplayName("creates structured agent with required fields")
    void createsStructuredAgentWithRequiredFields() {
      Agent.Structured<TestPerson> agent =
          Agent.builder()
              .name("PersonExtractor")
              .model("test-model")
              .instructions("Extract person info")
              .responder(responder)
              .structured(TestPerson.class)
              .build();

      assertNotNull(agent);
      assertEquals("PersonExtractor", agent.name());
      assertEquals(TestPerson.class, agent.outputType());
    }

    @Test
    @DisplayName("temperature can be configured via StructuredBuilder")
    void temperatureCanBeConfigured() {
      Agent.Structured<TestPerson> agent =
          Agent.builder()
              .name("Test")
              .model("test-model")
              .instructions("Test")
              .responder(responder)
              .structured(TestPerson.class)
              .temperature(0.5)
              .build();

      assertNotNull(agent);
    }

    @Test
    @DisplayName("maxTurns can be configured via StructuredBuilder")
    void maxTurnsCanBeConfigured() {
      Agent.Structured<TestPerson> agent =
          Agent.builder()
              .name("Test")
              .model("test-model")
              .instructions("Test")
              .responder(responder)
              .structured(TestPerson.class)
              .maxTurns(5)
              .build();

      assertNotNull(agent);
    }

    @Test
    @DisplayName("objectMapper can be configured via StructuredBuilder")
    void objectMapperCanBeConfigured() {
      Agent.Structured<TestPerson> agent =
          Agent.builder()
              .name("Test")
              .model("test-model")
              .instructions("Test")
              .responder(responder)
              .structured(TestPerson.class)
              .objectMapper(new com.fasterxml.jackson.databind.ObjectMapper())
              .build();

      assertNotNull(agent);
    }

    @Test
    @DisplayName("addInputGuardrail works via StructuredBuilder")
    void addInputGuardrailWorks() {
      Agent.Structured<TestPerson> agent =
          Agent.builder()
              .name("Test")
              .model("test-model")
              .instructions("Test")
              .responder(responder)
              .structured(TestPerson.class)
              .addInputGuardrail((input, ctx) -> GuardrailResult.passed())
              .build();

      assertNotNull(agent);
    }

    @Test
    @DisplayName("addOutputGuardrail works via StructuredBuilder")
    void addOutputGuardrailWorks() {
      Agent.Structured<TestPerson> agent =
          Agent.builder()
              .name("Test")
              .model("test-model")
              .instructions("Test")
              .responder(responder)
              .structured(TestPerson.class)
              .addOutputGuardrail((output, ctx) -> GuardrailResult.passed())
              .build();

      assertNotNull(agent);
    }

    @Test
    @DisplayName("telemetryContext can be configured via StructuredBuilder")
    void telemetryContextCanBeConfigured() {
      Agent.Structured<TestPerson> agent =
          Agent.builder()
              .name("Test")
              .model("test-model")
              .instructions("Test")
              .responder(responder)
              .structured(TestPerson.class)
              .telemetryContext(com.paragon.telemetry.TelemetryContext.builder().build())
              .build();

      assertNotNull(agent);
    }
  }

  // ==================== Structured interact Tests ====================

  @Nested
  @DisplayName("Structured Interact")
  class StructuredInteractTests {

    @Test
    @DisplayName("interact(String) returns StructuredAgentResult directly")
    void interactStringReturnsStructuredAgentResult() {
      Agent.Structured<TestPerson> agent = createTestStructuredAgent();
      enqueueStructuredResponse("{\"name\":\"John\",\"age\":30}");

      StructuredAgentResult<TestPerson> result = agent.interact("Extract John");

      assertNotNull(result);
      assertInstanceOf(StructuredAgentResult.class, result);
    }

    @Test
    @DisplayName("interact(String, Context) returns StructuredAgentResult directly")
    void interactStringContextReturnsStructuredAgentResult() {
      Agent.Structured<TestPerson> agent = createTestStructuredAgent();
      AgentContext context = AgentContext.create();
      enqueueStructuredResponse("{\"name\":\"Jane\",\"age\":25}");

      StructuredAgentResult<TestPerson> result =
          agent.interact("Extract Jane", context);

      assertNotNull(result);
    }

    @Test
    @DisplayName("interact(Context) returns StructuredAgentResult directly")
    void interactContextReturnsStructuredAgentResult() {
      Agent.Structured<TestPerson> agent = createTestStructuredAgent();
      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Extract Bob"));
      enqueueStructuredResponse("{\"name\":\"Bob\",\"age\":40}");

      StructuredAgentResult<TestPerson> result = agent.interact(context);

      assertNotNull(result);
    }

    @Test
    @DisplayName("interact returns result")
    void interactReturnsResult() throws Exception {
      Agent.Structured<TestPerson> agent = createTestStructuredAgent();
      enqueueStructuredResponse("{\"name\":\"Alice\",\"age\":28}");

      StructuredAgentResult<TestPerson> result =
          agent.interact("Extract Alice");

      assertNotNull(result);
    }
  }

  // Helper methods

  private Agent.Structured<TestPerson> createTestStructuredAgent() {
    return Agent.builder()
        .name("TestStructured")
        .model("test-model")
        .instructions("Extract person info")
        .responder(responder)
        .structured(TestPerson.class)
        .build();
  }

  private void enqueueStructuredResponse(String jsonOutput) {
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
            "output_tokens": 20,
            "total_tokens": 30
          }
        }
        """
            .formatted(jsonOutput.replace("\"", "\\\""));

    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(json)
            .addHeader("Content-Type", "application/json"));
  }

  // Test record
  public record TestPerson(String name, int age) {}
}
