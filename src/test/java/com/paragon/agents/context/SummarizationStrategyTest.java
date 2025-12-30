package com.paragon.agents.context;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.*;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for SummarizationStrategy. */
@DisplayName("SummarizationStrategy")
class SummarizationStrategyTest {

  private MockWebServer mockWebServer;
  private Responder responder;
  private SimpleTokenCounter counter;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    responder =
        Responder.builder().baseUrl(mockWebServer.url("/v1/responses")).apiKey("test-key").build();

    counter = new SimpleTokenCounter();
  }

  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Nested
  @DisplayName("Builder")
  class BuilderTests {

    @Test
    @DisplayName("builds with required fields")
    void buildsWithRequiredFields() {
      SummarizationStrategy strategy =
          SummarizationStrategy.builder().responder(responder).model("test-model").build();

      assertNotNull(strategy);
      assertEquals("test-model", strategy.model());
    }

    @Test
    @DisplayName("withResponder factory method works")
    void withResponderFactoryWorks() {
      SummarizationStrategy strategy = SummarizationStrategy.withResponder(responder, "test-model");

      assertNotNull(strategy);
      assertEquals("test-model", strategy.model());
    }

    @Test
    @DisplayName("throws when responder is null")
    void throwsWhenResponderNull() {
      assertThrows(
          NullPointerException.class,
          () -> SummarizationStrategy.builder().model("test-model").build());
    }

    @Test
    @DisplayName("throws when model is null")
    void throwsWhenModelNull() {
      assertThrows(
          NullPointerException.class,
          () -> SummarizationStrategy.builder().responder(responder).build());
    }

    @Test
    @DisplayName("keepRecentMessages is configurable")
    void keepRecentMessagesConfigurable() {
      SummarizationStrategy strategy =
          SummarizationStrategy.builder()
              .responder(responder)
              .model("test-model")
              .keepRecentMessages(10)
              .build();

      assertEquals(10, strategy.keepRecentMessages());
    }

    @Test
    @DisplayName("default keepRecentMessages is 5")
    void defaultKeepRecentMessages() {
      SummarizationStrategy strategy =
          SummarizationStrategy.builder().responder(responder).model("test-model").build();

      assertEquals(5, strategy.keepRecentMessages());
    }
  }

  @Nested
  @DisplayName("Manage Behavior")
  class ManageBehavior {

    @Test
    @DisplayName("returns same history when under limit")
    void returnsSameHistory_whenUnderLimit() {
      SummarizationStrategy strategy =
          SummarizationStrategy.builder().responder(responder).model("test-model").build();

      List<ResponseInputItem> history = List.of(Message.user("Hello"), Message.user("World"));

      List<ResponseInputItem> result = strategy.manage(history, 10000, counter);

      assertEquals(history.size(), result.size());
      // No request should have been made
      assertEquals(0, mockWebServer.getRequestCount());
    }

    @Test
    @DisplayName("handles empty history")
    void handlesEmptyHistory() {
      SummarizationStrategy strategy =
          SummarizationStrategy.builder().responder(responder).model("test-model").build();

      List<ResponseInputItem> result = strategy.manage(List.of(), 1000, counter);

      assertTrue(result.isEmpty());
      assertEquals(0, mockWebServer.getRequestCount());
    }

    @Test
    @DisplayName("handles zero max tokens")
    void handlesZeroMaxTokens() {
      SummarizationStrategy strategy =
          SummarizationStrategy.builder().responder(responder).model("test-model").build();

      List<ResponseInputItem> history = List.of(Message.user("Test"));

      List<ResponseInputItem> result = strategy.manage(history, 0, counter);

      assertEquals(1, result.size());
    }

    @Test
    @DisplayName("throws for null history")
    void throwsForNullHistory() {
      SummarizationStrategy strategy =
          SummarizationStrategy.builder().responder(responder).model("test-model").build();

      assertThrows(NullPointerException.class, () -> strategy.manage(null, 1000, counter));
    }

    @Test
    @DisplayName("throws for null counter")
    void throwsForNullCounter() {
      SummarizationStrategy strategy =
          SummarizationStrategy.builder().responder(responder).model("test-model").build();

      assertThrows(NullPointerException.class, () -> strategy.manage(List.of(), 1000, null));
    }
  }

  @Nested
  @DisplayName("Custom Prompt")
  class CustomPromptTests {

    @Test
    @DisplayName("custom summarization prompt can be set")
    void customPromptCanBeSet() {
      String customPrompt = "Summarize briefly: %s";

      SummarizationStrategy strategy =
          SummarizationStrategy.builder()
              .responder(responder)
              .model("test-model")
              .summarizationPrompt(customPrompt)
              .build();

      assertNotNull(strategy);
    }
  }

  // Helper methods

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
