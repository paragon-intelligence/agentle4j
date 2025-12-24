package com.paragon.responses;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.paragon.responses.spec.Message;

/**
 * Tests for MessageHistory record.
 */
@DisplayName("MessageHistory Tests")
class MessageHistoryTest {

  @Nested
  @DisplayName("Factory Methods")
  class FactoryMethods {

    @Test
    @DisplayName("of() creates empty history")
    void ofCreatesEmptyHistory() {
      MessageHistory history = MessageHistory.of();
      
      assertNotNull(history);
      assertTrue(history.messages().isEmpty());
    }

    @Test
    @DisplayName("of(list) creates history with messages")
    void ofCreatesWithMessages() {
      List<Message> messages = List.of(
          Message.user("Hello"),
          Message.assistant("Hi there"));
      
      MessageHistory history = MessageHistory.of(messages);
      
      assertEquals(2, history.messages().size());
      assertEquals(messages, history.messages());
    }

    @Test
    @DisplayName("of(mutable list) preserves messages")
    void ofPreservesMutableList() {
      List<Message> messages = new ArrayList<>();
      messages.add(Message.user("First"));
      
      MessageHistory history = MessageHistory.of(messages);
      
      assertEquals(1, history.messages().size());
    }
  }

  @Nested
  @DisplayName("Record Behavior")
  class RecordBehavior {

    @Test
    @DisplayName("messages() returns the list")
    void messagesReturnsTheList() {
      List<Message> messages = List.of(Message.user("Test"));
      MessageHistory history = new MessageHistory(messages);
      
      assertSame(messages, history.messages());
    }

    @Test
    @DisplayName("equality works correctly")
    void equalityWorks() {
      List<Message> msgs = List.of(Message.user("Same"));
      MessageHistory h1 = MessageHistory.of(msgs);
      MessageHistory h2 = MessageHistory.of(msgs);
      
      assertEquals(h1, h2);
    }
  }
}
