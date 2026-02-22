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
class AgenticContextTest {

  @Nested
  @DisplayName("Creation")
  class Creation {

    @Test
    @DisplayName("create() returns fresh context with no history")
    void create_returnsFreshContext() {
      AgenticContext context = AgenticContext.create();

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

      AgenticContext context = AgenticContext.withHistory(history);

      assertEquals(1, context.historySize());
      assertEquals(message, context.getHistory().getFirst());
    }

    @Test
    @DisplayName("withHistory() throws NullPointerException when null")
    void withHistory_throwsWhenNull() {
      assertThrows(NullPointerException.class, () -> AgenticContext.withHistory(null));
    }
  }

  @Nested
  @DisplayName("History Management")
  class HistoryManagement {

    @Test
    @DisplayName("addMessage() adds message to history")
    void addMessage_addsToHistory() {
      AgenticContext context = AgenticContext.create();
      Message message = Message.user(Text.valueOf("Test"));

      context.addMessage(message);

      assertEquals(1, context.historySize());
      assertTrue(context.getHistory().contains(message));
    }

    @Test
    @DisplayName("addMessage() supports chaining")
    void addMessage_supportsChaining() {
      AgenticContext context = AgenticContext.create();
      Message msg1 = Message.user(Text.valueOf("First"));
      Message msg2 = Message.user(Text.valueOf("Second"));

      AgenticContext result = context.addMessage(msg1).addMessage(msg2);

      assertSame(context, result);
      assertEquals(2, context.historySize());
    }

    @Test
    @DisplayName("addInput() adds ResponseInputItem to history")
    void addInput_addsToHistory() {
      AgenticContext context = AgenticContext.create();
      Message message = Message.user(Text.valueOf("Input"));

      context.addInput(message);

      assertEquals(1, context.historySize());
    }

    @Test
    @DisplayName("getHistory() returns unmodifiable list")
    void getHistory_returnsUnmodifiableList() {
      AgenticContext context = AgenticContext.create();
      context.addMessage(Message.user(Text.valueOf("Test")));

      List<ResponseInputItem> history = context.getHistory();

      assertThrows(
          UnsupportedOperationException.class,
          () -> history.add(Message.user(Text.valueOf("New"))));
    }

    @Test
    @DisplayName("getHistoryMutable() returns mutable copy")
    void getHistoryMutable_returnsMutableCopy() {
      AgenticContext context = AgenticContext.create();
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
      AgenticContext context = AgenticContext.create();

      context.setState("key", "value");

      assertEquals("value", context.getState("key").orElse(null));
    }

    @Test
    @DisplayName("setState() supports chaining")
    void setState_supportsChaining() {
      AgenticContext context = AgenticContext.create();

      AgenticContext result = context.setState("a", 1).setState("b", 2);

      assertSame(context, result);
      assertEquals(1, context.getState("a").orElse(null));
      assertEquals(2, context.getState("b").orElse(null));
    }

    @Test
    @DisplayName("setState() with null value removes key")
    void setState_nullRemovesKey() {
      AgenticContext context = AgenticContext.create();
      context.setState("key", "value");

      context.setState("key", null);

      assertTrue(context.getState("key").isEmpty());
      assertFalse(context.hasState("key"));
    }

    @Test
    @DisplayName("getState() with type returns properly typed value")
    void getState_withType_returnsTypedValue() {
      AgenticContext context = AgenticContext.create();
      context.setState("number", 42);

      Integer value = context.getState("number", Integer.class).orElse(null);

      assertEquals(42, value);
    }

    @Test
    @DisplayName("getState() returns empty for missing key")
    void getState_returnsEmptyForMissingKey() {
      AgenticContext context = AgenticContext.create();

      assertTrue(context.getState("nonexistent").isEmpty());
    }

    @Test
    @DisplayName("hasState() returns true for existing key")
    void hasState_returnsTrueForExistingKey() {
      AgenticContext context = AgenticContext.create();
      context.setState("key", "value");

      assertTrue(context.hasState("key"));
    }

    @Test
    @DisplayName("hasState() returns false for missing key")
    void hasState_returnsFalseForMissingKey() {
      AgenticContext context = AgenticContext.create();

      assertFalse(context.hasState("nonexistent"));
    }

