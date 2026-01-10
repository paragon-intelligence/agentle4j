package com.paragon.prompts;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Coverage tests for prompt storage interfaces and implementations.
 */
@DisplayName("Prompt Store Coverage Tests")
class PromptStoreCoverageTest {

  // =========================================================================
  // PromptStore Interface Tests
  // =========================================================================
  @Nested
  @DisplayName("PromptStore Interface")
  class PromptStoreTest {

    private InMemoryPromptStore store;

    @BeforeEach
    void setUp() {
      store = new InMemoryPromptStore();
    }

    @Test
    @DisplayName("should save prompt with Prompt object")
    void shouldSavePromptWithPromptObject() {
      Prompt prompt = Prompt.of("Hello, {{name}}!");
      store.save("greeting", prompt);

      assertTrue(store.exists("greeting"));
    }

    @Test
    @DisplayName("should save prompt with string content")
    void shouldSavePromptWithStringContent() {
      store.save("welcome", "Welcome to our service!");

      assertTrue(store.exists("welcome"));
    }

    @Test
    @DisplayName("should overwrite existing prompt")
    void shouldOverwriteExistingPrompt() {
      store.save("greeting", "Old greeting");
      store.save("greeting", "New greeting");

      Optional<Prompt> retrieved = store.retrieve("greeting");
      assertTrue(retrieved.isPresent());
      assertEquals("New greeting", retrieved.get().content());
    }

    @Test
    @DisplayName("should delete prompt")
    void shouldDeletePrompt() {
      store.save("temp", "Temporary prompt");
      assertTrue(store.exists("temp"));

      store.delete("temp");
      assertFalse(store.exists("temp"));
    }

    @Test
    @DisplayName("should handle delete of non-existent prompt")
    void shouldHandleDeleteOfNonExistentPrompt() {
      // Should not throw
      assertDoesNotThrow(() -> store.delete("non-existent"));
    }
  }

  // =========================================================================
  // Prompt Record Tests
  // =========================================================================
  @Nested
  @DisplayName("Prompt Record")
  class PromptRecordTest {

    @Test
    @DisplayName("should create prompt with factory method")
    void shouldCreatePromptWithFactoryMethod() {
      Prompt prompt = Prompt.of("Hello, World!");
      assertEquals("Hello, World!", prompt.content());
    }

    @Test
    @DisplayName("should handle template variables")
    void shouldHandleTemplateVariables() {
      Prompt prompt = Prompt.of("Hello, {{name}}! You are {{age}} years old.");
      assertTrue(prompt.content().contains("{{name}}"));
      assertTrue(prompt.content().contains("{{age}}"));
    }

    @Test
    @DisplayName("should handle empty content")
    void shouldHandleEmptyContent() {
      Prompt prompt = Prompt.of("");
      assertEquals("", prompt.content());
    }

    @Test
    @DisplayName("should handle multiline content")
    void shouldHandleMultilineContent() {
      String multiline = """
          Line 1
          Line 2
          Line 3
          """;
      Prompt prompt = Prompt.of(multiline);
      assertTrue(prompt.content().contains("Line 1"));
      assertTrue(prompt.content().contains("Line 3"));
    }

    @Test
    @DisplayName("should have proper equals and hashCode")
    void shouldHaveProperEqualsAndHashCode() {
      Prompt p1 = Prompt.of("Same content");
      Prompt p2 = Prompt.of("Same content");
      Prompt p3 = Prompt.of("Different content");

      assertEquals(p1, p2);
      assertEquals(p1.hashCode(), p2.hashCode());
      assertNotEquals(p1, p3);
    }
  }

  // =========================================================================
  // PromptProvider Interface Tests
  // =========================================================================
  @Nested
  @DisplayName("PromptProvider Interface")
  class PromptProviderTest {

    private InMemoryPromptStore provider;

    @BeforeEach
    void setUp() {
      provider = new InMemoryPromptStore();
      provider.save("existing", "An existing prompt");
    }

    @Test
    @DisplayName("should retrieve existing prompt")
    void shouldRetrieveExistingPrompt() {
      Optional<Prompt> result = provider.retrieve("existing");
      assertTrue(result.isPresent());
      assertEquals("An existing prompt", result.get().content());
    }

    @Test
    @DisplayName("should return empty for non-existent prompt")
    void shouldReturnEmptyForNonExistentPrompt() {
      Optional<Prompt> result = provider.retrieve("non-existent");
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should check if prompt exists")
    void shouldCheckIfPromptExists() {
      assertTrue(provider.exists("existing"));
      assertFalse(provider.exists("non-existent"));
    }

    @Test
    @DisplayName("should list all prompt IDs")
    void shouldListAllPromptIds() {
      provider.save("prompt1", "Content 1");
      provider.save("prompt2", "Content 2");

      Set<String> ids = provider.listPromptIds();

      assertTrue(ids.contains("existing"));
      assertTrue(ids.contains("prompt1"));
      assertTrue(ids.contains("prompt2"));
    }
  }

  // =========================================================================
  // Edge Cases
  // =========================================================================
  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTest {

    @Test
    @DisplayName("should handle unicode in prompts")
    void shouldHandleUnicodeInPrompts() {
      InMemoryPromptStore store = new InMemoryPromptStore();
      store.save("unicode", "Hello 世界 🌍 مرحبا");

      Optional<Prompt> retrieved = store.retrieve("unicode");
      assertTrue(retrieved.isPresent());
      assertEquals("Hello 世界 🌍 مرحبا", retrieved.get().content());
    }

    @Test
    @DisplayName("should handle special characters in prompt ID")
    void shouldHandleSpecialCharactersInPromptId() {
      InMemoryPromptStore store = new InMemoryPromptStore();
      store.save("prompt/with/slashes", "Content");

      assertTrue(store.exists("prompt/with/slashes"));
    }

    @Test
    @DisplayName("should handle very long prompt content")
    void shouldHandleVeryLongPromptContent() {
      InMemoryPromptStore store = new InMemoryPromptStore();
      String longContent = "x".repeat(100000);
      store.save("long", longContent);

      Optional<Prompt> retrieved = store.retrieve("long");
      assertTrue(retrieved.isPresent());
      assertEquals(100000, retrieved.get().content().length());
    }
  }

  // =========================================================================
  // In-Memory Implementation for Testing
  // =========================================================================

  /**
   * Simple in-memory implementation of PromptStore and PromptProvider for testing.
   */
  private static class InMemoryPromptStore implements PromptStore, PromptProvider {
    private final ConcurrentHashMap<String, Prompt> prompts = new ConcurrentHashMap<>();

    @Override
    public void save(String promptId, Prompt prompt) {
      prompts.put(promptId, prompt);
    }

    @Override
    public void delete(String promptId) {
      prompts.remove(promptId);
    }

    @Override
    public Prompt providePrompt(String promptId, java.util.Map<String, String> filters) {
      Prompt prompt = prompts.get(promptId);
      if (prompt == null) {
        throw new PromptProviderException(promptId, "Prompt not found: " + promptId);
      }
      return prompt;
    }

    @Override
    public boolean exists(String promptId) {
      return prompts.containsKey(promptId);
    }

    @Override
    public Set<String> listPromptIds() {
      return prompts.keySet();
    }

    // Convenience method for tests
    public java.util.Optional<Prompt> retrieve(String promptId) {
      return java.util.Optional.ofNullable(prompts.get(promptId));
    }
  }
}
