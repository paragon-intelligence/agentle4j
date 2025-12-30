package com.paragon.responses.tools.memory;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.spec.FunctionToolCallOutput;
import com.paragon.responses.spec.FunctionToolCallOutputStatus;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for MemoryTool and related classes. */
@DisplayName("Memory Tool Tests")
class MemoryToolTest {

  private InMemoryStore memoryStore;
  private MemoryTool memoryTool;

  @BeforeEach
  void setUp() {
    memoryStore = new InMemoryStore();
    memoryTool = new MemoryTool(memoryStore);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // STORE ACTION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Store Action")
  class StoreAction {

    @Test
    @DisplayName("stores value successfully")
    void storesValue() {
      MemoryToolParams params = new MemoryToolParams(MemoryAction.STORE, "user_name", "John");

      FunctionToolCallOutput output = memoryTool.call(params);

      assertEquals(FunctionToolCallOutputStatus.COMPLETED, output.status());
      assertEquals("John", memoryStore.retrieve("user_name"));
    }

    @Test
    @DisplayName("overwrites existing value")
    void overwritesValue() {
      memoryStore.store("key1", "original");

      MemoryToolParams params = new MemoryToolParams(MemoryAction.STORE, "key1", "updated");
      memoryTool.call(params);

      assertEquals("updated", memoryStore.retrieve("key1"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // RETRIEVE ACTION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Retrieve Action")
  class RetrieveAction {

    @Test
    @DisplayName("retrieves stored value")
    void retrievesValue() {
      memoryStore.store("city", "New York");

      MemoryToolParams params = new MemoryToolParams(MemoryAction.RETRIEVE, "city", null);

      FunctionToolCallOutput output = memoryTool.call(params);

      assertTrue(output.toString().contains("New York"));
    }

    @Test
    @DisplayName("returns not found for missing key")
    void returnsNotFound() {
      MemoryToolParams params = new MemoryToolParams(MemoryAction.RETRIEVE, "nonexistent", null);

      FunctionToolCallOutput output = memoryTool.call(params);

      assertTrue(output.toString().contains("No value found"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // DELETE ACTION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Delete Action")
  class DeleteAction {

    @Test
    @DisplayName("deletes stored value")
    void deletesValue() {
      memoryStore.store("temp", "data");

      MemoryToolParams params = new MemoryToolParams(MemoryAction.DELETE, "temp", null);

      memoryTool.call(params);

      assertNull(memoryStore.retrieve("temp"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ERROR HANDLING
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Error Handling")
  class ErrorHandling {

    @Test
    @DisplayName("returns error for null params")
    void errorOnNullParams() {
      FunctionToolCallOutput output = memoryTool.call(null);

      assertEquals(FunctionToolCallOutputStatus.INCOMPLETE, output.status());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MEMORY TOOL PARAMS VALIDATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("MemoryToolParams Validation")
  class ParamsValidation {

    @Test
    @DisplayName("throws on blank key")
    void throwsOnBlankKey() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new MemoryToolParams(MemoryAction.RETRIEVE, "  ", null));
    }

    @Test
    @DisplayName("throws on null value for STORE")
    void throwsOnNullValueForStore() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new MemoryToolParams(MemoryAction.STORE, "key", null));
    }

    @Test
    @DisplayName("allows null value for RETRIEVE")
    void allowsNullValueForRetrieve() {
      MemoryToolParams params = new MemoryToolParams(MemoryAction.RETRIEVE, "key", null);

      assertNotNull(params);
    }

    @Test
    @DisplayName("allows null value for DELETE")
    void allowsNullValueForDelete() {
      MemoryToolParams params = new MemoryToolParams(MemoryAction.DELETE, "key", null);

      assertNotNull(params);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MEMORY ACTION ENUM
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("MemoryAction Enum")
  class ActionEnum {

    @Test
    @DisplayName("has all expected values")
    void hasAllValues() {
      assertEquals(3, MemoryAction.values().length);
      assertNotNull(MemoryAction.valueOf("STORE"));
      assertNotNull(MemoryAction.valueOf("RETRIEVE"));
      assertNotNull(MemoryAction.valueOf("DELETE"));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TEST IMPLEMENTATION
  // ═══════════════════════════════════════════════════════════════════════════

  /** Simple in-memory implementation for testing. */
  static class InMemoryStore implements MemoryStore {
    private final ConcurrentHashMap<String, String> data = new ConcurrentHashMap<>();

    @Override
    public String store(String key, String value) {
      data.put(key, value);
      return "Stored: " + key + " = " + value;
    }

    @Override
    public String retrieve(String key) {
      return data.get(key);
    }

    @Override
    public String delete(String key) {
      data.remove(key);
      return "Deleted: " + key;
    }
  }
}
