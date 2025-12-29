package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.paragon.responses.Responder;

import org.jspecify.annotations.NonNull;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Tests for Agent.Structured<T> functionality.
 *
 * <p>Tests cover:
 * - Structured agent creation via Agent.builder().structured(Class)
 * - JSON parsing to typed objects
 * - StructuredAgentResult handling
 */
@DisplayName("Agent Structured Output Tests")
class AgentStructuredOutputTest {

  private MockWebServer mockWebServer;
  private Responder responder;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    responder = Responder.builder()
        .openRouter()
        .apiKey("test-key")
        .baseUrl(mockWebServer.url("/"))
        .build();
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // STRUCTURED AGENT CREATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Structured Agent Creation")
  class StructuredAgentCreation {

    @Test
    @DisplayName("structured agent is created from builder")
    void structuredAgent_createdFromBuilder() {
      Agent.Structured<PersonInfo> structured = Agent.builder()
          .structured(PersonInfo.class)
          .name("PersonExtractor")
          .instructions("Extract person information.")
          .model("test-model")
          .responder(responder)
          .build();

      assertNotNull(structured);
    }

    @Test
    @DisplayName("structured agent returns output type")
    void structuredAgent_returnsOutputType() {
      Agent.Structured<PersonInfo> structured = Agent.builder()
          .structured(PersonInfo.class)
          .name("PersonExtractor")
          .instructions("Extract person info.")
          .model("test-model")
          .responder(responder)
          .build();

      assertEquals(PersonInfo.class, structured.outputType());
    }

    @Test
    @DisplayName("structured agent has name")
    void structuredAgent_hasName() {
      Agent.Structured<PersonInfo> structured = Agent.builder()
          .structured(PersonInfo.class)
          .name("PersonExtractor")
          .instructions("Extract person info.")
          .model("test-model")
          .responder(responder)
          .build();

      assertEquals("PersonExtractor", structured.name());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // STRUCTURED INTERACTION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Structured Interaction")
  class StructuredInteraction {

    @Test
    @DisplayName("interact returns StructuredAgentResult")
    void interact_returnsStructuredAgentResult() {
      Agent.Structured<PersonInfo> structured = createStructuredAgent(PersonInfo.class);

      enqueueStructuredResponse("{\"name\": \"John Doe\", \"age\": 30}");

      StructuredAgentResult<PersonInfo> result = structured.interact("Tell me about John").join();

      assertNotNull(result);
      assertFalse(result.isError());
    }

    @Test
    @DisplayName("interact parses JSON to typed object")
    void interact_parsesToTypedObject() {
      Agent.Structured<PersonInfo> structured = createStructuredAgent(PersonInfo.class);

      enqueueStructuredResponse("{\"name\": \"John Doe\", \"age\": 30}");

      StructuredAgentResult<PersonInfo> result = structured.interact("Tell me about John").join();

      assertNotNull(result.output());
      assertEquals("John Doe", result.output().name());
      assertEquals(30, result.output().age());
    }

    @Test
    @DisplayName("interact with context returns parsed object")
    void interactWithContext_returnsParsedObject() {
      Agent.Structured<PersonInfo> structured = createStructuredAgent(PersonInfo.class);
      AgentContext context = AgentContext.create();

      enqueueStructuredResponse("{\"name\": \"Jane\", \"age\": 25}");

      context.addInput(com.paragon.responses.spec.Message.user("Tell me about Jane"));
      StructuredAgentResult<PersonInfo> result = structured.interact(context).join();

      assertNotNull(result);
      assertFalse(result.isError());
      assertNotNull(result.output());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // RESULT ACCESSORS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Result Accessors")
  class ResultAccessors {

    @Test
    @DisplayName("result provides raw JSON output")
    void result_providesRawOutput() {
      Agent.Structured<PersonInfo> structured = createStructuredAgent(PersonInfo.class);

      enqueueStructuredResponse("{\"name\": \"Alice\", \"age\": 28}");

      StructuredAgentResult<PersonInfo> result = structured.interact("Who is Alice?").join();

      assertNotNull(result.rawOutput());
      assertTrue(result.rawOutput().contains("Alice"));
    }

    @Test
    @DisplayName("result provides turns used")
    void result_providesTurnsUsed() {
      Agent.Structured<PersonInfo> structured = createStructuredAgent(PersonInfo.class);

      enqueueStructuredResponse("{\"name\": \"Bob\", \"age\": 35}");

      StructuredAgentResult<PersonInfo> result = structured.interact("Who is Bob?").join();

      assertEquals(1, result.turnsUsed());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPERS
  // ═══════════════════════════════════════════════════════════════════════════

  private <T> Agent.Structured<T> createStructuredAgent(Class<T> outputType) {
    return Agent.builder()
        .structured(outputType)
        .name("TestExtractor")
        .instructions("Extract information as JSON.")
        .model("test-model")
        .responder(responder)
        .build();
  }

  private void enqueueStructuredResponse(String jsonContent) {
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
            "output_tokens": 50,
            "total_tokens": 60
          }
        }
        """.formatted(System.nanoTime(), jsonContent.replace("\"", "\\\"").replace("\n", ""));

    mockWebServer.enqueue(new MockResponse()
        .setBody(responseJson)
        .setHeader("Content-Type", "application/json")
        .setResponseCode(200));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TYPE DEFINITIONS
  // ═══════════════════════════════════════════════════════════════════════════

  public record PersonInfo(@NonNull String name, int age) {}
}
