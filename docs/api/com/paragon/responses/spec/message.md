# :material-code-braces: Message

> This docs was updated at: 2026-02-23

`com.paragon.responses.spec.Message` &nbsp;·&nbsp; **Class**

Implements `ResponseInputItem`, `Item`

---

Represents a message input to an AI model with role-based instruction hierarchy.

Messages form the conversation context for model interactions, with roles determining
instruction precedence: `developer` and `system` roles override `user` role
instructions. The `assistant` role contains model-generated responses from previous
interactions.

This is a sealed abstract class with three permitted implementations:

  
- `DeveloperMessage` - Highest priority instructions from developers that guide model
      behavior and establish constraints
- `UserMessage` - Instructions and queries from end users representing the primary
      interaction content
- `AssistantMessage` - Model-generated responses from prior conversation turns that
      provide context

### Role Hierarchy and Precedence

The message role determines how the AI model interprets and prioritizes instructions:
- **Developer Role:** Highest priority - establishes system behavior, safety constraints,
      and operational parameters
- **User Role:** Standard priority - contains user queries, instructions, and interaction
      content
- **Assistant Role:** Context only - provides conversation history and previous model
      responses

### Usage Examples

#### Simple Text Messages

```java
// Create a basic user message
UserMessage userMsg = Message.user("What is the weather today?");
// Create a developer instruction
DeveloperMessage devMsg = Message.developer("Always respond in JSON format.");
// Create an assistant response
AssistantMessage assistantMsg = Message.assistant("The weather is sunny and 72°F.");
```

#### Multi-Content Messages

```java
// Message with text and image
UserMessage multiContent = Message.user(List.of(
    Text.valueOf("Analyze this image:"),
    Image.fromUrl("https://example.com/chart.jpg")
));
// Multiple text segments
DeveloperMessage instructions = Message.developer(
    "You are a helpful assistant.",
    "Always be concise.",
    "Use proper grammar."
);
```

#### Using the Builder Pattern

```java
// Complex message construction
UserMessage complexMsg = Message.builder()
    .addText("Please analyze the following:")
    .addContent(Image.fromBytes(imageData))
    .addText("Focus on color distribution.")
    .status(InputMessageStatus.COMPLETED)
    .asUser();
// Conditional content building
MessageBuilder builder = Message.builder()
    .addText("Process this data:");
if (includeImage) {
    builder.addContent(imageContent);
}
UserMessage msg = builder.asUser();
```

#### Working with Status

```java
// Create message with specific status
UserMessage inProgress = Message.user("Processing...", InputMessageStatus.IN_PROGRESS);
// Check message status
if (message.isCompleted()) {
    processMessage(message);
}
```

### Thread Safety

All Message instances are **immutable** and therefore **thread-safe**. The content list
is defensively copied during construction and returned as an unmodifiable copy, preventing any
external modification.

### JSON Serialization

Messages are serialized using Jackson with polymorphic type handling:
- The `type` property (from `ResponseInputItem`) identifies this as a message
- The `role` property distinguishes between concrete implementations
- Deserialization automatically creates the correct subclass based on the role value

Example JSON representation:

```java
{
  "type": "message",
  "role": "user",
  "content": [
    {
      "type": "text",
      "text": "Hello, world!"
    }
  ],
  "status": "completed"
}
```

### Validation

The class enforces the following invariants:
- Content list must not be null
- Content list must not be empty
- Individual content items must not be null
- Status may be null for unprocessed messages

**See Also**

- `DeveloperMessage`
- `UserMessage`
- `AssistantMessage`
- `MessageContent`
- `InputMessageStatus`
- `ResponseInputItem`

*Since: 1.0*

## Fields

### `content`

```java
private final @NonNull List<MessageContent> content
```

The message content provided to the model.

Contains text, image, audio, or other input types used for response generation, or previous
assistant responses that provide conversation context. The content list may contain multiple
items of various types (text, images, audio) in a single message, enabling rich multi-modal
interactions.

This field is immutable and guaranteed to be non-null and non-empty. The list is defensively
copied during construction to ensure immutability.

**See Also**

- `MessageContent`
- `Text`
- `Image`

---

### `status`

```java
private final @Nullable InputMessageStatus status
```

The processing status of this message item.

