# :material-database: TextResponse

`com.paragon.messaging.whatsapp.response.TextResponse` &nbsp;·&nbsp; **Record**

---

Simple text response for structured AI output.

Represents a plain text message with optional reaction and reply context.

### Usage Example

```java
// Simple text response
TextResponse response = new TextResponse("Hello! How can I help?");
// With reply context
TextResponse response = TextResponse.builder()
    .text("Thanks for your message!")
    .replyTo("wamid.xyz123")
    .build();
// With reaction
TextResponse response = TextResponse.builder()
    .text("Great choice!")
    .reactTo("wamid.xyz123", "👍")
    .build();
```

*Since: 2.1*

## Methods

### `TextResponse`

```java
public TextResponse(@NonNull String text)
```

Creates a simple text response with no context.

**Parameters**

| Name | Description |
|------|-------------|
| `text` | the message text |

---

### `builder`

```java
public static Builder builder()
```

Creates a builder for TextResponse.

**Returns**

new builder

---

### `text`

```java
public Builder text(@NonNull String text)
```

Sets the message text.

**Parameters**

| Name | Description |
|------|-------------|
| `text` | the text content |

**Returns**

this builder

---

### `replyTo`

```java
public Builder replyTo(@NonNull String messageId)
```

Sets the message ID to reply to.

**Parameters**

| Name | Description |
|------|-------------|
| `messageId` | the message ID |

**Returns**

this builder

---

### `reactTo`

```java
public Builder reactTo(@NonNull String messageId, @NonNull String emoji)
```

Sets the reaction for this response.

**Parameters**

| Name | Description |
|------|-------------|
| `messageId` | the message ID to react to |
| `emoji` | the reaction emoji |

**Returns**

this builder

---

### `previewUrl`

```java
public Builder previewUrl(boolean preview)
```

Enables or disables URL preview.

**Parameters**

| Name | Description |
|------|-------------|
| `preview` | true to enable URL preview |

**Returns**

this builder

---

### `context`

```java
public Builder context(@NonNull ResponseContext context)
```

Sets the full response context.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | the context |

**Returns**

this builder

---

### `build`

```java
public TextResponse build()
```

Builds the TextResponse.

**Returns**

the built response

