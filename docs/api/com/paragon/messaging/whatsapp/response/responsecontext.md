# :material-database: ResponseContext

> This docs was updated at: 2026-02-23

`com.paragon.messaging.whatsapp.response.ResponseContext` &nbsp;·&nbsp; **Record**

---

Context for WhatsApp responses including reaction and reply metadata.

Provides additional context for how responses should be sent, including:

  
- Reply context (quote a specific message)
- Reactions (send an emoji reaction)
- URL preview settings
- Typing indicators

### Usage Examples

```java
// Simple response (no special context)
ResponseContext context = ResponseContext.simple();
// Reply to a specific message
ResponseContext context = ResponseContext.replyTo("wamid.xyz123");
// React to user's message
ResponseContext context = ResponseContext.withReaction("wamid.xyz123", "👍");
// Combine reply and reaction
ResponseContext context = ResponseContext.builder()
    .replyTo("wamid.xyz123")
    .reactTo("wamid.xyz123", "🎉")
    .typingIndicator(Duration.ofSeconds(2))
    .build();
```

*Since: 2.1*

## Methods

### `simple`

```java
public static ResponseContext simple()
```

Creates a simple response context with no special settings.

URL preview is enabled by default.

**Returns**

simple context

---

### `replyTo`

```java
public static ResponseContext replyTo(@NonNull String messageId)
```

Creates a context that replies to a specific message.

The response will appear as a quoted reply in WhatsApp.

**Parameters**

| Name | Description |
|------|-------------|
| `messageId` | the message ID to reply to |

**Returns**

reply context

---

### `withReaction`

```java
public static ResponseContext withReaction(@NonNull String messageId, @NonNull String emoji)
```

Creates a context that reacts to a message with an emoji.

The reaction is sent as a separate message before the main response.

**Parameters**

| Name | Description |
|------|-------------|
| `messageId` | the message ID to react to |
| `emoji` | the emoji reaction |

**Returns**

reaction context

---

### `replyAndReact`

```java
public static ResponseContext replyAndReact(@NonNull String messageId, @NonNull String emoji)
```

Creates a context that both replies to and reacts to a message.

**Parameters**

| Name | Description |
|------|-------------|
| `messageId` | the message ID to reply to and react to |
| `emoji` | the emoji reaction |

**Returns**

combined reply and reaction context

---

### `builder`

```java
public static Builder builder()
```

Creates a builder for custom response context.

**Returns**

new builder

---

### `hasReply`

```java
public boolean hasReply()
```

Checks if this context has a reply-to message.

**Returns**

true if replyToMessageId is set

---

### `hasReaction`

```java
public boolean hasReaction()
```

Checks if this context has a reaction.

**Returns**

true if both reactionEmoji and reactToMessageId are set

---

### `hasTypingIndicator`

```java
public boolean hasTypingIndicator()
```

Checks if a typing indicator should be shown.

**Returns**

true if typingIndicator is set and positive

---

### `replyTo`

```java
public Builder replyTo(@Nullable String messageId)
```

Sets the message ID to reply to (quote).

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

Sets the reaction emoji and target message.

**Parameters**

| Name | Description |
|------|-------------|
| `messageId` | the message ID to react to |
| `emoji` | the emoji reaction |

**Returns**

this builder

---

### `reaction`

```java
public Builder reaction(@NonNull String emoji)
```

Sets just the reaction emoji (uses replyToMessageId as target if set).

**Parameters**

| Name | Description |
|------|-------------|
| `emoji` | the emoji reaction |

**Returns**

this builder

---

### `previewUrl`

```java
public Builder previewUrl(boolean preview)
```

Enables or disables URL preview in text messages.

**Parameters**

| Name | Description |
|------|-------------|
| `preview` | true to enable URL preview |

**Returns**

this builder

---

### `typingIndicator`

```java
public Builder typingIndicator(@Nullable Duration duration)
```

Sets the typing indicator duration.

When set, a typing indicator will be shown for this duration before the response is sent.

**Parameters**

| Name | Description |
|------|-------------|
| `duration` | the typing indicator duration |

**Returns**

this builder

---

### `build`

```java
public ResponseContext build()
```

Builds the ResponseContext.

**Returns**

the built context