Indicates whether the message is currently being processed (`InputMessageStatus.IN_PROGRESS`), has been fully processed (`InputMessageStatus.COMPLETED`), or encountered an error (`InputMessageStatus.INCOMPLETE`).

This field is populated when items are returned from the API and may be `null` for
newly created messages that haven't been processed yet. A `null` status should be treated
as equivalent to `InputMessageStatus.COMPLETED` for message construction purposes.

**See Also**

- `InputMessageStatus`


## Methods

### `Message`

```java
protected Message(@NonNull List<MessageContent> content, @Nullable InputMessageStatus status)
```

Constructs a Message with the specified content and status.

This constructor performs defensive copying of the content list to ensure immutability and
validates that the content is not null or empty.

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the message content to be processed by the model; must not be null or empty |
| `status` | the processing status of the message; may be null for unprocessed messages |

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if content is null or empty |
| `NullPointerException` | if any element in the content list is null |

---

### `developer`

```java
public static @NonNull DeveloperMessage developer(@NonNull String message)
```

Creates a developer message from a single text string with completed status.

Developer messages have the highest priority in the instruction hierarchy and are typically
used to provide system-level instructions, safety constraints, or operational parameters to the
model.

This is the most convenient method for creating simple developer instructions.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the text content; must not be null or empty |

**Returns**

a new `DeveloperMessage` instance with completed status

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if message is null or results in empty content |

---

### `developer`

```java
public static @NonNull DeveloperMessage developer(
      @NonNull String message, @NonNull InputMessageStatus status)
```

Creates a developer message from a single text string with the specified status.

Use this method when you need to specify a particular processing status for the developer
message, such as when reconstructing messages from API responses.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the text content; must not be null or empty |
| `status` | the processing status; must not be null |

**Returns**

a new `DeveloperMessage` instance

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if message is null or results in empty content |

---

### `developer`

```java
public static @NonNull DeveloperMessage developer(@NonNull String... messages)
```

Creates a developer message from multiple text strings with completed status.

This method is useful for creating developer messages with multiple instruction segments.
Each string will be converted to a separate `Text` content item.

**Parameters**

| Name | Description |
|------|-------------|
| `messages` | the text content segments; must not be null, empty, or contain null elements |

**Returns**

a new `DeveloperMessage` instance with completed status

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if messages is null, empty, or results in empty content |
| `NullPointerException` | if any message element is null |

---

### `developer`

```java
public static @NonNull DeveloperMessage developer(@NonNull MessageContent content)
```

Creates a developer message from a single content item with completed status.

Use this method when you have a pre-constructed `MessageContent` item (such as text,
image, or audio) that you want to use as a developer message.

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the message content; must not be null |

**Returns**

a new `DeveloperMessage` instance with completed status

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if content is null |

---

### `developer`

```java
public static @NonNull DeveloperMessage developer(@NonNull List<MessageContent> content)
```

Creates a developer message from a list of content items with completed status.

This method provides maximum flexibility for creating developer messages with multiple
content types (text, images, audio, etc.).

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the list of message content items; must not be null or empty |

**Returns**

a new `DeveloperMessage` instance with completed status

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if content is null or empty |
| `NullPointerException` | if any content item is null |

---

### `developer`

```java
public static @NonNull DeveloperMessage developer(
      @NonNull List<MessageContent> content, @NonNull InputMessageStatus status)
```

Creates a developer message from a list of content items with the specified status.

This is the most flexible factory method for developer messages, allowing full control over
both content and status. Use this when reconstructing messages from API responses or when you
need precise control over message state.

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the list of message content items; must not be null or empty |
| `status` | the processing status; must not be null |

**Returns**

a new `DeveloperMessage` instance

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if content is null or empty |
| `NullPointerException` | if any content item is null |

---

### `user`

```java
public static @NonNull UserMessage user(@NonNull String message)
```

Creates a user message from a single text string with completed status.

This is the most common way to create a simple text message from user input. User messages
represent the primary interaction content from end users.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the text content; must not be null or empty |

**Returns**

a new `UserMessage` instance with completed status

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if message is null or results in empty content |

---

### `user`

```java
public static @NonNull UserMessage user(
      @NonNull String message, @NonNull InputMessageStatus status)
```

Creates a user message from a single text string with the specified status.

