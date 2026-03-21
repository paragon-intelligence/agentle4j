package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.Responder;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.spec.Reasoning;
import com.paragon.responses.spec.ResponseOutput;
import java.io.IOException;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

/**
 * Tests for Agent.Structured<T> functionality.
 *
 * <p>Tests cover: - Structured agent creation via Agent.builder().structured(Class) - JSON parsing
 * to typed objects - StructuredAgentResult handling
 */
@DisplayName("Agent Structured Output Tests")
class AgentStructuredOutputTest {

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
  // STRUCTURED AGENT CREATION
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

  // ═══════════════════════════════════════════════════════════════════════════
  // STRUCTURED INTERACTION
  // ═══════════════════════════════════════════════════════════════════════════

  private void enqueueStructuredResponse(String jsonContent) {
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
            "output_tokens": 50,
            "total_tokens": 60
          }
        }
        """
            .formatted(System.nanoTime(), jsonContent.replace("\"", "\\\"").replace("\n", ""));

    mockWebServer.enqueue(
        new MockResponse()
            .setBody(responseJson)
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // RESULT ACCESSORS
  // ═══════════════════════════════════════════════════════════════════════════

  public record PersonInfo(@NonNull String name, int age) {}

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPERS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Structured Agent Creation")
  class StructuredAgentCreation {

    @Test
    @DisplayName("structured agent is created from builder")
    void structuredAgent_createdFromBuilder() {
      Agent.Structured<PersonInfo> structured =
          Agent.builder()
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
      Agent.Structured<PersonInfo> structured =
          Agent.builder()
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
      Agent.Structured<PersonInfo> structured =
          Agent.builder()
              .structured(PersonInfo.class)
              .name("PersonExtractor")
              .instructions("Extract person info.")
              .model("test-model")
              .responder(responder)
              .build();

      assertEquals("PersonExtractor", structured.name());
    }
  }

  @Nested
  @DisplayName("Structured Interaction")
  class StructuredInteraction {

    @Test
    @DisplayName("interact returns StructuredAgentResult")
    void interact_returnsStructuredAgentResult() {
      Agent.Structured<PersonInfo> structured = createStructuredAgent(PersonInfo.class);

      enqueueStructuredResponse("{\"name\": \"John Doe\", \"age\": 30}");

      StructuredAgentResult<PersonInfo> result = structured.interact("Tell me about John");

      assertNotNull(result);
      assertFalse(result.isError());
    }

    @Test
    @DisplayName("interact parses JSON to typed object")
    void interact_parsesToTypedObject() {
      Agent.Structured<PersonInfo> structured = createStructuredAgent(PersonInfo.class);

      enqueueStructuredResponse("{\"name\": \"John Doe\", \"age\": 30}");

      StructuredAgentResult<PersonInfo> result = structured.interact("Tell me about John");

      assertNotNull(result.parsed());
      assertEquals("John Doe", result.parsed().name());
      assertEquals(30, result.parsed().age());
    }

    @Test
    @DisplayName("interact with context returns parsed object")
    void interactWithContext_returnsParsedObject() {
      Agent.Structured<PersonInfo> structured = createStructuredAgent(PersonInfo.class);
      AgenticContext context = AgenticContext.create();

      enqueueStructuredResponse("{\"name\": \"Jane\", \"age\": 25}");

      context.addInput(com.paragon.responses.spec.Message.user("Tell me about Jane"));
      StructuredAgentResult<PersonInfo> result = structured.interact(context);

      assertNotNull(result);
      assertFalse(result.isError());
      assertNotNull(result.parsed());
    }

    @Test
    @DisplayName("interact unwraps root polymorphic structured output")
    void interact_unwrapsRootPolymorphicStructuredOutput() {
      Agent.Structured<ResponseOutput> structured =
          Agent.builder()
              .structured(ResponseOutput.class)
              .name("PolymorphicExtractor")
              .instructions("Return a structured response item.")
              .model("test-model")
              .responder(responder)
              .objectMapper(ResponsesApiObjectMapper.create())
              .build();

      enqueueStructuredResponse(
          """
          {"value":{"type":"reasoning","id":"reasoning_123","summary":[],"status":"completed"}}
          """);

      StructuredAgentResult<ResponseOutput> result = structured.interact("Return a reasoning item");

      assertInstanceOf(Reasoning.class, result.parsed());
      assertTrue(result.output().contains("\"value\""));
    }

    @Test
    @DisplayName("interact parses TypeReference list root")
    void interact_parsesTypeReferenceListRoot() {
      Agent.Structured<List<PersonInfo>> structured =
          Agent.builder()
              .structured(new TypeReference<List<PersonInfo>>() {})
              .name("ListExtractor")
              .instructions("Return a list of people.")
              .model("test-model")
              .responder(responder)
              .build();

      enqueueStructuredResponse(
          """
          {"value":[{"name":"John Doe","age":30},{"name":"Jane Doe","age":25}]}
          """);

      StructuredAgentResult<List<PersonInfo>> result = structured.interact("Return two people");

      assertEquals(List.class, structured.outputType());
      assertEquals(2, result.parsed().size());
      assertEquals("John Doe", result.parsed().getFirst().name());
      assertTrue(result.output().contains("\"value\""));
    }

    @Test
    @DisplayName("interact parses JavaType list root")
    void interact_parsesJavaTypeListRoot() {
      JavaType listType =
          new ObjectMapper().constructType(new TypeReference<List<PersonInfo>>() {}.getType());

      Agent.Structured<List<PersonInfo>> structured =
          Agent.builder()
              .<List<PersonInfo>>structured(listType)
              .name("ListExtractor")
              .instructions("Return a list of people.")
              .model("test-model")
              .responder(responder)
              .build();

      enqueueStructuredResponse(
          """
          {"value":[{"name":"Alice","age":28}]}
          """);

      StructuredAgentResult<List<PersonInfo>> result = structured.interact("Return one person");

      assertEquals(1, result.parsed().size());
      assertEquals("Alice", result.parsed().getFirst().name());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TYPE DEFINITIONS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Result Accessors")
  class ResultAccessors {

    @Test
    @DisplayName("result provides raw JSON output")
    void result_providesRawOutput() {
      Agent.Structured<PersonInfo> structured = createStructuredAgent(PersonInfo.class);

      enqueueStructuredResponse("{\"name\": \"Alice\", \"age\": 28}");

      StructuredAgentResult<PersonInfo> result = structured.interact("Who is Alice?");

      assertNotNull(result.output());
      assertTrue(result.output().contains("Alice"));
    }

    @Test
    @DisplayName("result provides turns used")
    void result_providesTurnsUsed() {
      Agent.Structured<PersonInfo> structured = createStructuredAgent(PersonInfo.class);

      enqueueStructuredResponse("{\"name\": \"Bob\", \"age\": 35}");

      StructuredAgentResult<PersonInfo> result = structured.interact("Who is Bob?");

      assertEquals(1, result.turnsUsed());
    }
  }
}
