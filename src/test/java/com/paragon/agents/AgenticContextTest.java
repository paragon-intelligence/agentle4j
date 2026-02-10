package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.ResponseInputItem;
import com.paragon.responses.spec.Text;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for AgentContext.
 *
 * <p>Tests cover: - Creation and factory methods - History management - State management (key-value
 * store) - Turn counting - Copy functionality for parallel agent isolation - Clear/reset
 * functionality
 */
@DisplayName("AgentContext")
class AgentContextTest {

  @Nested
  @DisplayName("Creation")
  class Creation {

    @Test
    @DisplayName("create() returns fresh context with no history")
    void create_returnsFreshContext() {
      AgentContext context = AgentContext.create();

      assertNotNull(context);
      assertTrue(context.getHistory().isEmpty());
      assertEquals(0, context.getTurnCount());
      assertEquals(0, context.historySize());
    }

    @Test
    @DisplayName("withHistory() pre-populates conversation history")
    void withHistory_prePoplatesHistory() {
      Message message = Message.user(Text.valueOf("Hello"));
      List<ResponseInputItem> history = List.of(message);

      AgentContext context = AgentContext.withHistory(history);

      assertEquals(1, context.historySize());
      assertEquals(message, context.getHistory().getFirst());
    }

    @Test
    @DisplayName("withHistory() throws NullPointerException when null")
    void withHistory_throwsWhenNull() {
      assertThrows(NullPointerException.class, () -> AgentContext.withHistory(null));
    }
  }

  @Nested
  @DisplayName("History Management")
  class HistoryManagement {

    @Test
    @DisplayName("addMessage() adds message to history")
    void addMessage_addsToHistory() {
      AgentContext context = AgentContext.create();
      Message message = Message.user(Text.valueOf("Test"));

      context.addMessage(message);

      assertEquals(1, context.historySize());
      assertTrue(context.getHistory().contains(message));
    }

    @Test
    @DisplayName("addMessage() supports chaining")
    void addMessage_supportsChaining() {
      AgentContext context = AgentContext.create();
      Message msg1 = Message.user(Text.valueOf("First"));
      Message msg2 = Message.user(Text.valueOf("Second"));

      AgentContext result = context.addMessage(msg1).addMessage(msg2);

      assertSame(context, result);
      assertEquals(2, context.historySize());
    }

    @Test
    @DisplayName("addInput() adds ResponseInputItem to history")
    void addInput_addsToHistory() {
      AgentContext context = AgentContext.create();
      Message message = Message.user(Text.valueOf("Input"));

      context.addInput(message);

      assertEquals(1, context.historySize());
    }

    @Test
    @DisplayName("getHistory() returns unmodifiable list")
    void getHistory_returnsUnmodifiableList() {
      AgentContext context = AgentContext.create();
      context.addMessage(Message.user(Text.valueOf("Test")));

      List<ResponseInputItem> history = context.getHistory();

      assertThrows(
          UnsupportedOperationException.class,
          () -> history.add(Message.user(Text.valueOf("New"))));
    }

    @Test
    @DisplayName("getHistoryMutable() returns mutable copy")
    void getHistoryMutable_returnsMutableCopy() {
      AgentContext context = AgentContext.create();
      context.addMessage(Message.user(Text.valueOf("Test")));

      List<ResponseInputItem> mutableHistory = context.getHistoryMutable();
      mutableHistory.add(Message.user(Text.valueOf("New")));

      assertEquals(1, context.historySize()); // Original unchanged
      assertEquals(2, mutableHistory.size()); // Copy modified
    }
  }

  @Nested
  @DisplayName("State Management")
  class StateManagement {

    @Test
    @DisplayName("setState() stores value")
    void setState_storesValue() {
      AgentContext context = AgentContext.create();

      context.setState("key", "value");

      assertEquals("value", context.getState("key").orElse(null));
    }

    @Test
    @DisplayName("setState() supports chaining")
    void setState_supportsChaining() {
      AgentContext context = AgentContext.create();

      AgentContext result = context.setState("a", 1).setState("b", 2);

      assertSame(context, result);
      assertEquals(1, context.getState("a").orElse(null));
      assertEquals(2, context.getState("b").orElse(null));
    }

    @Test
    @DisplayName("setState() with null value removes key")
    void setState_nullRemovesKey() {
      AgentContext context = AgentContext.create();
      context.setState("key", "value");

      context.setState("key", null);

      assertTrue(context.getState("key").isEmpty());
      assertFalse(context.hasState("key"));
    }

    @Test
    @DisplayName("getState() with type returns properly typed value")
    void getState_withType_returnsTypedValue() {
      AgentContext context = AgentContext.create();
      context.setState("number", 42);

      Integer value = context.getState("number", Integer.class).orElse(null);

      assertEquals(42, value);
    }

