package com.paragon.agents.context;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.spec.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for TokenCounter implementations. */
@DisplayName("TokenCounter")
class TokenCounterTest {

  private SimpleTokenCounter counter;

  @BeforeEach
  void setUp() {
    counter = new SimpleTokenCounter();
  }

  @Nested
  @DisplayName("SimpleTokenCounter")
  class SimpleTokenCounterTests {

    @Test
    @DisplayName("default constructor uses 4 chars per token")
    void defaultConstructor_uses4CharsPerToken() {
      assertEquals(4, counter.charsPerToken());
    }

    @Test
    @DisplayName("custom chars per token is respected")
    void customCharsPerToken_isRespected() {
      SimpleTokenCounter custom = new SimpleTokenCounter(3);
      assertEquals(3, custom.charsPerToken());
    }

    @Test
    @DisplayName("throws for invalid chars per token")
    void throwsForInvalidCharsPerToken() {
      assertThrows(IllegalArgumentException.class, () -> new SimpleTokenCounter(0));
      assertThrows(IllegalArgumentException.class, () -> new SimpleTokenCounter(-1));
    }

    @Test
    @DisplayName("counts text tokens correctly")
    void countsTextTokens() {
      // 12 chars / 4 = 3 tokens
      assertEquals(3, counter.countText("Hello World!"));

      // 4 chars / 4 = 1 token
      assertEquals(1, counter.countText("Test"));

      // Empty text = 0 tokens
      assertEquals(0, counter.countText(""));

      // 1 char = minimum 1 token
      assertEquals(1, counter.countText("A"));
    }

    @Test
    @DisplayName("counts image tokens based on detail level")
    void countsImageTokens() {
      assertEquals(
          SimpleTokenCounter.HIGH_DETAIL_IMAGE_TOKENS,
          counter.countImage(Image.fromUrl(ImageDetail.HIGH, "http://example.com/img.jpg")));

      assertEquals(
          SimpleTokenCounter.LOW_DETAIL_IMAGE_TOKENS,
          counter.countImage(Image.fromUrl(ImageDetail.LOW, "http://example.com/img.jpg")));

      assertEquals(
          SimpleTokenCounter.AUTO_DETAIL_IMAGE_TOKENS,
          counter.countImage(Image.fromUrl("http://example.com/img.jpg")));
    }

    @Test
    @DisplayName("counts message tokens including overhead")
    void countsMessageTokens() {
      Message userMsg = Message.user("Hello");
      // "Hello" = 5 chars / 4 = 1 token + 4 overhead = 5
      int tokens = counter.countTokens(userMsg);
      assertTrue(tokens >= 5, "Should include overhead for message structure");
    }

    @Test
    @DisplayName("counts message with multiple content items")
    void countsMessageWithMultipleContent() {
      Message msg =
          Message.user(List.of(Text.valueOf("Hello"), Image.fromUrl("http://example.com/img.jpg")));

      int tokens = counter.countTokens(msg);
      // Text + Image + overhead
      assertTrue(tokens > SimpleTokenCounter.AUTO_DETAIL_IMAGE_TOKENS);
    }

    @Test
    @DisplayName("counts tool call output tokens")
    void countsToolCallOutputTokens() {
      FunctionToolCallOutput output = FunctionToolCallOutput.success("call_123", "Result text");

      int tokens = counter.countTokens(output);
      assertTrue(tokens >= 10, "Should include overhead for tool structure");
    }

    @Test
    @DisplayName("counts list of items")
    void countsListOfItems() {
      List<ResponseInputItem> items =
          List.of(Message.user("First message"), Message.user("Second message"));

      int total = counter.countTokens(items);
      int first = counter.countTokens(items.get(0));
      int second = counter.countTokens(items.get(1));

      assertEquals(first + second, total);
    }

    @Test
    @DisplayName("handles null text gracefully")
    void handlesNullTextGracefully() {
      assertThrows(NullPointerException.class, () -> counter.countText(null));
    }

    @Test
    @DisplayName("handles null image gracefully")
    void handlesNullImageGracefully() {
      assertThrows(NullPointerException.class, () -> counter.countImage(null));
    }

    @Test
    @DisplayName("handles null item gracefully")
    void handlesNullItemGracefully() {
      assertThrows(NullPointerException.class, () -> counter.countTokens((ResponseInputItem) null));
    }
  }

  @Nested
  @DisplayName("Custom Token Counter")
  class CustomTokenCounterTests {

    @Test
    @DisplayName("custom ratio changes text counting")
    void customRatioChangesTextCounting() {
      SimpleTokenCounter custom = new SimpleTokenCounter(2);

      // 8 chars / 2 = 4 tokens
      assertEquals(4, custom.countText("TestTest"));

      // Compare with default (8 / 4 = 2)
      assertEquals(2, counter.countText("TestTest"));
    }
  }
}
