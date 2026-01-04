package com.paragon.prompts;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link LangfusePromptResponse}. */
class LangfusePromptResponseTest {

  // ===== Type Detection Tests =====

  @Nested
  class TypeDetection {

    @Test
    void isTextPrompt_textType_returnsTrue() {
      LangfusePromptResponse response =
          new LangfusePromptResponse(
              "text", "my-prompt", 1, "Hello world", null, null, null, null, null);

      assertTrue(response.isTextPrompt());
      assertFalse(response.isChatPrompt());
    }

    @Test
    void isChatPrompt_chatType_returnsTrue() {
      LangfusePromptResponse response =
          new LangfusePromptResponse(
              "chat", "my-prompt", 1, List.of(), null, null, null, null, null);

      assertTrue(response.isChatPrompt());
      assertFalse(response.isTextPrompt());
    }

    @Test
    void isTextPrompt_unknownType_returnsFalse() {
      LangfusePromptResponse response =
          new LangfusePromptResponse(
              "unknown", "my-prompt", 1, "content", null, null, null, null, null);

      assertFalse(response.isTextPrompt());
      assertFalse(response.isChatPrompt());
    }
  }

  // ===== Prompt Content Extraction =====

  @Nested
  class PromptContentExtraction {

    @Test
    void getPromptContent_textPrompt_returnsText() {
      LangfusePromptResponse response =
          new LangfusePromptResponse(
              "text", "my-prompt", 1, "Hello, {{name}}!", null, null, null, null, null);

      assertEquals("Hello, {{name}}!", response.getPromptContent());
    }

    @Test
    void getPromptContent_nullPrompt_returnsEmpty() {
      LangfusePromptResponse response =
          new LangfusePromptResponse("text", "my-prompt", 1, null, null, null, null, null, null);

      assertEquals("", response.getPromptContent());
    }

    @Test
    void getPromptContent_chatPrompt_concatenatesMessages() {
      List<Map<String, Object>> messages =
          List.of(
              Map.of("role", "system", "content", "You are a helpful assistant."),
              Map.of("role", "user", "content", "Hello!"));

      LangfusePromptResponse response =
          new LangfusePromptResponse(
              "chat", "my-prompt", 1, messages, null, null, null, null, null);

      String content = response.getPromptContent();
      assertTrue(content.contains("system: You are a helpful assistant."));
      assertTrue(content.contains("user: Hello!"));
    }

    @Test
    void getPromptContent_chatPromptSingleMessage_noLeadingNewline() {
      List<Map<String, Object>> messages = List.of(Map.of("role", "user", "content", "Hello!"));

      LangfusePromptResponse response =
          new LangfusePromptResponse(
              "chat", "my-prompt", 1, messages, null, null, null, null, null);

      assertEquals("user: Hello!", response.getPromptContent());
    }

    @Test
    void getPromptContent_chatPromptEmptyMessages_returnsEmpty() {
      LangfusePromptResponse response =
          new LangfusePromptResponse(
              "chat", "my-prompt", 1, List.of(), null, null, null, null, null);

      assertEquals("", response.getPromptContent());
    }

    @Test
    void getPromptContent_chatPromptWithPlaceholder_includesPlaceholder() {
      List<Map<String, Object>> messages =
          List.of(Map.of("role", "user", "content", "Hello, {{name}}!"));

      LangfusePromptResponse response =
          new LangfusePromptResponse(
              "chat", "my-prompt", 1, messages, null, null, null, null, null);

      assertTrue(response.getPromptContent().contains("{{name}}"));
    }

    @Test
    void getPromptContent_unknownType_returnsToString() {
      LangfusePromptResponse response =
          new LangfusePromptResponse("other", "my-prompt", 1, 12345, null, null, null, null, null);

      assertEquals("12345", response.getPromptContent());
    }
  }

  // ===== Chat Messages Extraction =====

  @Nested
  class ChatMessagesExtraction {

    @Test
    void getChatMessages_chatPrompt_returnsMessages() {
      List<Map<String, Object>> messages =
          List.of(
              Map.of("type", "chatmessage", "role", "system", "content", "Be helpful"),
              Map.of("type", "chatmessage", "role", "user", "content", "Hi"));

      LangfusePromptResponse response =
          new LangfusePromptResponse(
              "chat", "my-prompt", 1, messages, null, null, null, null, null);

      List<LangfusePromptResponse.ChatMessage> chatMessages = response.getChatMessages();

      assertEquals(2, chatMessages.size());
      assertEquals("system", chatMessages.get(0).role());
      assertEquals("Be helpful", chatMessages.get(0).content());
      assertEquals("user", chatMessages.get(1).role());
      assertEquals("Hi", chatMessages.get(1).content());
    }

    @Test
    void getChatMessages_textPrompt_returnsEmptyList() {
      LangfusePromptResponse response =
          new LangfusePromptResponse("text", "my-prompt", 1, "Hello", null, null, null, null, null);

      assertTrue(response.getChatMessages().isEmpty());
    }

