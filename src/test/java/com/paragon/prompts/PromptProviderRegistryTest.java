package com.paragon.prompts;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.*;

/** Tests for {@link PromptProviderRegistry}. */
class PromptProviderRegistryTest {

  @BeforeEach
  void setUp() {
    PromptProviderRegistry.clear();
  }

  @AfterEach
  void tearDown() {
    PromptProviderRegistry.clear();
  }

  @Test
  void registerAndGet() {
    PromptProvider mock = createMockProvider("Hello");
    PromptProviderRegistry.register("test", mock);

    assertNotNull(PromptProviderRegistry.get("test"));
    assertSame(mock, PromptProviderRegistry.get("test"));
  }

  @Test
  void getUnregisteredReturnsNull() {
    assertNull(PromptProviderRegistry.get("nonexistent"));
  }

  @Test
  void containsRegistered() {
    PromptProviderRegistry.register("langfuse", createMockProvider("x"));
    assertTrue(PromptProviderRegistry.contains("langfuse"));
    assertFalse(PromptProviderRegistry.contains("other"));
  }

  @Test
  void registeredIds() {
    PromptProviderRegistry.register("a", createMockProvider("x"));
    PromptProviderRegistry.register("b", createMockProvider("y"));

    var ids = PromptProviderRegistry.registeredIds();
    assertEquals(2, ids.size());
    assertTrue(ids.contains("a"));
    assertTrue(ids.contains("b"));
  }

  @Test
  void registeredIdsAreUnmodifiable() {
    PromptProviderRegistry.register("a", createMockProvider("x"));
    var ids = PromptProviderRegistry.registeredIds();
    assertThrows(UnsupportedOperationException.class, () -> ids.add("b"));
  }

  @Test
  void clearRemovesAll() {
    PromptProviderRegistry.register("a", createMockProvider("x"));
    PromptProviderRegistry.register("b", createMockProvider("y"));
    assertEquals(2, PromptProviderRegistry.registeredIds().size());

    PromptProviderRegistry.clear();
    assertEquals(0, PromptProviderRegistry.registeredIds().size());
    assertNull(PromptProviderRegistry.get("a"));
  }

  @Test
  void unregisterSpecific() {
    PromptProviderRegistry.register("a", createMockProvider("x"));
    PromptProviderRegistry.register("b", createMockProvider("y"));

    PromptProviderRegistry.unregister("a");
    assertFalse(PromptProviderRegistry.contains("a"));
    assertTrue(PromptProviderRegistry.contains("b"));
  }

  @Test
  void registerOverwritesPrevious() {
    PromptProvider first = createMockProvider("first");
    PromptProvider second = createMockProvider("second");

    PromptProviderRegistry.register("key", first);
    PromptProviderRegistry.register("key", second);

    assertSame(second, PromptProviderRegistry.get("key"));
    assertEquals(1, PromptProviderRegistry.registeredIds().size());
  }

  @Test
  void registerNullIdThrows() {
    assertThrows(NullPointerException.class,
        () -> PromptProviderRegistry.register(null, createMockProvider("x")));
  }

  @Test
  void registerNullProviderThrows() {
    assertThrows(NullPointerException.class,
        () -> PromptProviderRegistry.register("test", null));
  }

  @Test
  void registerEmptyIdThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> PromptProviderRegistry.register("", createMockProvider("x")));
  }

  @Test
  void getNullIdThrows() {
    assertThrows(NullPointerException.class,
        () -> PromptProviderRegistry.get(null));
  }

  private PromptProvider createMockProvider(String content) {
    return new PromptProvider() {
      @Override
      public Prompt providePrompt(String promptId, Map<String, String> filters) {
        return Prompt.of(content);
      }

      @Override
      public boolean exists(String promptId) {
        return true;
      }

      @Override
      public Set<String> listPromptIds() {
        return Set.of("prompt");
      }
    };
  }
}
