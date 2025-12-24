package com.paragon;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for LlmProvider enum.
 */
@DisplayName("LlmProvider Tests")
class LlmProviderTest {

  @Nested
  @DisplayName("Enum Values")
  class EnumValues {

    @Test
    @DisplayName("has major providers")
    void hasMajorProviders() {
      assertNotNull(LlmProvider.valueOf("OPENAI"));
      assertNotNull(LlmProvider.valueOf("ANTHROPIC"));
      assertNotNull(LlmProvider.valueOf("GOOGLE"));
      assertNotNull(LlmProvider.valueOf("AZURE"));
      assertNotNull(LlmProvider.valueOf("MISTRAL"));
      assertNotNull(LlmProvider.valueOf("COHERE"));
      assertNotNull(LlmProvider.valueOf("DEEPSEEK"));
    }

    @Test
    @DisplayName("has all 68 providers")
    void hasAllProviders() {
      assertEquals(68, LlmProvider.values().length);
    }
  }

  @Nested
  @DisplayName("ToString")
  class ToStringTests {

    @Test
    @DisplayName("OpenAI toString returns display name")
    void openaiToString() {
      assertEquals("OpenAI", LlmProvider.OPENAI.toString());
    }

    @Test
    @DisplayName("Anthropic toString returns display name")
    void anthropicToString() {
      assertEquals("Anthropic", LlmProvider.ANTHROPIC.toString());
    }

    @Test
    @DisplayName("Google AI Studio toString returns display name")
    void googleAiStudioToString() {
      assertEquals("Google AI Studio", LlmProvider.GOOGLE_AI_STUDIO.toString());
    }
  }
}