    @Test
    @DisplayName("getAllState() returns unmodifiable map")
    void getAllState_returnsUnmodifiableMap() {
      AgenticContext context = AgenticContext.create();
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
      AgenticContext context = AgenticContext.create();

      assertEquals(0, context.getTurnCount());
    }

    @Test
    @DisplayName("incrementTurn() increments and returns new count")
    void incrementTurn_incrementsAndReturns() {
      AgenticContext context = AgenticContext.create();

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
      AgenticContext original = AgenticContext.create();
      original.addMessage(Message.user(Text.valueOf("Hello")));

      AgenticContext copy = original.copy();

      assertEquals(original.historySize(), copy.historySize());
      assertNotSame(original, copy);
    }

    @Test
    @DisplayName("copy() isolates state changes")
    void copy_isolatesStateChanges() {
      AgenticContext original = AgenticContext.create();
      original.setState("shared", "initial");

      AgenticContext copy = original.copy();
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
      AgenticContext original = AgenticContext.create();
      original.incrementTurn();
      original.incrementTurn();

      AgenticContext copy = original.copy();

      assertEquals(2, copy.getTurnCount());
    }

    @Test
    @DisplayName("copy() isolates history modifications")
    void copy_isolatesHistoryModifications() {
      AgenticContext original = AgenticContext.create();
      original.addMessage(Message.user(Text.valueOf("Original")));

      AgenticContext copy = original.copy();
      copy.addMessage(Message.user(Text.valueOf("New")));

      assertEquals(1, original.historySize());
      assertEquals(2, copy.historySize());
    }
  }

  @Nested
  @DisplayName("Utility Methods")
  class UtilityMethods {

    @Test
    @DisplayName("extractLastUserMessageText returns last user message text")
    void extractLastUserMessageText_returnsLastUserMessage() {
      AgenticContext context = AgenticContext.create();
      context.addInput(Message.user("First"));
      context.addInput(Message.assistant("Response"));
      context.addInput(Message.user("Second"));

      var result = context.extractLastUserMessageText();

      assertTrue(result.isPresent());
      assertEquals("Second", result.get());
    }