    @Test
    @DisplayName("getState() returns empty for missing key")
    void getState_returnsEmptyForMissingKey() {
      AgentContext context = AgentContext.create();

      assertTrue(context.getState("nonexistent").isEmpty());
    }

    @Test
    @DisplayName("hasState() returns true for existing key")
    void hasState_returnsTrueForExistingKey() {
      AgentContext context = AgentContext.create();
      context.setState("key", "value");

      assertTrue(context.hasState("key"));
    }

    @Test
    @DisplayName("hasState() returns false for missing key")
    void hasState_returnsFalseForMissingKey() {
      AgentContext context = AgentContext.create();

      assertFalse(context.hasState("nonexistent"));
    }

    @Test
    @DisplayName("getAllState() returns unmodifiable map")
    void getAllState_returnsUnmodifiableMap() {
      AgentContext context = AgentContext.create();
      context.setState("key", "value");

      Map<String, Object> state = context.getAllState();

      assertThrows(UnsupportedOperationException.class, () -> state.put("new", "value"));
    }
  }

  @Nested
  @DisplayName("Turn Counting")
  class TurnCounting {

    @Test
    @DisplayName("getTurnCount() starts at zero")
    void getTurnCount_startsAtZero() {
      AgentContext context = AgentContext.create();

      assertEquals(0, context.getTurnCount());
    }

    @Test
    @DisplayName("incrementTurn() increments and returns new count")
    void incrementTurn_incrementsAndReturns() {
      AgentContext context = AgentContext.create();

      int first = context.incrementTurn();
      int second = context.incrementTurn();
      int third = context.incrementTurn();

      assertEquals(1, first);
      assertEquals(2, second);
      assertEquals(3, third);
      assertEquals(3, context.getTurnCount());
    }
  }

  @Nested
  @DisplayName("Copy Functionality")
  class CopyFunctionality {

    @Test
    @DisplayName("copy() creates independent copy with same history")
    void copy_createsIndependentCopyWithHistory() {
      AgentContext original = AgentContext.create();
      original.addMessage(Message.user(Text.valueOf("Hello")));

      AgentContext copy = original.copy();

      assertEquals(original.historySize(), copy.historySize());
      assertNotSame(original, copy);
    }

    @Test
    @DisplayName("copy() isolates state changes")
    void copy_isolatesStateChanges() {
      AgentContext original = AgentContext.create();
      original.setState("shared", "initial");

      AgentContext copy = original.copy();
      copy.setState("shared", "modified");
      copy.setState("new", "value");

      // Original unchanged
      assertEquals("initial", original.getState("shared").orElse(null));
      assertFalse(original.hasState("new"));

      // Copy has changes
      assertEquals("modified", copy.getState("shared").orElse(null));
      assertTrue(copy.hasState("new"));
    }

    @Test
    @DisplayName("copy() preserves turn count")
    void copy_preservesTurnCount() {
      AgentContext original = AgentContext.create();
      original.incrementTurn();
      original.incrementTurn();

      AgentContext copy = original.copy();

      assertEquals(2, copy.getTurnCount());
    }

    @Test
    @DisplayName("copy() isolates history modifications")
    void copy_isolatesHistoryModifications() {
      AgentContext original = AgentContext.create();
      original.addMessage(Message.user(Text.valueOf("Original")));

      AgentContext copy = original.copy();
      copy.addMessage(Message.user(Text.valueOf("New")));

      assertEquals(1, original.historySize());
      assertEquals(2, copy.historySize());
    }
  }

  @Nested
  @DisplayName("Clear Functionality")
  class ClearFunctionality {

    @Test
    @DisplayName("clear() removes all history")
    void clear_removesAllHistory() {
      AgentContext context = AgentContext.create();
      context.addMessage(Message.user(Text.valueOf("Test")));

      context.clear();

      assertEquals(0, context.historySize());
    }

    @Test
    @DisplayName("clear() removes all state")
    void clear_removesAllState() {
      AgentContext context = AgentContext.create();
      context.setState("key", "value");

      context.clear();

      assertFalse(context.hasState("key"));
      assertTrue(context.getAllState().isEmpty());
    }

    @Test
    @DisplayName("clear() resets turn count")
    void clear_resetsTurnCount() {
      AgentContext context = AgentContext.create();
      context.incrementTurn();
      context.incrementTurn();

      context.clear();

      assertEquals(0, context.getTurnCount());
    }

    @Test
    @DisplayName("clear() supports chaining")
    void clear_supportsChaining() {
      AgentContext context = AgentContext.create();

      AgentContext result = context.clear();

      assertSame(context, result);
    }
  }
}
