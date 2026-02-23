# :material-code-braces: UserMessage

> This docs was updated at: 2026-02-23

`com.paragon.responses.spec.UserMessage` &nbsp;·&nbsp; **Class**

Extends `Message`

---

Represents a user message containing instructions or queries from end users.

User messages provide input from the application's end users, such as questions, requests, or
conversational input. These messages have lower priority than developer or system messages in the
instruction hierarchy, meaning developer-level instructions will override conflicting user
instructions.

This is a final implementation of `Message` and cannot be subclassed. User messages are
created without an explicit status, which is determined and populated by the API during
processing.

**See Also**

- `Message`
- `DeveloperMessage`
- `AssistantMessage`

## Methods

### `UserMessage`

```java
public UserMessage(@NonNull List<MessageContent> content)
```

Constructs a UserMessage with the specified content.

The status is implicitly set to `null` by the parent constructor, and will be
populated by the API after processing.

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the message content from the user; must not be null |

---

### `UserMessage`

```java
public UserMessage(
      @JsonProperty("content") @NonNull List<MessageContent> content,
      @JsonProperty("status") @Nullable InputMessageStatus status)
```

Constructs a UserMessage with the specified content and status (for deserialization).

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the message content from the user; must not be null |
| `status` | the processing status; may be null |

---

### `text`

```java
public static UserMessage text(@NonNull String text)
```

Convenience method to create a UserMessage with a single text content.

**Parameters**

| Name | Description |
|------|-------------|
| `text` | the text content |

**Returns**

a new UserMessage with the text

---

### `role`

```java
public MessageRole role()
```

Returns the role identifier for this message type.

User messages have lower priority than developer messages but represent the primary
conversational input from application users.

**Returns**

the string `"user"`

