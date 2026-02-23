# :material-code-braces: DeveloperMessage

> This docs was updated at: 2026-02-23

`com.paragon.responses.spec.DeveloperMessage` &nbsp;·&nbsp; **Class**

Extends `Message`

---

Represents a developer-level message with the highest instruction priority.

Developer messages are used to provide system-level instructions, constraints, or context that
should take precedence over user instructions. These messages typically configure the model's
behavior, define boundaries, or establish rules that the model should follow throughout the
conversation.

This is a final implementation of `Message` and cannot be subclassed. Developer messages
are created with a `null` status initially, which is populated by the API after processing.

**See Also**

- `Message`
- `UserMessage`
- `AssistantMessage`

## Methods

### `DeveloperMessage`

```java
public DeveloperMessage(
      @JsonProperty("content") @NonNull List<MessageContent> content,
      @JsonProperty("status") InputMessageStatus status)
```

Constructs a DeveloperMessage with the specified content and status.

This constructor is used by Jackson for deserialization and by the factory method.

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the message content for developer instructions; must not be null |
| `status` | the processing status of the message; typically null for new messages |

---

### `of`

```java
public static DeveloperMessage of(
      @NonNull List<MessageContent> content, @Nullable InputMessageStatus status)
```

Creates a new DeveloperMessage with the specified content.

The message is created with a `null` status, which will be populated by the API after
the message is processed.

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the content for the developer message; must not be null |
| `status` |  |

**Returns**

a new DeveloperMessage instance

---

### `role`

```java
public MessageRole role()
```

Returns the role identifier for this message type.

Developer messages have the highest priority in the instruction hierarchy, overriding both
user and assistant message instructions.

**Returns**

the string `"developer"`