Use this method when you need to specify a particular processing status for the user
message, such as when the message is still being typed or processed.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the text content; must not be null or empty |
| `status` | the processing status; must not be null |

**Returns**

a new `UserMessage` instance

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if message is null or results in empty content |

---

### `user`

```java
public static @NonNull UserMessage user(@NonNull String... messages)
```

Creates a user message from multiple text strings with completed status.

This method is useful for creating user messages with multiple text segments. Each string
will be converted to a separate `Text` content item.

**Parameters**

| Name | Description |
|------|-------------|
| `messages` | the text content segments; must not be null, empty, or contain null elements |

**Returns**

a new `UserMessage` instance with completed status

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if messages is null, empty, or results in empty content |
| `NullPointerException` | if any message element is null |

---

### `user`

```java
public static @NonNull UserMessage user(@NonNull MessageContent content)
```

Creates a user message from a single content item with completed status.

Use this method when you have a pre-constructed `MessageContent` item (such as text,
image, or audio) that you want to use as a user message.

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the message content; must not be null |

**Returns**

a new `UserMessage` instance with completed status

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if content is null |

---

### `user`

```java
public static @NonNull UserMessage user(@NonNull List<MessageContent> contents)
```

Creates a user message from a list of content items with completed status.

This method is commonly used for multi-modal user messages that combine text with images,
audio, or other content types.

**Parameters**

| Name | Description |
|------|-------------|
| `contents` | the list of message content items; must not be null or empty |

**Returns**

a new `UserMessage` instance with completed status

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if contents is null or empty |
| `NullPointerException` | if any content item is null |

---

### `user`

```java
public static UserMessage user(
      @NonNull List<MessageContent> content, @Nullable InputMessageStatus status)
```

Creates a user message from a list of content items with the specified status.

This is the most flexible factory method for user messages, allowing full control over both
content and status. Use this when reconstructing messages from API responses or when you need
precise control over message state.

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the list of message content items; must not be null or empty |
| `status` | the processing status; may be null (treated as completed) |

**Returns**

a new `UserMessage` instance

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if content is null or empty |
| `NullPointerException` | if any content item is null |

---

### `assistant`

```java
public static @NonNull AssistantMessage assistant(@NonNull String message)
```

Creates an assistant message from a single text string with completed status.

Assistant messages contain model-generated responses from previous conversation turns and
provide context for subsequent interactions.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the text content; must not be null or empty |

**Returns**

a new `AssistantMessage` instance with completed status

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if message is null or results in empty content |

---

### `assistant`

```java
public static @NonNull AssistantMessage assistant(
      @NonNull String message, @NonNull InputMessageStatus status)
```

Creates an assistant message from a single text string with the specified status.

Use this method when you need to specify a particular processing status for the assistant
message, such as when the response is still being generated.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the text content; must not be null or empty |
| `status` | the processing status; must not be null |

**Returns**

a new `AssistantMessage` instance

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if message is null or results in empty content |

---

### `assistant`

```java
public static @NonNull AssistantMessage assistant(@NonNull String... messages)
```

Creates an assistant message from multiple text strings with completed status.

This method is useful for creating assistant messages with multiple response segments. Each
string will be converted to a separate `Text` content item.

**Parameters**

| Name | Description |
|------|-------------|
| `messages` | the text content segments; must not be null, empty, or contain null elements |

**Returns**

a new `AssistantMessage` instance with completed status

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if messages is null, empty, or results in empty content |
| `NullPointerException` | if any message element is null |

---

### `assistant`

```java
public static @NonNull AssistantMessage assistant(@NonNull MessageContent content)
```

Creates an assistant message from a single content item with completed status.

Use this method when you have a pre-constructed `MessageContent` item (such as text,
image, or audio) that represents an assistant response.

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the message content; must not be null |

**Returns**

a new `AssistantMessage` instance with completed status

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if content is null |

---

### `assistant`

```java
public static @NonNull AssistantMessage assistant(@NonNull List<MessageContent> content)
```

Creates an assistant message from a list of content items with completed status.

This method provides flexibility for creating assistant messages with multiple content types
(text, images, etc.) in the response.

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the list of message content items; must not be null or empty |

**Returns**

a new `AssistantMessage` instance with completed status

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if content is null or empty |
| `NullPointerException` | if any content item is null |

---

