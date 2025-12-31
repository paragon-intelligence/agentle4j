package com.paragon.agents.context;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.Responder;
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
    responder = Responder.builder()
        .baseUrl(mockWebServer.url("/v1/responses"))
        .apiKey("test-key")
        .build();
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
      SummarizationStrategy strategy = SummarizationStrategy.withResponder(
          responder, "gpt-4o-mini");

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
      SummarizationStrategy strategy = SummarizationStrategy.builder()
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
      assertThrows(NullPointerException.class, () ->
          SummarizationStrategy.builder()
              .model("gpt-4o-mini")
              .build()
      );
    }

    @Test
    @DisplayName("model is required")
    void modelIsRequired() {
      assertThrows(NullPointerException.class, () ->
          SummarizationStrategy.builder()
              .responder(responder)
              .build()
      );
    }

    @Test
    @DisplayName("keepRecentMessages defaults to 5")
    void keepRecentMessagesDefaultsTo5() {
      SummarizationStrategy strategy = SummarizationStrategy.builder()
          .responder(responder)
          .model("gpt-4o-mini")
          .build();

      assertEquals(5, strategy.keepRecentMessages());
    }

    @Test
    @DisplayName("keepRecentMessages can be customized")
    void keepRecentMessagesCanBeCustomized() {
      SummarizationStrategy strategy = SummarizationStrategy.builder()
          .responder(responder)
          .model("gpt-4o-mini")
          .keepRecentMessages(3)
          .build();

      assertEquals(3, strategy.keepRecentMessages());
    }

    @Test
    @DisplayName("summarizationPrompt can be customized")
    void summarizationPromptCanBeCustomized() {
      SummarizationStrategy strategy = SummarizationStrategy.builder()
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
      SummarizationStrategy strategy = SummarizationStrategy.withResponder(
          responder, "gpt-3.5-turbo");

      assertEquals("gpt-3.5-turbo", strategy.model());
    }

    @Test
    @DisplayName("keepRecentMessages returns configured count")
    void keepRecentMessagesReturnsValue() {
      SummarizationStrategy strategy = SummarizationStrategy.builder()
          .responder(responder)
          .model("gpt-4o-mini")
          .keepRecentMessages(7)
          .build();

      assertEquals(7, strategy.keepRecentMessages());
    }
  }
}
