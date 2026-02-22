package com.paragon.agents.context;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.spec.Image;
import com.paragon.responses.spec.ResponseInputItem;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Coverage tests for context management configuration and strategies. */
@DisplayName("Context Management Coverage Tests")
class ContextManagementCoverageTest {

  // =========================================================================
  // ContextManagementConfig Tests
  // =========================================================================
  @Nested
  @DisplayName("ContextManagementConfig")
  class ContextManagementConfigTest {

    @Test
    @DisplayName("should build with all required fields")
    void shouldBuildWithAllRequiredFields() {
      ContextWindowStrategy strategy = createMockStrategy();

      ContextManagementConfig config =
          ContextManagementConfig.builder().strategy(strategy).maxTokens(4000).build();

      assertEquals(strategy, config.strategy());
      assertEquals(4000, config.maxTokens());
      assertNotNull(config.tokenCounter()); // default SimpleTokenCounter
    }

    @Test
    @DisplayName("should build with custom token counter")
    void shouldBuildWithCustomTokenCounter() {
      TokenCounter customCounter = createMockTokenCounter();

      ContextManagementConfig config =
          ContextManagementConfig.builder()
              .strategy(createMockStrategy())
              .maxTokens(8000)
              .tokenCounter(customCounter)
              .build();

      assertEquals(customCounter, config.tokenCounter());
    }

    @Test
    @DisplayName("should reject null strategy")
    void shouldRejectNullStrategy() {
      assertThrows(
          NullPointerException.class,
          () -> ContextManagementConfig.builder().maxTokens(4000).build());
    }

    @Test
    @DisplayName("should reject zero maxTokens")
    void shouldRejectZeroMaxTokens() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              ContextManagementConfig.builder()
                  .strategy(createMockStrategy())
                  .maxTokens(0)
                  .build());
    }

    @Test
    @DisplayName("should reject negative maxTokens")
    void shouldRejectNegativeMaxTokens() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              ContextManagementConfig.builder()
                  .strategy(createMockStrategy())
                  .maxTokens(-100)
                  .build());
    }

    @Test
    @DisplayName("should accept minimum positive maxTokens")
    void shouldAcceptMinimumPositiveMaxTokens() {
      ContextManagementConfig config =
          ContextManagementConfig.builder().strategy(createMockStrategy()).maxTokens(1).build();

      assertEquals(1, config.maxTokens());
    }

    @Test
    @DisplayName("should accept large maxTokens")
    void shouldAcceptLargeMaxTokens() {
      ContextManagementConfig config =
          ContextManagementConfig.builder()
              .strategy(createMockStrategy())
              .maxTokens(128000)
              .build();

      assertEquals(128000, config.maxTokens());
    }
  }

  // =========================================================================
  // ContextWindowStrategy Interface Tests
  // =========================================================================
  @Nested
  @DisplayName("ContextWindowStrategy Interface")
  class ContextWindowStrategyTest {

    @Test
    @DisplayName("should manage empty history")
    void shouldManageEmptyHistory() {
      ContextWindowStrategy strategy = createMockStrategy();
      TokenCounter counter = createMockTokenCounter();
      List<ResponseInputItem> empty = new ArrayList<>();

      List<ResponseInputItem> result = strategy.manage(empty, 1000, counter);

      assertNotNull(result);
    }

    @Test
    @DisplayName("should implement manage method correctly")
    void shouldImplementManageMethodCorrectly() {
      // Pass-through strategy
      ContextWindowStrategy passThrough = (history, maxTokens, counter) -> new ArrayList<>(history);

      List<ResponseInputItem> input = new ArrayList<>();
      List<ResponseInputItem> result = passThrough.manage(input, 1000, createMockTokenCounter());

      assertNotNull(result);
    }
  }

  // =========================================================================
  // TokenCounter Tests
  // =========================================================================
  @Nested
  @DisplayName("TokenCounter Interface")
  class TokenCounterTest {

    @Test
    @DisplayName("should count text tokens")
    void shouldCountTextTokens() {
      TokenCounter counter = createMockTokenCounter();

      int count = counter.countText("Hello World Test");
      assertTrue(count > 0);
    }

    @Test
    @DisplayName("should handle empty string")
    void shouldHandleEmptyString() {
      TokenCounter counter = createMockTokenCounter();

      int count = counter.countText("");
      assertEquals(0, count);
    }

    @Test
    @DisplayName("should count tokens for list of items")
    void shouldCountTokensForListOfItems() {
      TokenCounter counter = createMockTokenCounter();
      List<ResponseInputItem> items = new ArrayList<>();

      int count = counter.countTokens(items);
      assertEquals(0, count);
    }
  }

  // =========================================================================
  // Integration Tests
  // =========================================================================
  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTest {

    @Test
    @DisplayName("should use config with strategy and counter together")
    void shouldUseConfigWithStrategyAndCounterTogether() {
      TokenCounter counter = createMockTokenCounter();

      // Strategy that returns original history if under limit
      ContextWindowStrategy strategy = (history, maxTokens, c) -> new ArrayList<>(history);

      ContextManagementConfig config =
          ContextManagementConfig.builder()
              .strategy(strategy)
              .maxTokens(4000)
              .tokenCounter(counter)
              .build();

      List<ResponseInputItem> history = new ArrayList<>();
      List<ResponseInputItem> managed =
          config.strategy().manage(history, config.maxTokens(), config.tokenCounter());

      assertNotNull(managed);
    }
  }

  // =========================================================================
  // Helper Methods
  // =========================================================================

  private ContextWindowStrategy createMockStrategy() {
    return (history, maxTokens, counter) -> new ArrayList<>(history);
  }

  private TokenCounter createMockTokenCounter() {
    return new TokenCounter() {
      @Override
      public int countTokens(ResponseInputItem item) {
        return 10; // Mock value
      }

      @Override
      public int countText(String text) {
        return text.isEmpty() ? 0 : text.length() / 4 + 1;
      }

      @Override
      public int countImage(Image image) {
        return 100; // Mock value for images
      }
    };
  }
}
