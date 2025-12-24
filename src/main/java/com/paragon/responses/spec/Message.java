package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents a message input to an AI model with role-based instruction hierarchy.
 *
 * <p>Messages form the conversation context for model interactions, with roles determining
 * instruction precedence: {@code developer} and {@code system} roles override {@code user} role
 * instructions. The {@code assistant} role contains model-generated responses from previous
 * interactions.
 *
 * <p>This is a sealed abstract class with three permitted implementations:
 *
 * <ul>
 *   <li>{@link DeveloperMessage} - Highest priority instructions from developers that guide model
 *       behavior and establish constraints
 *   <li>{@link UserMessage} - Instructions and queries from end users representing the primary
 *       interaction content
 *   <li>{@link AssistantMessage} - Model-generated responses from prior conversation turns that
 *       provide context
 * </ul>
 *
 * <h2>Role Hierarchy and Precedence</h2>
 *
 * <p>The message role determines how the AI model interprets and prioritizes instructions:
 *
 * <ol>
 *   <li><b>Developer Role:</b> Highest priority - establishes system behavior, safety constraints,
 *       and operational parameters
 *   <li><b>User Role:</b> Standard priority - contains user queries, instructions, and interaction
 *       content
 *   <li><b>Assistant Role:</b> Context only - provides conversation history and previous model
 *       responses
 * </ol>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Simple Text Messages</h3>
 *
 * <pre>{@code
 * // Create a basic user message
 * UserMessage userMsg = Message.user("What is the weather today?");
 *
 * // Create a developer instruction
 * DeveloperMessage devMsg = Message.developer("Always respond in JSON format.");
 *
 * // Create an assistant response
 * AssistantMessage assistantMsg = Message.assistant("The weather is sunny and 72°F.");
 * }</pre>
 *
 * <h3>Multi-Content Messages</h3>
 *
 * <pre>{@code
 * // Message with text and image
 * UserMessage multiContent = Message.user(List.of(
 *     Text.valueOf("Analyze this image:"),
 *     Image.fromUrl("https://example.com/chart.jpg")
 * ));
 *
 * // Multiple text segments
 * DeveloperMessage instructions = Message.developer(
 *     "You are a helpful assistant.",
 *     "Always be concise.",
 *     "Use proper grammar."
 * );
 * }</pre>
 *
 * <h3>Using the Builder Pattern</h3>
 *
 * <pre>{@code
 * // Complex message construction
 * UserMessage complexMsg = Message.builder()
 *     .addText("Please analyze the following:")
 *     .addContent(Image.fromBytes(imageData))
 *     .addText("Focus on color distribution.")
 *     .status(InputMessageStatus.COMPLETED)
 *     .asUser();
 *
 * // Conditional content building
 * MessageBuilder builder = Message.builder()
 *     .addText("Process this data:");
 *
 * if (includeImage) {
 *     builder.addContent(imageContent);
 * }
 *
 * UserMessage msg = builder.asUser();
 * }</pre>
 *
 * <h3>Working with Status</h3>
 *
 * <pre>{@code
 * // Create message with specific status
 * UserMessage inProgress = Message.user("Processing...", InputMessageStatus.IN_PROGRESS);
 *
 * // Check message status
 * if (message.isCompleted()) {
 *     processMessage(message);
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 *
 * <p>All Message instances are <b>immutable</b> and therefore <b>thread-safe</b>. The content list
 * is defensively copied during construction and returned as an unmodifiable copy, preventing any
 * external modification.
 *
 * <h2>JSON Serialization</h2>
 *
 * <p>Messages are serialized using Jackson with polymorphic type handling:
 *
 * <ul>
 *   <li>The {@code type} property (from {@link ResponseInputItem}) identifies this as a message
 *   <li>The {@code role} property distinguishes between concrete implementations
 *   <li>Deserialization automatically creates the correct subclass based on the role value
 * </ul>
 *
 * <p>Example JSON representation:
 *
 * <pre>{@code
 * {
 *   "type": "message",
 *   "role": "user",
 *   "content": [
 *     {
 *       "type": "text",
 *       "text": "Hello, world!"
 *     }
 *   ],
 *   "status": "completed"
 * }
 * }</pre>
 *
 * <h2>Validation</h2>
 *
 * <p>The class enforces the following invariants:
 *
 * <ul>
 *   <li>Content list must not be null
 *   <li>Content list must not be empty
 *   <li>Individual content items must not be null
 *   <li>Status may be null for unprocessed messages
 * </ul>
 *
 * @see DeveloperMessage
 * @see UserMessage
 * @see AssistantMessage
 * @see MessageContent
 * @see InputMessageStatus
 * @see ResponseInputItem
 * @since 1.0
 */
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@com.fasterxml.jackson.annotation.JsonTypeInfo(
    use = com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NONE)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(
    using = com.paragon.responses.json.MessageDeserializer.class)
public abstract sealed class Message implements ResponseInputItem, Item
    permits DeveloperMessage, UserMessage, AssistantMessage {

  /**
   * The message content provided to the model.
   *
   * <p>Contains text, image, audio, or other input types used for response generation, or previous
   * assistant responses that provide conversation context. The content list may contain multiple
   * items of various types (text, images, audio) in a single message, enabling rich multi-modal
   * interactions.
   *
   * <p>This field is immutable and guaranteed to be non-null and non-empty. The list is defensively
   * copied during construction to ensure immutability.
   *
   * @see MessageContent
   * @see Text
   * @see Image
   */
  private final @NonNull List<MessageContent> content;

  /**
   * The processing status of this message item.
   *
   * <p>Indicates whether the message is currently being processed ({@link
   * InputMessageStatus#IN_PROGRESS}), has been fully processed ({@link
   * InputMessageStatus#COMPLETED}), or encountered an error ({@link
   * InputMessageStatus#INCOMPLETE}).
   *
   * <p>This field is populated when items are returned from the API and may be {@code null} for
   * newly created messages that haven't been processed yet. A {@code null} status should be treated
   * as equivalent to {@link InputMessageStatus#COMPLETED} for message construction purposes.
   *
   * @see InputMessageStatus
   */
  private final @Nullable InputMessageStatus status;

  /**
   * Constructs a Message with the specified content and status.
   *
   * <p>This constructor performs defensive copying of the content list to ensure immutability and
   * validates that the content is not null or empty.
   *
   * @param content the message content to be processed by the model; must not be null or empty
   * @param status the processing status of the message; may be null for unprocessed messages
   * @throws IllegalArgumentException if content is null or empty
   * @throws NullPointerException if any element in the content list is null
   */
  protected Message(@NonNull List<MessageContent> content, @Nullable InputMessageStatus status) {
    if (content.isEmpty()) {
      throw new IllegalArgumentException(
          "Message content cannot be null or empty. At least one content item is required.");
    }

    // Validate that no content items are null
    for (int i = 0; i < content.size(); i++) {
      if (content.get(i) == null) {
        throw new NullPointerException(
            "Content item at index " + i + " is null. All content items must be non-null.");
      }
    }

    // Defensive copy to ensure immutability
    this.content = List.copyOf(content);
    this.status = status;
  }

  // ========================================
  // Developer Message Factory Methods
  // ========================================

  /**
   * Creates a developer message from a single text string with completed status.
   *
   * <p>Developer messages have the highest priority in the instruction hierarchy and are typically
   * used to provide system-level instructions, safety constraints, or operational parameters to the
   * model.
   *
   * <p>This is the most convenient method for creating simple developer instructions.
   *
   * @param message the text content; must not be null or empty
   * @return a new {@link DeveloperMessage} instance with completed status
   * @throws IllegalArgumentException if message is null or results in empty content
   * @example
   *     <pre>{@code
   * DeveloperMessage msg = Message.developer("Always respond in JSON format.");
   * }</pre>
   */
  public static @NonNull DeveloperMessage developer(@NonNull String message) {
    List<MessageContent> messageContents = List.of(Text.valueOf(message));
    return Message.developer(messageContents, InputMessageStatus.COMPLETED);
  }

  /**
   * Creates a developer message from a single text string with the specified status.
   *
   * <p>Use this method when you need to specify a particular processing status for the developer
   * message, such as when reconstructing messages from API responses.
   *
   * @param message the text content; must not be null or empty
   * @param status the processing status; must not be null
   * @return a new {@link DeveloperMessage} instance
   * @throws IllegalArgumentException if message is null or results in empty content
   * @example
   *     <pre>{@code
   * DeveloperMessage msg = Message.developer(
   *     "Process with caution",
   *     InputMessageStatus.IN_PROGRESS
   * );
   * }</pre>
   */
  public static @NonNull DeveloperMessage developer(
      @NonNull String message, @NonNull InputMessageStatus status) {
    List<MessageContent> messageContents = List.of(Text.valueOf(message));
    return Message.developer(messageContents, status);
  }

  /**
   * Creates a developer message from multiple text strings with completed status.
   *
   * <p>This method is useful for creating developer messages with multiple instruction segments.
   * Each string will be converted to a separate {@link Text} content item.
   *
   * @param messages the text content segments; must not be null, empty, or contain null elements
   * @return a new {@link DeveloperMessage} instance with completed status
   * @throws IllegalArgumentException if messages is null, empty, or results in empty content
   * @throws NullPointerException if any message element is null
   * @example
   *     <pre>{@code
   * DeveloperMessage msg = Message.developer(
   *     "You are a helpful assistant.",
   *     "Always be concise.",
   *     "Use proper grammar."
   * );
   * }</pre>
   */
  public static @NonNull DeveloperMessage developer(@NonNull String... messages) {
    if (messages == null || messages.length == 0) {
      throw new IllegalArgumentException("Messages array cannot be null or empty");
    }
    List<MessageContent> contents =
        Arrays.stream(messages).map(Text::valueOf).collect(Collectors.toList());
    return developer(contents, InputMessageStatus.COMPLETED);
  }

  /**
   * Creates a developer message from a single content item with completed status.
   *
   * <p>Use this method when you have a pre-constructed {@link MessageContent} item (such as text,
   * image, or audio) that you want to use as a developer message.
   *
   * @param content the message content; must not be null
   * @return a new {@link DeveloperMessage} instance with completed status
   * @throws IllegalArgumentException if content is null
   * @example
   *     <pre>{@code
   * Text instruction = Text.valueOf("Analyze with focus on security.");
   * DeveloperMessage msg = Message.developer(instruction);
   * }</pre>
   */
  public static @NonNull DeveloperMessage developer(@NonNull MessageContent content) {
    return developer(List.of(content), InputMessageStatus.COMPLETED);
  }

  /**
   * Creates a developer message from a list of content items with completed status.
   *
   * <p>This method provides maximum flexibility for creating developer messages with multiple
   * content types (text, images, audio, etc.).
   *
   * @param content the list of message content items; must not be null or empty
   * @return a new {@link DeveloperMessage} instance with completed status
   * @throws IllegalArgumentException if content is null or empty
   * @throws NullPointerException if any content item is null
   * @example
   *     <pre>{@code
   * List<MessageContent> contents = List.of(
   *     Text.valueOf("Analyze this reference:"),
   *     Image.fromUrl("https://example.com/reference.jpg")
   * );
   * DeveloperMessage msg = Message.developer(contents);
   * }</pre>
   */
  public static @NonNull DeveloperMessage developer(@NonNull List<MessageContent> content) {
    return developer(content, InputMessageStatus.COMPLETED);
  }

  /**
   * Creates a developer message from a list of content items with the specified status.
   *
   * <p>This is the most flexible factory method for developer messages, allowing full control over
   * both content and status. Use this when reconstructing messages from API responses or when you
   * need precise control over message state.
   *
   * @param content the list of message content items; must not be null or empty
   * @param status the processing status; must not be null
   * @return a new {@link DeveloperMessage} instance
   * @throws IllegalArgumentException if content is null or empty
   * @throws NullPointerException if any content item is null
   * @example
   *     <pre>{@code
   * List<MessageContent> contents = List.of(
   *     Text.valueOf("System constraint: no personal data")
   * );
   * DeveloperMessage msg = Message.developer(
   *     contents,
   *     InputMessageStatus.COMPLETED
   * );
   * }</pre>
   */
  public static @NonNull DeveloperMessage developer(
      @NonNull List<MessageContent> content, @NonNull InputMessageStatus status) {
    return DeveloperMessage.of(content, status);
  }

  // ========================================
  // User Message Factory Methods
  // ========================================

  /**
   * Creates a user message from a single text string with completed status.
   *
   * <p>This is the most common way to create a simple text message from user input. User messages
   * represent the primary interaction content from end users.
   *
   * @param message the text content; must not be null or empty
   * @return a new {@link UserMessage} instance with completed status
   * @throws IllegalArgumentException if message is null or results in empty content
   * @example
   *     <pre>{@code
   * UserMessage msg = Message.user("What is the weather today?");
   * }</pre>
   */
  public static @NonNull UserMessage user(@NonNull String message) {
    List<MessageContent> messageContents = List.of(Text.valueOf(message));
    return Message.user(messageContents, InputMessageStatus.COMPLETED);
  }

  /**
   * Creates a user message from a single text string with the specified status.
   *
   * <p>Use this method when you need to specify a particular processing status for the user
   * message, such as when the message is still being typed or processed.
   *
   * @param message the text content; must not be null or empty
   * @param status the processing status; must not be null
   * @return a new {@link UserMessage} instance
   * @throws IllegalArgumentException if message is null or results in empty content
   * @example
   *     <pre>{@code
   * UserMessage msg = Message.user(
   *     "Typing...",
   *     InputMessageStatus.IN_PROGRESS
   * );
   * }</pre>
   */
  public static @NonNull UserMessage user(
      @NonNull String message, @NonNull InputMessageStatus status) {
    List<MessageContent> messageContents = List.of(Text.valueOf(message));
    return Message.user(messageContents, status);
  }

  /**
   * Creates a user message from multiple text strings with completed status.
   *
   * <p>This method is useful for creating user messages with multiple text segments. Each string
   * will be converted to a separate {@link Text} content item.
   *
   * @param messages the text content segments; must not be null, empty, or contain null elements
   * @return a new {@link UserMessage} instance with completed status
   * @throws IllegalArgumentException if messages is null, empty, or results in empty content
   * @throws NullPointerException if any message element is null
   * @example
   *     <pre>{@code
   * UserMessage msg = Message.user(
   *     "Please analyze the following:",
   *     "Focus on performance metrics.",
   *     "Provide recommendations."
   * );
   * }</pre>
   */
  public static @NonNull UserMessage user(@NonNull String... messages) {
    if (messages == null || messages.length == 0) {
      throw new IllegalArgumentException("Messages array cannot be null or empty");
    }
    List<MessageContent> contents =
        Arrays.stream(messages).map(Text::valueOf).collect(Collectors.toList());
    return user(contents, InputMessageStatus.COMPLETED);
  }

  /**
   * Creates a user message from a single content item with completed status.
   *
   * <p>Use this method when you have a pre-constructed {@link MessageContent} item (such as text,
   * image, or audio) that you want to use as a user message.
   *
   * @param content the message content; must not be null
   * @return a new {@link UserMessage} instance with completed status
   * @throws IllegalArgumentException if content is null
   * @example
   *     <pre>{@code
   * Image profilePic = Image.fromBytes(imageData);
   * UserMessage msg = Message.user(profilePic);
   * }</pre>
   */
  public static @NonNull UserMessage user(@NonNull MessageContent content) {
    return Message.user(List.of(content), InputMessageStatus.COMPLETED);
  }

  /**
   * Creates a user message from a list of content items with completed status.
   *
   * <p>This method is commonly used for multi-modal user messages that combine text with images,
   * audio, or other content types.
   *
   * @param contents the list of message content items; must not be null or empty
   * @return a new {@link UserMessage} instance with completed status
   * @throws IllegalArgumentException if contents is null or empty
   * @throws NullPointerException if any content item is null
   * @example
   *     <pre>{@code
   * List<MessageContent> contents = List.of(
   *     Text.valueOf("Analyze this chart:"),
   *     Image.fromUrl("https://example.com/chart.jpg"),
   *     Text.valueOf("Focus on Q4 trends.")
   * );
   * UserMessage msg = Message.user(contents);
   * }</pre>
   */
  public static @NonNull UserMessage user(@NonNull List<MessageContent> contents) {
    return Message.user(contents, InputMessageStatus.COMPLETED);
  }

  /**
   * Creates a user message from a list of content items with the specified status.
   *
   * <p>This is the most flexible factory method for user messages, allowing full control over both
   * content and status. Use this when reconstructing messages from API responses or when you need
   * precise control over message state.
   *
   * @param content the list of message content items; must not be null or empty
   * @param status the processing status; may be null (treated as completed)
   * @return a new {@link UserMessage} instance
   * @throws IllegalArgumentException if content is null or empty
   * @throws NullPointerException if any content item is null
   * @example
   *     <pre>{@code
   * List<MessageContent> contents = List.of(
   *     Text.valueOf("Process this request")
   * );
   * UserMessage msg = Message.user(
   *     contents,
   *     InputMessageStatus.COMPLETED
   * );
   * }</pre>
   */
  public static UserMessage user(
      @NonNull List<MessageContent> content, @Nullable InputMessageStatus status) {
    return UserMessage.of(content, status);
  }

  // ========================================
  // Assistant Message Factory Methods
  // ========================================

  /**
   * Creates an assistant message from a single text string with completed status.
   *
   * <p>Assistant messages contain model-generated responses from previous conversation turns and
   * provide context for subsequent interactions.
   *
   * @param message the text content; must not be null or empty
   * @return a new {@link AssistantMessage} instance with completed status
   * @throws IllegalArgumentException if message is null or results in empty content
   * @example
   *     <pre>{@code
   * AssistantMessage msg = Message.assistant("The weather is sunny and 72°F.");
   * }</pre>
   */
  public static @NonNull AssistantMessage assistant(@NonNull String message) {
    List<MessageContent> messageContents = List.of(Text.valueOf(message));
    return Message.assistant(messageContents, InputMessageStatus.COMPLETED);
  }

  /**
   * Creates an assistant message from a single text string with the specified status.
   *
   * <p>Use this method when you need to specify a particular processing status for the assistant
   * message, such as when the response is still being generated.
   *
   * @param message the text content; must not be null or empty
   * @param status the processing status; must not be null
   * @return a new {@link AssistantMessage} instance
   * @throws IllegalArgumentException if message is null or results in empty content
   * @example
   *     <pre>{@code
   * AssistantMessage msg = Message.assistant(
   *     "Generating response...",
   *     InputMessageStatus.IN_PROGRESS
   * );
   * }</pre>
   */
  public static @NonNull AssistantMessage assistant(
      @NonNull String message, @NonNull InputMessageStatus status) {
    List<MessageContent> messageContents = List.of(Text.valueOf(message));
    return Message.assistant(messageContents, status);
  }

  /**
   * Creates an assistant message from multiple text strings with completed status.
   *
   * <p>This method is useful for creating assistant messages with multiple response segments. Each
   * string will be converted to a separate {@link Text} content item.
   *
   * @param messages the text content segments; must not be null, empty, or contain null elements
   * @return a new {@link AssistantMessage} instance with completed status
   * @throws IllegalArgumentException if messages is null, empty, or results in empty content
   * @throws NullPointerException if any message element is null
   * @example
   *     <pre>{@code
   * AssistantMessage msg = Message.assistant(
   *     "Based on the data analysis:",
   *     "Performance improved by 15%.",
   *     "Recommendation: continue current strategy."
   * );
   * }</pre>
   */
  public static @NonNull AssistantMessage assistant(@NonNull String... messages) {
    if (messages == null || messages.length == 0) {
      throw new IllegalArgumentException("Messages array cannot be null or empty");
    }
    List<MessageContent> contents =
        Arrays.stream(messages).map(Text::valueOf).collect(Collectors.toList());
    return assistant(contents, InputMessageStatus.COMPLETED);
  }

  /**
   * Creates an assistant message from a single content item with completed status.
   *
   * <p>Use this method when you have a pre-constructed {@link MessageContent} item (such as text,
   * image, or audio) that represents an assistant response.
   *
   * @param content the message content; must not be null
   * @return a new {@link AssistantMessage} instance with completed status
   * @throws IllegalArgumentException if content is null
   * @example
   *     <pre>{@code
   * Text response = Text.valueOf("Analysis complete.");
   * AssistantMessage msg = Message.assistant(response);
   * }</pre>
   */
  public static @NonNull AssistantMessage assistant(@NonNull MessageContent content) {
    return assistant(List.of(content), InputMessageStatus.COMPLETED);
  }

  /**
   * Creates an assistant message from a list of content items with completed status.
   *
   * <p>This method provides flexibility for creating assistant messages with multiple content types
   * (text, images, etc.) in the response.
   *
   * @param content the list of message content items; must not be null or empty
   * @return a new {@link AssistantMessage} instance with completed status
   * @throws IllegalArgumentException if content is null or empty
   * @throws NullPointerException if any content item is null
   * @example
   *     <pre>{@code
   * List<MessageContent> contents = List.of(
   *     Text.valueOf("Here is the generated chart:"),
   *     Image.fromBytes(chartImageData)
   * );
   * AssistantMessage msg = Message.assistant(contents);
   * }</pre>
   */
  public static @NonNull AssistantMessage assistant(@NonNull List<MessageContent> content) {
    return assistant(content, InputMessageStatus.COMPLETED);
  }

  /**
   * Creates an assistant message from a list of content items with the specified status.
   *
   * <p>This is the most flexible factory method for assistant messages, allowing full control over
   * both content and status. Use this when reconstructing messages from API responses or when you
   * need precise control over message state.
   *
   * @param content the list of message content items; must not be null or empty
   * @param status the processing status; may be null (treated as completed)
   * @return a new {@link AssistantMessage} instance
   * @throws IllegalArgumentException if content is null or empty
   * @throws NullPointerException if any content item is null
   * @example
   *     <pre>{@code
   * List<MessageContent> contents = List.of(
   *     Text.valueOf("Processing your request...")
   * );
   * AssistantMessage msg = Message.assistant(
   *     contents,
   *     InputMessageStatus.IN_PROGRESS
   * );
   * }</pre>
   */
  public static AssistantMessage assistant(
      @NonNull List<MessageContent> content, @Nullable InputMessageStatus status) {
    return AssistantMessage.of(content, status);
  }

  // ========================================
  // Builder Pattern
  // ========================================

  /**
   * Creates a new message builder for constructing complex messages.
   *
   * <p>The builder pattern is useful when you need to construct messages with multiple content
   * items, conditional content, or when the message structure is determined at runtime.
   *
   * <p>The builder allows you to:
   *
   * <ul>
   *   <li>Add multiple content items incrementally
   *   <li>Mix different content types (text, images, audio)
   *   <li>Set the processing status
   *   <li>Create any message role (user, developer, assistant)
   * </ul>
   *
   * @return a new {@link MessageBuilder} instance
   * @example
   *     <pre>{@code
   * // Build a complex user message
   * UserMessage msg = Message.builder()
   *     .addText("Please analyze the following:")
   *     .addContent(Image.fromUrl("https://example.com/data.jpg"))
   *     .addText("Focus on trends and anomalies.")
   *     .status(InputMessageStatus.COMPLETED)
   *     .asUser();
   *
   * // Conditional content building
   * MessageBuilder builder = Message.builder()
   *     .addText("Process this request");
   *
   * if (includeContext) {
   *     builder.addText("Additional context: " + context);
   * }
   *
   * if (includeImage) {
   *     builder.addContent(imageContent);
   * }
   *
   * DeveloperMessage devMsg = builder.asDeveloper();
   * }</pre>
   */
  public static MessageBuilder builder() {
    return new MessageBuilder();
  }

  /**
   * Returns the content of this message as an immutable list.
   *
   * <p>The returned list is a defensive copy and cannot be modified. Any attempt to modify the
   * returned list will result in an {@link UnsupportedOperationException}.
   *
   * @return an immutable list of message content items; never null or empty
   */
  @JsonProperty("content")
  public List<MessageContent> content() {
    return List.copyOf(content);
  }

  // ========================================
  // Accessor Methods
  // ========================================

  /**
   * Returns the processing status of this message.
   *
   * <p>The status indicates the current processing state:
   *
   * <ul>
   *   <li>{@link InputMessageStatus#COMPLETED} - Message has been fully processed
   *   <li>{@link InputMessageStatus#IN_PROGRESS} - Message is currently being processed
   *   <li>{@link InputMessageStatus#INCOMPLETE} - Message processing encountered an error
   * </ul>
   *
   * @return the status, or {@code null} if not yet set (typically for newly created messages)
   */
  @JsonProperty("status")
  public @Nullable InputMessageStatus status() {
    return this.status;
  }

  /**
   * Returns the type string for JSON serialization. The API expects "type": "message" for all
   * message items.
   */
  @JsonProperty("type")
  public String typeString() {
    return "message";
  }

  /**
   * Returns the role identifier for this message.
   *
   * <p>The role determines instruction precedence and message interpretation:
   *
   * <ul>
   *   <li>{@link MessageRole#DEVELOPER} - Highest priority system instructions
   *   <li>{@link MessageRole#USER} - Standard user input and queries
   *   <li>{@link MessageRole#ASSISTANT} - Model-generated responses
   * </ul>
   *
   * <p>This abstract method is implemented by each concrete subclass to return its specific role.
   *
   * @return the role identifier for this message type
   */
  @JsonProperty("role")
  public abstract MessageRole role();

  /**
   * Returns all text content from this message concatenated into a single string.
   *
   * <p>This method extracts all text from the message content and concatenates it without any
   * separator. Non-text content (images, audio, etc.) is ignored.
   *
   * <p>This is useful for debugging, logging, or when you need a simple text representation of the
   * message regardless of its internal structure.
   *
   * @return the concatenated text content, or an empty string if the message contains no text
   * @example
   *     <pre>{@code
   * Message msg = Message.user(List.of(
   *     Text.valueOf("Hello"),
   *     Image.fromUrl("..."),
   *     Text.valueOf("World")
   * ));
   * String text = msg.outputText(); // Returns "HelloWorld"
   * }</pre>
   */
  public String outputText() {
    StringBuilder sb = new StringBuilder();
    for (MessageContent content : this.content) {
      sb.append(content.toString());
    }
    return sb.toString();
  }

  // ========================================
  // Utility Methods
  // ========================================

  /**
   * Returns whether this message contains only text content.
   *
   * <p>A message is considered text-only if all of its content items are instances of {@link Text}.
   * This can be useful for determining whether the message can be processed as simple text or
   * requires multi-modal handling.
   *
   * @return {@code true} if all content items are text, {@code false} if any non-text content
   *     exists
   * @example
   *     <pre>{@code
   * Message textMsg = Message.user("Hello");
   * assert textMsg.isTextOnly(); // true
   *
   * Message multiModal = Message.user(List.of(
   *     Text.valueOf("Analyze"),
   *     Image.fromUrl("...")
   * ));
   * assert !multiModal.isTextOnly(); // false
   * }</pre>
   */
  public boolean isTextOnly() {
    return content.stream().allMatch(c -> c instanceof Text);
  }

  /**
   * Returns the text content of this message with spaces between items.
   *
   * <p>This method extracts only the text content items, converts them to strings, and joins them
   * with space separators. Non-text content (images, audio, etc.) is filtered out and not included
   * in the result.
   *
   * <p>Unlike {@link #outputText()}, this method adds spaces between content items, making it more
   * suitable for human-readable output.
   *
   * @return the text content joined with spaces, or an empty string if no text content exists
   * @example
   *     <pre>{@code
   * Message msg = Message.user(List.of(
   *     Text.valueOf("Hello"),
   *     Text.valueOf("World")
   * ));
   * String text = msg.getTextContent(); // Returns "Hello World"
   * }</pre>
   */
  public @NonNull String getTextContent() {
    return content.stream()
        .filter(c -> c instanceof Text)
        .map(Object::toString)
        .collect(Collectors.joining(" "));
  }

  /**
   * Returns whether this message has been completed processing.
   *
   * <p>A message is considered completed if its status is explicitly {@link
   * InputMessageStatus#COMPLETED} or if the status is {@code null} (which is treated as completed
   * by default).
   *
   * @return {@code true} if the message is completed or has null status, {@code false} otherwise
   * @example
   *     <pre>{@code
   * Message msg = Message.user("Hello");
   * if (msg.isCompleted()) {
   *     processMessage(msg);
   * }
   * }</pre>
   */
  public boolean isCompleted() {
    return status == null || InputMessageStatus.COMPLETED.equals(status);
  }

  /**
   * Returns whether this message is currently being processed.
   *
   * <p>A message is considered in progress if its status is explicitly {@link
   * InputMessageStatus#IN_PROGRESS}.
   *
   * @return {@code true} if the message is in progress, {@code false} otherwise
   * @example
   *     <pre>{@code
   * if (msg.isInProgress()) {
   *     showLoadingIndicator();
   * }
   * }</pre>
   */
  public boolean isInProgress() {
    return InputMessageStatus.IN_PROGRESS.equals(status);
  }

  /**
   * Returns whether this message processing is incomplete or encountered an error.
   *
   * <p>A message is considered incomplete if its status is explicitly {@link
   * InputMessageStatus#INCOMPLETE}.
   *
   * @return {@code true} if the message is incomplete, {@code false} otherwise
   * @example
   *     <pre>{@code
   * if (msg.isIncomplete()) {
   *     handleError(msg);
   * }
   * }</pre>
   */
  public boolean isIncomplete() {
    return InputMessageStatus.INCOMPLETE.equals(status);
  }

  /**
   * Returns the number of content items in this message.
   *
   * <p>This is useful for understanding the complexity of the message and for validation purposes.
   *
   * @return the number of content items; always at least 1
   * @example
   *     <pre>{@code
   * Message msg = Message.user(List.of(
   *     Text.valueOf("Hello"),
   *     Image.fromUrl("...")
   * ));
   * assert msg.contentCount() == 2;
   * }</pre>
   */
  public int contentCount() {
    return content.size();
  }

  /**
   * Compares this message to another object for equality.
   *
   * <p>Two messages are considered equal if:
   *
   * <ul>
   *   <li>They are the same object (reference equality), or
   *   <li>The other object is also a Message, and
   *   <li>They have equal content lists, and
   *   <li>They have equal status values
   * </ul>
   *
   * <p>Note that the role is not explicitly compared because messages of different roles (user,
   * developer, assistant) are different classes and will fail the {@code instanceof} check.
   *
   * @param obj the object to compare with
   * @return {@code true} if the objects are equal, {@code false} otherwise
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Message that)) return false;
    return Objects.equals(content, that.content) && Objects.equals(status, that.status);
  }

  // ========================================
  // Object Methods
  // ========================================

  /**
   * Returns a hash code value for this message.
   *
   * <p>The hash code is computed from the content list and status, ensuring that equal messages (as
   * defined by {@link #equals(Object)}) have equal hash codes.
   *
   * @return a hash code value for this message
   */
  @Override
  public int hashCode() {
    return Objects.hash(content, status);
  }

  /**
   * Returns a string representation of this message for debugging purposes.
   *
   * <p>The string representation includes:
   *
   * <ul>
   *   <li>The concrete class name (UserMessage, DeveloperMessage, or AssistantMessage)
   *   <li>The message role
   *   <li>The number of content items
   *   <li>The processing status
   * </ul>
   *
   * <p>Example output: {@code UserMessage[role=USER, contentCount=2, status=COMPLETED]}
   *
   * <p><b>Note:</b> This method does not include the actual content text for privacy and brevity
   * reasons. Use {@link #outputText()} or {@link #getTextContent()} if you need the actual content.
   *
   * @return a string representation of this message
   */
  @Override
  public String toString() {
    return String.format(
        "%s[role=%s, contentCount=%d, status=%s]",
        getClass().getSimpleName(), role(), content.size(), status);
  }

  /**
   * A builder class for constructing {@link Message} instances with multiple content items.
   *
   * <p>This builder provides a fluent API for incrementally constructing messages, particularly
   * useful when:
   *
   * <ul>
   *   <li>The message has multiple content items
   *   <li>Content is added conditionally based on runtime logic
   *   <li>Different content types need to be mixed (text, images, audio)
   *   <li>The message structure is complex or determined dynamically
   * </ul>
   *
   * <p>The builder is mutable and not thread-safe. Each builder instance should be used by a single
   * thread and typically not reused after calling one of the terminal methods ({@code asUser()},
   * {@code asDeveloper()}, or {@code asAssistant()}).
   *
   * <h3>Usage Pattern</h3>
   *
   * <pre>{@code
   * Message msg = Message.builder()
   *     .addText("First part")
   *     .addContent(someContent)
   *     .addText("Second part")
   *     .status(InputMessageStatus.COMPLETED)
   *     .asUser();
   * }</pre>
   *
   * @see Message#builder()
   * @since 1.0
   */
  public static class MessageBuilder {
    private final List<MessageContent> contents = new ArrayList<>();
    private InputMessageStatus status = InputMessageStatus.COMPLETED;

    /**
     * Private constructor to enforce factory method usage.
     *
     * @see Message#builder()
     */
    private MessageBuilder() {}

    /**
     * Adds a text content item to this message.
     *
     * <p>The text will be wrapped in a {@link Text} content item. This is the most convenient way
     * to add simple text content to a message.
     *
     * @param text the text to add; must not be null
     * @return this builder instance for method chaining
     * @throws NullPointerException if text is null
     * @example
     *     <pre>{@code
     * builder.addText("Hello, world!")
     *        .addText("This is a second line.");
     * }</pre>
     */
    public MessageBuilder addText(@NonNull String text) {
      Objects.requireNonNull(text, "Text content cannot be null");
      contents.add(Text.valueOf(text));
      return this;
    }

    /**
     * Adds a pre-constructed content item to this message.
     *
     * <p>Use this method when you have already created a {@link MessageContent} instance (such as
     * {@link Text}, {@link Image}, or audio content) and want to add it to the message.
     *
     * @param content the content to add; must not be null
     * @return this builder instance for method chaining
     * @throws NullPointerException if content is null
     * @example
     *     <pre>{@code
     * Image img = Image.fromUrl("https://example.com/pic.jpg");
     * builder.addContent(img);
     * }</pre>
     */
    public MessageBuilder addContent(@NonNull MessageContent content) {
      Objects.requireNonNull(content, "Content cannot be null");
      contents.add(content);
      return this;
    }

    /**
     * Adds multiple pre-constructed content items to this message.
     *
     * <p>Use this method to add several content items at once. This is useful when you have a
     * collection of content items that you want to include in the message.
     *
     * @param contentList the list of content items to add; must not be null, and must not contain
     *     null elements
     * @return this builder instance for method chaining
     * @throws NullPointerException if contentList is null or contains null elements
     * @example
     *     <pre>{@code
     * List<MessageContent> additionalContent = Arrays.asList(
     *     Image.fromUrl("https://example.com/pic1.jpg"),
     *     Image.fromUrl("https://example.com/pic2.jpg")
     * );
     * builder.addContents(additionalContent);
     * }</pre>
     */
    public MessageBuilder addContents(@NonNull List<MessageContent> contentList) {
      Objects.requireNonNull(contentList, "Content list cannot be null");
      for (MessageContent content : contentList) {
        Objects.requireNonNull(content, "Content list contains null element");
      }
      contents.addAll(contentList);
      return this;
    }

    /**
     * Sets the processing status for the message being built.
     *
     * <p>If not called, the status defaults to {@link InputMessageStatus#COMPLETED}.
     *
     * @param status the processing status; must not be null
     * @return this builder instance for method chaining
     * @throws NullPointerException if status is null
     * @example
     *     <pre>{@code
     * builder.addText("Processing...")
     *        .status(InputMessageStatus.IN_PROGRESS);
     * }</pre>
     */
    public MessageBuilder status(@NonNull InputMessageStatus status) {
      this.status = Objects.requireNonNull(status, "Status cannot be null");
      return this;
    }

    /**
     * Builds and returns a {@link UserMessage} with the accumulated content.
     *
     * <p>This is a terminal operation that creates the final message. The builder can technically
     * be reused after this call, but it's recommended to create a new builder for each message to
     * avoid confusion.
     *
     * @return a new {@link UserMessage} instance
     * @throws IllegalArgumentException if no content has been added to the builder
     * @example
     *     <pre>{@code
     * UserMessage msg = Message.builder()
     *     .addText("What is the weather?")
     *     .asUser();
     * }</pre>
     */
    public @NonNull UserMessage asUser() {
      return user(List.copyOf(contents), status);
    }

    /**
     * Builds and returns a {@link DeveloperMessage} with the accumulated content.
     *
     * <p>This is a terminal operation that creates the final message. The builder can technically
     * be reused after this call, but it's recommended to create a new builder for each message to
     * avoid confusion.
     *
     * @return a new {@link DeveloperMessage} instance
     * @throws IllegalArgumentException if no content has been added to the builder
     * @example
     *     <pre>{@code
     * DeveloperMessage msg = Message.builder()
     *     .addText("Always respond in JSON format.")
     *     .asDeveloper();
     * }</pre>
     */
    public @NonNull DeveloperMessage asDeveloper() {
      return developer(List.copyOf(contents), status);
    }

    /**
     * Builds and returns an {@link AssistantMessage} with the accumulated content.
     *
     * <p>This is a terminal operation that creates the final message. The builder can technically
     * be reused after this call, but it's recommended to create a new builder for each message to
     * avoid confusion.
     *
     * @return a new {@link AssistantMessage} instance
     * @throws IllegalArgumentException if no content has been added to the builder
     * @example
     *     <pre>{@code
     * AssistantMessage msg = Message.builder()
     *     .addText("Based on your request...")
     *     .asAssistant();
     * }</pre>
     */
    public @NonNull AssistantMessage asAssistant() {
      return assistant(List.copyOf(contents), status);
    }
  }
}