    @Test
    void getChatMessages_nullPrompt_returnsEmptyList() {
      LangfusePromptResponse response =
          new LangfusePromptResponse("chat", "my-prompt", 1, null, null, null, null, null, null);

      assertTrue(response.getChatMessages().isEmpty());
    }

    @Test
    void getChatMessages_nonListPrompt_returnsEmptyList() {
      LangfusePromptResponse response =
          new LangfusePromptResponse(
              "chat", "my-prompt", 1, "not a list", null, null, null, null, null);

      assertTrue(response.getChatMessages().isEmpty());
    }

    @Test
    void getChatMessages_withMissingType_defaultsToChatMessage() {
      List<Map<String, Object>> messages = List.of(Map.of("role", "user", "content", "Hello"));

      LangfusePromptResponse response =
          new LangfusePromptResponse(
              "chat", "my-prompt", 1, messages, null, null, null, null, null);

      List<LangfusePromptResponse.ChatMessage> chatMessages = response.getChatMessages();
      assertEquals("chatmessage", chatMessages.get(0).type());
    }

    @Test
    void getChatMessages_withNullRole_defaultsToEmpty() {
      List<Map<String, Object>> messages = List.of(Map.of("content", "Hello"));

      LangfusePromptResponse response =
          new LangfusePromptResponse(
              "chat", "my-prompt", 1, messages, null, null, null, null, null);

      List<LangfusePromptResponse.ChatMessage> chatMessages = response.getChatMessages();
      assertEquals("", chatMessages.get(0).role());
    }

    @Test
    void getChatMessages_withNullContent_defaultsToEmpty() {
      List<Map<String, Object>> messages = List.of(Map.of("role", "user"));

      LangfusePromptResponse response =
          new LangfusePromptResponse(
              "chat", "my-prompt", 1, messages, null, null, null, null, null);

      List<LangfusePromptResponse.ChatMessage> chatMessages = response.getChatMessages();
      assertEquals("", chatMessages.get(0).content());
    }

    @Test
    void getChatMessages_filtersNonMapElements() {
      List<Object> messages = List.of(Map.of("role", "user", "content", "Hello"), "not a map", 123);

      LangfusePromptResponse response =
          new LangfusePromptResponse(
              "chat", "my-prompt", 1, messages, null, null, null, null, null);

      List<LangfusePromptResponse.ChatMessage> chatMessages = response.getChatMessages();
      assertEquals(1, chatMessages.size());
    }
  }

  // ===== ChatMessage Tests =====

  @Nested
  class ChatMessageTests {

    @Test
    void isPlaceholder_placeholderType_returnsTrue() {
      LangfusePromptResponse.ChatMessage msg =
          new LangfusePromptResponse.ChatMessage("", "{{messages}}", "placeholder");

      assertTrue(msg.isPlaceholder());
    }

    @Test
    void isPlaceholder_chatMessageType_returnsFalse() {
      LangfusePromptResponse.ChatMessage msg =
          new LangfusePromptResponse.ChatMessage("user", "Hello", "chatmessage");

      assertFalse(msg.isPlaceholder());
    }

    @Test
    void chatMessage_accessors() {
      LangfusePromptResponse.ChatMessage msg =
          new LangfusePromptResponse.ChatMessage("assistant", "Hi there!", "chatmessage");

      assertEquals("assistant", msg.role());
      assertEquals("Hi there!", msg.content());
      assertEquals("chatmessage", msg.type());
    }
  }

  // ===== Record Accessors =====

  @Nested
  class RecordAccessors {

    @Test
    void accessors_returnCorrectValues() {
      LangfusePromptResponse response =
          new LangfusePromptResponse(
              "text",
              "my-prompt",
              5,
              "Content",
              Map.of("key", "value"),
              List.of("production", "latest"),
              List.of("tag1", "tag2"),
              "Initial commit",
              Map.of("dep1", "v1"));

      assertEquals("text", response.type());
      assertEquals("my-prompt", response.name());
      assertEquals(5, response.version());
      assertEquals("Content", response.prompt());
      assertNotNull(response.config());
      assertEquals(2, response.labels().size());
      assertEquals(2, response.tags().size());
      assertEquals("Initial commit", response.commitMessage());
      assertNotNull(response.resolutionGraph());
    }

    @Test
    void accessors_nullableFieldsCanBeNull() {
      LangfusePromptResponse response =
          new LangfusePromptResponse("text", "my-prompt", 1, null, null, null, null, null, null);

      assertNull(response.prompt());
      assertNull(response.config());
      assertNull(response.labels());
      assertNull(response.tags());
      assertNull(response.commitMessage());
      assertNull(response.resolutionGraph());
    }
  }

  // ===== Constants =====

  @Nested
  class Constants {

    @Test
    void typeText_hasCorrectValue() {
      assertEquals("text", LangfusePromptResponse.TYPE_TEXT);
    }

    @Test
    void typeChat_hasCorrectValue() {
      assertEquals("chat", LangfusePromptResponse.TYPE_CHAT);
    }
  }
}