    @Test
    @DisplayName("extractLastUserMessageText returns empty when no user messages")
    void extractLastUserMessageText_returnsEmptyWhenNoUserMessages() {
      AgenticContext context = AgenticContext.create();
      context.addInput(Message.assistant("Response"));

      var result = context.extractLastUserMessageText();

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("extractLastUserMessageText returns empty for empty history")
    void extractLastUserMessageText_returnsEmptyForEmptyHistory() {
      AgenticContext context = AgenticContext.create();

      assertTrue(context.extractLastUserMessageText().isEmpty());
    }

    @Test
    @DisplayName("extractLastUserMessageText with fallback returns fallback when no user message")
    void extractLastUserMessageText_fallback_returnsFallback() {
      AgenticContext context = AgenticContext.create();

      assertEquals("[No query]", context.extractLastUserMessageText("[No query]"));
    }

    @Test
    @DisplayName("extractLastUserMessageText with fallback returns text when present")
    void extractLastUserMessageText_fallback_returnsTextWhenPresent() {
      AgenticContext context = AgenticContext.create();
      context.addInput(Message.user("Hello"));

      assertEquals("Hello", context.extractLastUserMessageText("[No query]"));
    }

    @Test
    @DisplayName("ensureTraceContext sets trace IDs when not present")
    void ensureTraceContext_setsTraceIds() {
      AgenticContext context = AgenticContext.create();
      assertFalse(context.hasTraceContext());

      context.ensureTraceContext();

      assertTrue(context.hasTraceContext());
      assertTrue(context.parentTraceId().isPresent());
      assertTrue(context.parentSpanId().isPresent());
    }

    @Test
    @DisplayName("ensureTraceContext preserves existing trace IDs")
    void ensureTraceContext_preservesExistingTraceIds() {
      AgenticContext context = AgenticContext.create();
      context.withTraceContext("my-trace-id", "my-span-id");

      context.ensureTraceContext();

      assertEquals("my-trace-id", context.parentTraceId().orElse(null));
      assertEquals("my-span-id", context.parentSpanId().orElse(null));
    }

    @Test
    @DisplayName("ensureTraceContext returns this for chaining")
    void ensureTraceContext_returnsThis() {
      AgenticContext context = AgenticContext.create();

      assertSame(context, context.ensureTraceContext());
    }

    @Test
    @DisplayName("createChildContext with shareHistory forks full context")
    void createChildContext_shareHistory_forksFullContext() {
      AgenticContext parent = AgenticContext.create();
      parent.addInput(Message.user("Original"));
      parent.setState("userId", "user-123");
      parent.withTraceContext("trace-1", "span-1");

      AgenticContext child = parent.createChildContext(true, true, "New request");

      // Child should have parent's history + new message
      assertEquals(2, child.historySize());
      // Child should have state
      assertEquals("user-123", child.getState("userId").orElse(null));
      // Child should have trace context (with same traceId)
      assertTrue(child.hasTraceContext());
      assertEquals("trace-1", child.parentTraceId().orElse(null));
      // Turn count reset
      assertEquals(0, child.getTurnCount());
    }

    @Test
    @DisplayName("createChildContext with shareState copies state only")
    void createChildContext_shareStateOnly_copiesStateButNotHistory() {
      AgenticContext parent = AgenticContext.create();
      parent.addInput(Message.user("Original"));
      parent.setState("userId", "user-123");
      parent.withTraceContext("trace-1", "span-1");
      parent.withRequestId("req-1");

      AgenticContext child = parent.createChildContext(true, false, "New request");

      // Child should only have the new message (fresh history)
      assertEquals(1, child.historySize());
      // Child should have state
      assertEquals("user-123", child.getState("userId").orElse(null));
      // Child should have trace context
      assertTrue(child.hasTraceContext());
      assertEquals("trace-1", child.parentTraceId().orElse(null));
      // Child should have request ID
      assertEquals("req-1", child.requestId().orElse(null));
    }

    @Test
    @DisplayName("createChildContext isolated creates completely clean context")
    void createChildContext_isolated_createsCleanContext() {
      AgenticContext parent = AgenticContext.create();
      parent.addInput(Message.user("Original"));
      parent.setState("userId", "user-123");
      parent.withTraceContext("trace-1", "span-1");

      AgenticContext child = parent.createChildContext(false, false, "New request");

      // Child should only have the new message
      assertEquals(1, child.historySize());
      // No state
      assertFalse(child.hasState("userId"));
      // No trace context
      assertFalse(child.hasTraceContext());
    }

    @Test
    @DisplayName("createChildContext shareState without trace copies state but no trace")
    void createChildContext_shareStateNoTrace_copiesStateOnly() {
      AgenticContext parent = AgenticContext.create();
      parent.setState("key", "value");
      // No trace set

      AgenticContext child = parent.createChildContext(true, false, "Request");

      assertEquals("value", child.getState("key").orElse(null));
      assertFalse(child.hasTraceContext());
      assertEquals(1, child.historySize());
    }
  }

  @Nested
  @DisplayName("Clear Functionality")
  class ClearFunctionality {

    @Test
    @DisplayName("clear() removes all history")
    void clear_removesAllHistory() {
      AgenticContext context = AgenticContext.create();
      context.addMessage(Message.user(Text.valueOf("Test")));

      context.clear();

      assertEquals(0, context.historySize());
    }

    @Test
    @DisplayName("clear() removes all state")
    void clear_removesAllState() {
      AgenticContext context = AgenticContext.create();
      context.setState("key", "value");

      context.clear();

      assertFalse(context.hasState("key"));
      assertTrue(context.getAllState().isEmpty());
    }

    @Test
    @DisplayName("clear() resets turn count")
    void clear_resetsTurnCount() {
      AgenticContext context = AgenticContext.create();
      context.incrementTurn();
      context.incrementTurn();

      context.clear();

      assertEquals(0, context.getTurnCount());
    }

    @Test
    @DisplayName("clear() supports chaining")
    void clear_supportsChaining() {
      AgenticContext context = AgenticContext.create();

      AgenticContext result = context.clear();

      assertSame(context, result);
    }
  }
}
