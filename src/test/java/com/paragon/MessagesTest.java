package com.paragon;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.spec.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for Messages utility class.
 */
@DisplayName("Messages Tests")
class MessagesTest {

  @Nested
  @DisplayName("Factory Methods")
  class FactoryMethods {

    @Test
    @DisplayName("of() creates empty messages")
    void ofCreatesEmpty() {
      Messages messages = Messages.of();
      
      assertNotNull(messages);
      assertTrue(messages.messages().isEmpty());
    }

    @Test
    @DisplayName("of(varargs) creates messages with items")
    void ofCreatesWithItems() {
      Message m1 = Message.user("Hello");
      Message m2 = Message.assistant("Hi there");
      
      Messages messages = Messages.of(m1, m2);
      
      assertEquals(2, messages.messages().size());
      assertEquals(m1, messages.messages().get(0));
      assertEquals(m2, messages.messages().get(1));
    }

    @Test
    @DisplayName("of(single) creates messages with one item")
    void ofCreatesWithSingleItem() {
      Message m = Message.user("Test");
      
      Messages messages = Messages.of(m);
      
      assertEquals(1, messages.messages().size());
    }
  }

  @Nested
  @DisplayName("Add Method")
  class AddMethod {

    @Test
    @DisplayName("add() appends message and returns same instance")
    void addAppendsMessage() {
      Messages messages = Messages.of();
      Message m = Message.user("New message");
      
      Messages result = messages.add(m);
      
      assertSame(messages, result);
      assertEquals(1, messages.messages().size());
      assertEquals(m, messages.messages().get(0));
    }

    @Test
    @DisplayName("add() can chain multiple messages")
    void addChainsMultiple() {
      Messages messages = Messages.of()
          .add(Message.user("First"))
          .add(Message.assistant("Second"))
          .add(Message.user("Third"));
      
      assertEquals(3, messages.messages().size());
    }
  }
}
