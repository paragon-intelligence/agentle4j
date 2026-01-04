package com.paragon.prompts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Response DTO for Langfuse prompt API.
 *
 * <p>Handles both "text" and "chat" prompt types from the Langfuse API.
 *
 * @author Agentle Framework
 * @since 1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LangfusePromptResponse(
    @JsonProperty("type") @NonNull String type,
    @JsonProperty("name") @NonNull String name,
    @JsonProperty("version") int version,
    @JsonProperty("prompt") @Nullable Object prompt,
    @JsonProperty("config") @Nullable Object config,
    @JsonProperty("labels") @Nullable List<String> labels,
    @JsonProperty("tags") @Nullable List<String> tags,
    @JsonProperty("commitMessage") @Nullable String commitMessage,
    @JsonProperty("resolutionGraph") @Nullable Map<String, Object> resolutionGraph) {

  /** Constant for text prompt type. */
  public static final String TYPE_TEXT = "text";

  /** Constant for chat prompt type. */
  public static final String TYPE_CHAT = "chat";

  /**
   * Returns whether this is a text prompt.
   *
   * @return true if text prompt, false otherwise
   */
  public boolean isTextPrompt() {
    return TYPE_TEXT.equals(type);
  }

  /**
   * Returns whether this is a chat prompt.
   *
   * @return true if chat prompt, false otherwise
   */
  public boolean isChatPrompt() {
    return TYPE_CHAT.equals(type);
  }

  /**
   * Returns the prompt content as a string.
   *
   * <p>For text prompts, returns the prompt string directly. For chat prompts, concatenates all
   * message contents with newlines.
   *
   * @return the prompt content as a string
   */
  @SuppressWarnings("unchecked")
  public @NonNull String getPromptContent() {
    if (prompt == null) {
      return "";
    }

    if (isTextPrompt()) {
      return prompt.toString();
    }

    if (isChatPrompt() && prompt instanceof List<?> messages) {
      StringBuilder sb = new StringBuilder();
      for (Object msg : messages) {
        if (msg instanceof Map<?, ?> messageMap) {
          Object role = messageMap.get("role");
          Object content = messageMap.get("content");
          if (role != null && content != null) {
            if (!sb.isEmpty()) {
              sb.append("\n");
            }
            sb.append(role).append(": ").append(content);
          }
        }
      }
      return sb.toString();
    }

    return prompt.toString();
  }

  /**
   * Returns the chat messages if this is a chat prompt.
   *
   * @return list of chat messages, or empty list if not a chat prompt
   */
  @SuppressWarnings("unchecked")
  public @NonNull List<ChatMessage> getChatMessages() {
    if (!isChatPrompt() || !(prompt instanceof List<?> messages)) {
      return List.of();
    }

    return messages.stream()
        .filter(msg -> msg instanceof Map<?, ?>)
        .map(
            msg -> {
              Map<String, Object> map = (Map<String, Object>) msg;
              String role = map.get("role") != null ? map.get("role").toString() : "";
              String content = map.get("content") != null ? map.get("content").toString() : "";
              String msgType = map.get("type") != null ? map.get("type").toString() : "chatmessage";
              return new ChatMessage(role, content, msgType);
            })
        .toList();
  }

  /**
   * Represents a chat message in a chat prompt.
   *
   * @param role the message role (e.g., "user", "assistant", "system")
   * @param content the message content
   * @param type the message type (e.g., "chatmessage", "placeholder")
   */
  public record ChatMessage(@NonNull String role, @NonNull String content, @NonNull String type) {

    /**
     * Returns whether this is a placeholder message.
     *
     * @return true if placeholder, false otherwise
     */
    public boolean isPlaceholder() {
      return "placeholder".equals(type);
    }
  }
}
