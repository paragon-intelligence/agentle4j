package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for Conversation, AssistantMessage, DeveloperMessage, and UserMessage classes.
 */
class ConversationAndMessagesTest {

  @Nested
  @DisplayName("Conversation record")
  class ConversationTests {

    @Test
    @DisplayName("Conversation can be created with id")
    void createWithId() {
      Conversation conv = new Conversation("conv-123");
      assertEquals("conv-123", conv.id());
    }

    @Test
    @DisplayName("Conversations with same id are equal")
    void equality() {
      Conversation conv1 = new Conversation("conv-abc");
      Conversation conv2 = new Conversation("conv-abc");

      assertEquals(conv1, conv2);
      assertEquals(conv1.hashCode(), conv2.hashCode());
    }

    @Test
    @DisplayName("Conversations with different ids are not equal")
    void notEqual() {
      Conversation conv1 = new Conversation("conv-1");
      Conversation conv2 = new Conversation("conv-2");

      assertNotEquals(conv1, conv2);
    }
  }

  @Nested
  @DisplayName("AssistantMessage class")
  class AssistantMessageTests {

    @Test
    @DisplayName("AssistantMessage can be created via constructor")
    void constructorCreation() {
      List<MessageContent> content = List.of(new Text("Hello!"));
      AssistantMessage message = new AssistantMessage(content, InputMessageStatus.COMPLETED);

      assertEquals(content, message.content());
      assertEquals(InputMessageStatus.COMPLETED, message.status());
    }

    @Test
    @DisplayName("AssistantMessage.of factory method works")
    void factoryMethod() {
      List<MessageContent> content = List.of(new Text("Hi there"));
      AssistantMessage message = AssistantMessage.of(content, InputMessageStatus.IN_PROGRESS);

      assertEquals(content, message.content());
      assertEquals(InputMessageStatus.IN_PROGRESS, message.status());
    }

    @Test
    @DisplayName("AssistantMessage role is ASSISTANT")
    void roleIsAssistant() {
      AssistantMessage message =
          new AssistantMessage(List.of(new Text("Test")), null);

      assertEquals(MessageRole.ASSISTANT, message.role());
    }

    @Test
    @DisplayName("AssistantMessage with null status is valid")
    void nullStatusAllowed() {
      AssistantMessage message =
          new AssistantMessage(List.of(new Text("Test")), null);

      assertNull(message.status());
    }

    @Test
    @DisplayName("AssistantMessage extends Message")
    void extendsMessage() {
      AssistantMessage message =
          new AssistantMessage(List.of(new Text("Test")), null);

      assertTrue(message instanceof Message);
    }
  }

  @Nested
  @DisplayName("UserMessage class")
  class UserMessageTests {

    @Test
    @DisplayName("UserMessage can be created via constructor")
    void constructorCreation() {
      List<MessageContent> content = List.of(new Text("User input"));
      UserMessage message = new UserMessage(content, InputMessageStatus.COMPLETED);

      assertEquals(content, message.content());
      assertEquals(InputMessageStatus.COMPLETED, message.status());
    }

    @Test
    @DisplayName("UserMessage.of factory method works")
    void factoryMethod() {
      List<MessageContent> content = List.of(new Text("Question?"));
      UserMessage message = UserMessage.of(content, null);

      assertEquals(content, message.content());
      assertNull(message.status());
    }

    @Test
    @DisplayName("UserMessage role is USER")
    void roleIsUser() {
      UserMessage message =
          new UserMessage(List.of(new Text("Test")), null);

      assertEquals(MessageRole.USER, message.role());
    }

    @Test
    @DisplayName("UserMessage extends Message")
    void extendsMessage() {
      UserMessage message =
          new UserMessage(List.of(new Text("Test")), null);

      assertTrue(message instanceof Message);
    }
  }

  @Nested
  @DisplayName("DeveloperMessage class")
  class DeveloperMessageTests {

    @Test
    @DisplayName("DeveloperMessage can be created via constructor")
    void constructorCreation() {
      List<MessageContent> content = List.of(new Text("System prompt"));
      DeveloperMessage message = new DeveloperMessage(content, InputMessageStatus.COMPLETED);

      assertEquals(content, message.content());
      assertEquals(InputMessageStatus.COMPLETED, message.status());
    }

    @Test
    @DisplayName("DeveloperMessage.of factory method works")
    void factoryMethod() {
      List<MessageContent> content = List.of(new Text("Developer instruction"));
      DeveloperMessage message = DeveloperMessage.of(content, null);

      assertEquals(content, message.content());
      assertNull(message.status());
    }

    @Test
    @DisplayName("DeveloperMessage role is DEVELOPER")
    void roleIsDeveloper() {
      DeveloperMessage message =
          new DeveloperMessage(List.of(new Text("Test")), null);

      assertEquals(MessageRole.DEVELOPER, message.role());
    }

    @Test
    @DisplayName("DeveloperMessage extends Message")
    void extendsMessage() {
      DeveloperMessage message =
          new DeveloperMessage(List.of(new Text("Test")), null);

      assertTrue(message instanceof Message);
    }
  }

  @Nested
  @DisplayName("Message polymorphism")
  class MessagePolymorphismTests {

    @Test
    @DisplayName("Different message types have different roles")
    void differentRoles() {
      Message assistant = new AssistantMessage(List.of(new Text("A")), null);
      Message user = new UserMessage(List.of(new Text("U")), null);
      Message developer = new DeveloperMessage(List.of(new Text("D")), null);

      assertNotEquals(assistant.role(), user.role());
      assertNotEquals(user.role(), developer.role());
      assertNotEquals(assistant.role(), developer.role());
    }

    @Test
    @DisplayName("All message types can be treated as Message")
    void polymorphicUsage() {
      List<Message> messages =
          List.of(
              new UserMessage(List.of(new Text("Hello")), null),
              new AssistantMessage(List.of(new Text("Hi")), InputMessageStatus.COMPLETED),
              new DeveloperMessage(List.of(new Text("System")), null));

      assertEquals(3, messages.size());
      assertEquals(MessageRole.USER, messages.get(0).role());
      assertEquals(MessageRole.ASSISTANT, messages.get(1).role());
      assertEquals(MessageRole.DEVELOPER, messages.get(2).role());
    }
  }
}
