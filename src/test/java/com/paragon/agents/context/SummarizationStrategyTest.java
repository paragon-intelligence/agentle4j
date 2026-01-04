package com.paragon.agents.context;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.*;
import java.util.ArrayList;
import java.util.List;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for SummarizationStrategy builder and accessors. */
@DisplayName("SummarizationStrategy")
class SummarizationStrategyTest {

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
  // FACTORY METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Factory Methods")
  class FactoryMethods {

    @Test
    @DisplayName("withResponder creates strategy with defaults")
    void withResponderCreatesStrategy() {
      SummarizationStrategy strategy =
          SummarizationStrategy.withResponder(responder, "gpt-4o-mini");

      assertNotNull(strategy);
      assertEquals("gpt-4o-mini", strategy.model());
      assertEquals(5, strategy.keepRecentMessages()); // default
    }

    @Test
    @DisplayName("builder returns new builder")
    void builderReturnsNewBuilder() {
      SummarizationStrategy.Builder builder = SummarizationStrategy.builder();
      assertNotNull(builder);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BUILDER
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Builder")
  class BuilderTests {

    @Test
    @DisplayName("build creates strategy with all fields")
    void buildCreatesStrategy() {
      SummarizationStrategy strategy =
          SummarizationStrategy.builder()
              .responder(responder)
              .model("gpt-4o-mini")
              .keepRecentMessages(10)
              .summarizationPrompt("Summarize: %s")
              .build();

      assertNotNull(strategy);
      assertEquals("gpt-4o-mini", strategy.model());
      assertEquals(10, strategy.keepRecentMessages());
    }

    @Test
    @DisplayName("responder is required")
    void responderIsRequired() {
      assertThrows(
          NullPointerException.class,
          () -> SummarizationStrategy.builder().model("gpt-4o-mini").build());
    }

    @Test
    @DisplayName("model is required")
    void modelIsRequired() {
      assertThrows(
          NullPointerException.class,
          () -> SummarizationStrategy.builder().responder(responder).build());
    }

    @Test
    @DisplayName("keepRecentMessages defaults to 5")
    void keepRecentMessagesDefaultsTo5() {
      SummarizationStrategy strategy =
          SummarizationStrategy.builder().responder(responder).model("gpt-4o-mini").build();

      assertEquals(5, strategy.keepRecentMessages());
    }

    @Test
    @DisplayName("keepRecentMessages can be customized")
    void keepRecentMessagesCanBeCustomized() {
      SummarizationStrategy strategy =
          SummarizationStrategy.builder()
              .responder(responder)
              .model("gpt-4o-mini")
              .keepRecentMessages(3)
              .build();

      assertEquals(3, strategy.keepRecentMessages());
    }

    @Test
    @DisplayName("summarizationPrompt can be customized")
    void summarizationPromptCanBeCustomized() {
      SummarizationStrategy strategy =
          SummarizationStrategy.builder()
              .responder(responder)
              .model("gpt-4o-mini")
              .summarizationPrompt("Custom prompt: %s")
              .build();

      assertNotNull(strategy);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ACCESSORS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Accessors")
  class AccessorTests {

    @Test
    @DisplayName("model returns configured model")
    void modelReturnsValue() {
      SummarizationStrategy strategy =
          SummarizationStrategy.withResponder(responder, "gpt-3.5-turbo");

      assertEquals("gpt-3.5-turbo", strategy.model());
    }

    @Test
    @DisplayName("keepRecentMessages returns configured count")
    void keepRecentMessagesReturnsValue() {
      SummarizationStrategy strategy =
          SummarizationStrategy.builder()
              .responder(responder)
              .model("gpt-4o-mini")
              .keepRecentMessages(7)
              .build();

      assertEquals(7, strategy.keepRecentMessages());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MANAGE - FAST PATHS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Manage - Fast Paths")
  class ManageFastPathsTests {

    private SummarizationStrategy strategy;
    private TokenCounter counter;

    @BeforeEach
    void setUpManage() {
      strategy = SummarizationStrategy.builder().responder(responder).model("gpt-4o-mini").build();
      counter = new SimpleTokenCounter();
    }

    @Test
    @DisplayName("returns empty history unchanged")
    void returnsEmptyHistoryUnchanged() {
      List<ResponseInputItem> history = new ArrayList<>();
      List<ResponseInputItem> result = strategy.manage(history, 1000, counter);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("returns history unchanged when maxTokens is zero")
    void returnsHistoryUnchangedWhenMaxTokensZero() {
      List<ResponseInputItem> history = List.of(Message.user("Hello"));
      List<ResponseInputItem> result = strategy.manage(history, 0, counter);
      assertEquals(1, result.size());
    }

    @Test
    @DisplayName("returns history unchanged when maxTokens is negative")
    void returnsHistoryUnchangedWhenMaxTokensNegative() {
      List<ResponseInputItem> history = List.of(Message.user("Hello"));
      List<ResponseInputItem> result = strategy.manage(history, -10, counter);
      assertEquals(1, result.size());
    }

    @Test
    @DisplayName("returns history unchanged when within limits")
    void returnsHistoryUnchangedWhenWithinLimits() {
      List<ResponseInputItem> history = List.of(Message.user("Hi"));
      List<ResponseInputItem> result = strategy.manage(history, 10000, counter);
      assertEquals(1, result.size());
    }

    @Test
    @DisplayName("manage throws on null history")
    void throwsOnNullHistory() {
      assertThrows(NullPointerException.class, () -> strategy.manage(null, 1000, counter));
    }

    @Test
    @DisplayName("manage throws on null counter")
    void throwsOnNullCounter() {
      List<ResponseInputItem> history = new ArrayList<>();
      assertThrows(NullPointerException.class, () -> strategy.manage(history, 1000, null));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MANAGE - SUMMARIZATION FLOW
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Manage - Summarization Flow")
  class ManageSummarizationFlowTests {

    @Test
    @DisplayName("returns recent messages when history matches keepRecentMessages")
    void returnsRecentMessagesWhenHistoryMatchesKeepRecent() {
      // Use a very small keepRecentMessages with a limited token budget
      SummarizationStrategy strategy =
          SummarizationStrategy.builder()
              .responder(responder)
              .model("gpt-4o-mini")
              .keepRecentMessages(2)
              .build();

      // This is really a test of the keep-recent logic
      List<ResponseInputItem> history = new ArrayList<>();
      history.add(Message.user("Message 1"));
      history.add(Message.user("Message 2"));

      TokenCounter counter = new SimpleTokenCounter();
      List<ResponseInputItem> result = strategy.manage(history, 10000, counter);

      // When within limits, returns unchanged
      assertEquals(2, result.size());
    }

    @Test
    @DisplayName("summarization with API failure falls back gracefully")
    void summarizationWithApiFailureFallsBackGracefully() {
      // Set up a strategy with small keepRecentMessages
      SummarizationStrategy strategy =
          SummarizationStrategy.builder()
              .responder(responder)
              .model("gpt-4o-mini")
              .keepRecentMessages(1)
              .build();

      // Create large history that will trigger summarization attempt
      List<ResponseInputItem> history = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        history.add(
            Message.user("Very long message number " + i + " with lots of words to count."));
      }

      // The API will fail since we don't enqueue a response,
      // but the strategy should handle it gracefully
      TokenCounter counter = new LimitedTokenCounter(50);
      List<ResponseInputItem> result = strategy.manage(history, 50, counter);

      // Should return something valid (either fallback summary or sliding window)
      assertNotNull(result);
      assertFalse(result.isEmpty());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MANAGE - HANDLE DIFFERENT MESSAGE TYPES
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Manage - Message Type Handling")
  class ManageMessageTypeHandlingTests {

    @Test
    @DisplayName("handles mixed message types")
    void handlesMixedMessageTypes() {
      SummarizationStrategy strategy =
          SummarizationStrategy.builder()
              .responder(responder)
              .model("gpt-4o-mini")
              .keepRecentMessages(5)
              .build();

      List<ResponseInputItem> history = new ArrayList<>();
      history.add(Message.user("Question about weather"));
      history.add(Message.assistant("I'll check the weather for you."));
      history.add(FunctionToolCallOutput.success("call-1", "{\"temp\": 72}"));
      history.add(Message.user("Thanks!"));

      TokenCounter counter = new SimpleTokenCounter();
      List<ResponseInputItem> result = strategy.manage(history, 10000, counter);

      assertEquals(4, result.size());
    }

    @Test
    @DisplayName("handles developer messages")
    void handlesDeveloperMessages() {
      SummarizationStrategy strategy =
          SummarizationStrategy.builder()
              .responder(responder)
              .model("gpt-4o-mini")
              .keepRecentMessages(5)
              .build();

      List<ResponseInputItem> history = new ArrayList<>();
      history.add(Message.developer("You are a helpful assistant."));
      history.add(Message.user("Hello"));
      history.add(Message.assistant("Hi there!"));

      TokenCounter counter = new SimpleTokenCounter();
      List<ResponseInputItem> result = strategy.manage(history, 10000, counter);

      assertEquals(3, result.size());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPER CLASSES
  // ═══════════════════════════════════════════════════════════════════════════

  /** Simple token counter that estimates tokens as characters / 4 */
  private static class SimpleTokenCounter implements TokenCounter {
    @Override
    public int countTokens(ResponseInputItem item) {
      if (item instanceof Message msg) {
        return msg.content().stream()
            .mapToInt(c -> c instanceof Text t ? t.text().length() / 4 : 10)
            .sum();
      }
      return 10;
    }

    @Override
    public int countText(String text) {
      return text.length() / 4;
    }

    @Override
    public int countImage(Image image) {
      return 100; // Estimate for images
    }
  }

  /** Token counter that returns a limited count to force summarization */
  private static class LimitedTokenCounter implements TokenCounter {
    private final int limit;
    private boolean first = true;

    LimitedTokenCounter(int limit) {
      this.limit = limit;
    }

    @Override
    public int countTokens(ResponseInputItem item) {
      return 20;
    }

    @Override
    public int countTokens(List<ResponseInputItem> items) {
      if (first) {
        first = false;
        return limit * 2; // Over limit
      }
      return limit / 2; // Under limit
    }

    @Override
    public int countText(String text) {
      return 10;
    }

    @Override
    public int countImage(Image image) {
      return 100;
    }
  }
}
