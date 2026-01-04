package com.paragon.responses.spec;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for Message class factory methods and builder.
 *
 * <p>Tests cover: - Developer/User/Assistant factory methods - Multi-content messages - Builder
 * pattern - Edge cases and validation
 */
@DisplayName("Message Tests")
class MessageTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // DEVELOPER MESSAGE FACTORY
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Developer Messages")
  class DeveloperMessages {

    @Test
    @DisplayName("creates developer message from string")
    void createsFromString() {
      DeveloperMessage msg = Message.developer("You are a helpful assistant.");

      assertNotNull(msg);
      assertEquals(MessageRole.DEVELOPER, msg.role());
      assertEquals(1, msg.content().size());
      assertEquals(InputMessageStatus.COMPLETED, msg.status());
    }

    @Test
    @DisplayName("creates developer message with status")
    void createsWithStatus() {
      DeveloperMessage msg = Message.developer("Processing...", InputMessageStatus.IN_PROGRESS);

      assertEquals(InputMessageStatus.IN_PROGRESS, msg.status());
    }

    @Test
    @DisplayName("creates developer message from varargs")
    void createsFromVarargs() {
      DeveloperMessage msg = Message.developer("Be helpful.", "Be concise.", "Use proper grammar.");

      assertEquals(3, msg.content().size());
    }

    @Test
    @DisplayName("creates developer message from content list")
    void createsFromContentList() {
      List<MessageContent> content =
          List.of(Text.valueOf("Instruction 1"), Text.valueOf("Instruction 2"));

      DeveloperMessage msg = Message.developer(content);

      assertEquals(2, msg.content().size());
    }

    @Test
    @DisplayName("throws on empty string varargs")
    void throwsOnEmptyVarargs() {
      assertThrows(IllegalArgumentException.class, () -> Message.developer(new String[0]));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // USER MESSAGE FACTORY
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("User Messages")
  class UserMessages {

    @Test
    @DisplayName("creates user message from string")
    void createsFromString() {
      UserMessage msg = Message.user("What is the weather?");

      assertNotNull(msg);
      assertEquals(MessageRole.USER, msg.role());
      assertEquals(1, msg.content().size());
    }

    @Test
    @DisplayName("creates user message with status")
    void createsWithStatus() {
      UserMessage msg = Message.user("Typing...", InputMessageStatus.IN_PROGRESS);

      assertEquals(InputMessageStatus.IN_PROGRESS, msg.status());
    }

    @Test
    @DisplayName("creates user message from varargs")
    void createsFromVarargs() {
      UserMessage msg = Message.user("First part.", "Second part.");

      assertEquals(2, msg.content().size());
    }

    @Test
    @DisplayName("creates user message from single content")
    void createsFromSingleContent() {
      Text text = Text.valueOf("Hello");
      UserMessage msg = Message.user(text);

      assertEquals(1, msg.content().size());
    }

    @Test
    @DisplayName("creates user message from content list")
    void createsFromContentList() {
      List<MessageContent> content =
          List.of(Text.valueOf("Analyze this:"), Text.valueOf("More context"));

      UserMessage msg = Message.user(content);

      assertEquals(2, msg.content().size());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ASSISTANT MESSAGE FACTORY
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Assistant Messages")
  class AssistantMessages {

    @Test
    @DisplayName("creates assistant message from string")
    void createsFromString() {
      AssistantMessage msg = Message.assistant("The weather is sunny.");

      assertNotNull(msg);
      assertEquals(MessageRole.ASSISTANT, msg.role());
      assertEquals(1, msg.content().size());
    }

    @Test
    @DisplayName("creates assistant message with status")
    void createsWithStatus() {
      AssistantMessage msg = Message.assistant("Generating...", InputMessageStatus.IN_PROGRESS);

      assertEquals(InputMessageStatus.IN_PROGRESS, msg.status());
    }

    @Test
    @DisplayName("creates assistant message from varargs")
    void createsFromVarargs() {
      AssistantMessage msg = Message.assistant("First response.", "Second response.");

      assertEquals(2, msg.content().size());
    }

    @Test
    @DisplayName("creates assistant message from content")
    void createsFromContent() {
      Text text = Text.valueOf("Response text");
      AssistantMessage msg = Message.assistant(text);

      assertEquals(1, msg.content().size());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // BUILDER PATTERN
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Message Builder")
  class MessageBuilder {

    @Test
    @DisplayName("builds user message")
    void buildsUserMessage() {
      UserMessage msg = Message.builder().addText("Hello").asUser();

      assertNotNull(msg);
      assertEquals(MessageRole.USER, msg.role());
    }

    @Test
    @DisplayName("builds developer message")
    void buildsDeveloperMessage() {
      DeveloperMessage msg = Message.builder().addText("Be helpful").asDeveloper();

      assertEquals(MessageRole.DEVELOPER, msg.role());
    }

    @Test
    @DisplayName("builds assistant message")
    void buildsAssistantMessage() {
      AssistantMessage msg = Message.builder().addText("Response").asAssistant();

      assertEquals(MessageRole.ASSISTANT, msg.role());
    }

    @Test
    @DisplayName("builds with multiple text segments")
    void buildsWithMultipleText() {
      UserMessage msg =
          Message.builder().addText("First").addText("Second").addText("Third").asUser();

      assertEquals(3, msg.content().size());
    }

    @Test
    @DisplayName("builds with status")
    void buildsWithStatus() {
      UserMessage msg =
          Message.builder().addText("Text").status(InputMessageStatus.IN_PROGRESS).asUser();

      assertEquals(InputMessageStatus.IN_PROGRESS, msg.status());
    }

    @Test
    @DisplayName("builds with content item")
    void buildsWithContentItem() {
      Text text = Text.valueOf("Custom text");
      UserMessage msg = Message.builder().addContent(text).asUser();

      assertEquals(1, msg.content().size());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MESSAGE ACCESSORS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Message Accessors")
  class MessageAccessors {

    @Test
    @DisplayName("outputText returns concatenated text")
    void outputTextReturnsConcatenated() {
      UserMessage msg = Message.user("Hello ", "World");

      String text = msg.outputText();

      assertEquals("Hello World", text);
    }

    @Test
    @DisplayName("content returns immutable list")
    void contentReturnsImmutableList() {
      UserMessage msg = Message.user("Test");

      assertThrows(
          UnsupportedOperationException.class, () -> msg.content().add(Text.valueOf("Illegal")));
    }

    @Test
    @DisplayName("typeString returns message")
    void typeStringReturnsMessage() {
      UserMessage msg = Message.user("Test");

      assertEquals("message", msg.typeString());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // VALIDATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Validation")
  class Validation {

    @Test
    @DisplayName("throws on empty content list")
    void throwsOnEmptyContent() {
      assertThrows(IllegalArgumentException.class, () -> Message.user(List.of()));
    }

    @Test
    @DisplayName("throws on null varargs")
    void throwsOnNullVarargs() {
      assertThrows(IllegalArgumentException.class, () -> Message.user((String[]) null));
    }

    @Test
    @DisplayName("developer with content list and status")
    void developerWithContentListAndStatus() {
      List<MessageContent> content =
          List.of(Text.valueOf("Instruction 1"), Text.valueOf("Instruction 2"));
      DeveloperMessage msg = Message.developer(content, InputMessageStatus.COMPLETED);

      assertEquals(2, msg.content().size());
      assertEquals(InputMessageStatus.COMPLETED, msg.status());
    }

    @Test
    @DisplayName("user with content list and status")
    void userWithContentListAndStatus() {
      List<MessageContent> content =
          List.of(Text.valueOf("User input 1"), Text.valueOf("User input 2"));
      UserMessage msg = Message.user(content, InputMessageStatus.IN_PROGRESS);

      assertEquals(2, msg.content().size());
      assertEquals(InputMessageStatus.IN_PROGRESS, msg.status());
    }

    @Test
    @DisplayName("assistant with content list and status")
    void assistantWithContentListAndStatus() {
      List<MessageContent> content =
          List.of(Text.valueOf("Response 1"), Text.valueOf("Response 2"));
      AssistantMessage msg = Message.assistant(content, InputMessageStatus.COMPLETED);

      assertEquals(2, msg.content().size());
      assertEquals(InputMessageStatus.COMPLETED, msg.status());
    }

    @Test
    @DisplayName("developer with single content")
    void developerWithSingleContent() {
      Text text = Text.valueOf("Single instruction");
      DeveloperMessage msg = Message.developer(text);

      assertEquals(1, msg.content().size());
      assertTrue(msg.content().get(0) instanceof Text);
    }

    @Test
    @DisplayName("assistant with single content")
    void assistantWithSingleContent() {
      Text text = Text.valueOf("Single response");
      AssistantMessage msg = Message.assistant(text);

      assertEquals(1, msg.content().size());
      assertTrue(msg.content().get(0) instanceof Text);
    }

    @Test
    @DisplayName("assistant from content list")
    void assistantFromContentList() {
      List<MessageContent> content = List.of(Text.valueOf("Part 1"), Text.valueOf("Part 2"));
      AssistantMessage msg = Message.assistant(content);

      assertEquals(2, msg.content().size());
    }
  }
}