### `assistant`

```java
public static AssistantMessage assistant(
      @NonNull List<MessageContent> content, @Nullable InputMessageStatus status)
```

Creates an assistant message from a list of content items with the specified status.

This is the most flexible factory method for assistant messages, allowing full control over
both content and status. Use this when reconstructing messages from API responses or when you
need precise control over message state.

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the list of message content items; must not be null or empty |
| `status` | the processing status; may be null (treated as completed) |

**Returns**

a new `AssistantMessage` instance

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if content is null or empty |
| `NullPointerException` | if any content item is null |

---

### `builder`

```java
public static MessageBuilder builder()
```

Creates a new message builder for constructing complex messages.

The builder pattern is useful when you need to construct messages with multiple content
items, conditional content, or when the message structure is determined at runtime.

The builder allows you to:

  
- Add multiple content items incrementally
- Mix different content types (text, images, audio)
- Set the processing status
- Create any message role (user, developer, assistant)

**Returns**

a new `MessageBuilder` instance

---

### `content`

```java
public List<MessageContent> content()
```

Returns the content of this message as an immutable list.

The returned list is a defensive copy and cannot be modified. Any attempt to modify the
returned list will result in an `UnsupportedOperationException`.

**Returns**

an immutable list of message content items; never null or empty

---

### `status`

```java
public @Nullable InputMessageStatus status()
```

Returns the processing status of this message.

The status indicates the current processing state:

  
- `InputMessageStatus.COMPLETED` - Message has been fully processed
- `InputMessageStatus.IN_PROGRESS` - Message is currently being processed
- `InputMessageStatus.INCOMPLETE` - Message processing encountered an error

**Returns**

the status, or `null` if not yet set (typically for newly created messages)

---

### `typeString`

```java
public String typeString()
```

Returns the type string for JSON serialization. The API expects "type": "message" for all
message items.

---

### `role`

```java
public abstract MessageRole role()
```

Returns the role identifier for this message.

The role determines instruction precedence and message interpretation:

  
- `MessageRole.DEVELOPER` - Highest priority system instructions
- `MessageRole.USER` - Standard user input and queries
- `MessageRole.ASSISTANT` - Model-generated responses

This abstract method is implemented by each concrete subclass to return its specific role.

**Returns**

the role identifier for this message type

---

### `outputText`

```java
public String outputText()
```

Returns all text content from this message concatenated into a single string.

This method extracts all text from the message content and concatenates it without any
separator. Non-text content (images, audio, etc.) is ignored.

This is useful for debugging, logging, or when you need a simple text representation of the
message regardless of its internal structure.

**Returns**

the concatenated text content, or an empty string if the message contains no text

---

### `isTextOnly`

```java
public boolean isTextOnly()
```

Returns whether this message contains only text content.

A message is considered text-only if all of its content items are instances of `Text`.
This can be useful for determining whether the message can be processed as simple text or
requires multi-modal handling.

**Returns**

`true` if all content items are text, `false` if any non-text content exists

---

### `getTextContent`

```java
public @NonNull String getTextContent()
```

Returns the text content of this message with spaces between items.

This method extracts only the text content items, converts them to strings, and joins them
with space separators. Non-text content (images, audio, etc.) is filtered out and not included
in the result.

Unlike `.outputText()`, this method adds spaces between content items, making it more
suitable for human-readable output.

**Returns**

the text content joined with spaces, or an empty string if no text content exists

---

### `isCompleted`

```java
public boolean isCompleted()
```

Returns whether this message has been completed processing.

A message is considered completed if its status is explicitly `InputMessageStatus.COMPLETED` or if the status is `null` (which is treated as completed
by default).

**Returns**

`true` if the message is completed or has null status, `false` otherwise

---

### `isInProgress`

```java
public boolean isInProgress()
```

Returns whether this message is currently being processed.

A message is considered in progress if its status is explicitly `InputMessageStatus.IN_PROGRESS`.

**Returns**

`true` if the message is in progress, `false` otherwise

---

### `isIncomplete`

```java
public boolean isIncomplete()
```

Returns whether this message processing is incomplete or encountered an error.

A message is considered incomplete if its status is explicitly `InputMessageStatus.INCOMPLETE`.

**Returns**

`true` if the message is incomplete, `false` otherwise

---

### `contentCount`

```java
public int contentCount()
```

Returns the number of content items in this message.

This is useful for understanding the complexity of the message and for validation purposes.

**Returns**

the number of content items; always at least 1

---

### `equals`

```java
public boolean equals(Object obj)
```

Compares this message to another object for equality.

Two messages are considered equal if:

  
- They are the same object (reference equality), or
- The other object is also a Message, and
- They have equal content lists, and
- They have equal status values

Note that the role is not explicitly compared because messages of different roles (user,
developer, assistant) are different classes and will fail the `instanceof` check.

**Parameters**

| Name | Description |
|------|-------------|
| `obj` | the object to compare with |

**Returns**

`true` if the objects are equal, `false` otherwise

---

### `hashCode`

```java
public int hashCode()
```

Returns a hash code value for this message.

The hash code is computed from the content list and status, ensuring that equal messages (as
defined by `.equals(Object)`) have equal hash codes.

**Returns**

a hash code value for this message

---

### `toString`

```java
public String toString()
```

Returns a string representation of this message for debugging purposes.

The string representation includes:

  
- The concrete class name (UserMessage, DeveloperMessage, or AssistantMessage)
- The message role
- The number of content items
- The processing status

Example output: `UserMessage[role=USER, contentCount=2, status=COMPLETED]`

**Note:** This method does not include the actual content text for privacy and brevity
reasons. Use `.outputText()` or `.getTextContent()` if you need the actual content.

**Returns**

a string representation of this message

---

### `MessageBuilder`

```java
private MessageBuilder()
```

Private constructor to enforce factory method usage.

**See Also**

- `Message#builder()`

---

### `addText`

```java
public MessageBuilder addText(@NonNull String text)
```

Adds a text content item to this message.

The text will be wrapped in a `Text` content item. This is the most convenient way
to add simple text content to a message.

**Parameters**

| Name | Description |
|------|-------------|
| `text` | the text to add; must not be null |

**Returns**

this builder instance for method chaining

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if text is null |

---

### `addContent`

```java
public MessageBuilder addContent(@NonNull MessageContent content)
```

Adds a pre-constructed content item to this message.

Use this method when you have already created a `MessageContent` instance (such as
`Text`, `Image`, or audio content) and want to add it to the message.

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the content to add; must not be null |

**Returns**

this builder instance for method chaining

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if content is null |

---

### `addContents`

```java
public MessageBuilder addContents(@NonNull List<MessageContent> contentList)
```

Adds multiple pre-constructed content items to this message.

Use this method to add several content items at once. This is useful when you have a
collection of content items that you want to include in the message.

**Parameters**

| Name | Description |
|------|-------------|
| `contentList` | the list of content items to add; must not be null, and must not contain null elements |

**Returns**

this builder instance for method chaining

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if contentList is null or contains null elements |

---

### `status`

```java
public MessageBuilder status(@NonNull InputMessageStatus status)
```

Sets the processing status for the message being built.

If not called, the status defaults to `InputMessageStatus.COMPLETED`.

**Parameters**

| Name | Description |
|------|-------------|
| `status` | the processing status; must not be null |

**Returns**

this builder instance for method chaining

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if status is null |

---

### `asUser`

```java
public @NonNull UserMessage asUser()
```

Builds and returns a `UserMessage` with the accumulated content.

This is a terminal operation that creates the final message. The builder can technically
be reused after this call, but it's recommended to create a new builder for each message to
avoid confusion.

**Returns**

a new `UserMessage` instance

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if no content has been added to the builder |

---

### `asDeveloper`

```java
public @NonNull DeveloperMessage asDeveloper()
```

Builds and returns a `DeveloperMessage` with the accumulated content.

This is a terminal operation that creates the final message. The builder can technically
be reused after this call, but it's recommended to create a new builder for each message to
avoid confusion.

**Returns**

a new `DeveloperMessage` instance

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if no content has been added to the builder |

---

### `asAssistant`

```java
public @NonNull AssistantMessage asAssistant()
```

Builds and returns an `AssistantMessage` with the accumulated content.

This is a terminal operation that creates the final message. The builder can technically
be reused after this call, but it's recommended to create a new builder for each message to
avoid confusion.

**Returns**

a new `AssistantMessage` instance

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if no content has been added to the builder |

