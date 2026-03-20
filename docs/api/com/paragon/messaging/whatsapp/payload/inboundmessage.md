# :material-approximately-equal: InboundMessage

`com.paragon.messaging.whatsapp.payload.InboundMessage` &nbsp;·&nbsp; **Interface**

---

Sealed interface for inbound WhatsApp webhook messages.

These are messages received FROM WhatsApp users TO the application via the Meta WhatsApp
Business API webhook. For outbound messages sent to users, see `com.paragon.messaging.core.OutboundMessage`.

### Supported Inbound Message Types

  
- `TextMessage` - Plain text messages
- `ImageMessage` - Image attachments
- `VideoMessage` - Video attachments
- `AudioMessage` - Voice messages and audio files
- `DocumentMessage` - Document/file attachments
- `StickerMessage` - Sticker messages
- `InteractiveMessage` - Button and list replies
- `LocationMessage` - Shared locations
- `ReactionMessage` - Emoji reactions
- `SystemMessage` - System notifications
- `OrderMessage` - Commerce order messages

### JSON Deserialization

This interface uses Jackson polymorphic type handling to automatically deserialize webhook
payloads to the correct message type based on the `type` field.

*Since: 2.1*

## Methods

### `from`

```java
String from()
```

Returns the sender's WhatsApp ID (phone number).

**Returns**

the sender's WhatsApp ID

---

### `id`

```java
String id()
```

Returns the unique message ID assigned by WhatsApp.

This ID can be used for:

  
- Message deduplication
- Replying to specific messages
- Sending reactions
- Tracking delivery status

**Returns**

the unique message ID

---

### `timestamp`

```java
String timestamp()
```

Returns the message timestamp (Unix epoch seconds).

**Returns**

the message timestamp

---

### `type`

```java
String type()
```

Returns the message type identifier.

Common values: "text", "image", "video", "audio", "document", "sticker", "interactive",
"location", "reaction", "system", "order"

**Returns**

the message type

---

### `context`

```java
MessageContext context()
```

Returns the message context if this is a reply to another message.

The context contains information about the quoted message, including its ID and whether it
was forwarded.

**Returns**

the message context, or null if not a reply

---

### `extractTextContent`

```java
default @NonNull String extractTextContent()
```

Extracts the primary text content from this inbound message.

For non-text messages, returns a descriptive placeholder:

  
- Text: Returns the message body
- Image/Video: Returns caption or "[Image]"/"[Video]"
- Audio: Returns "[Audio message]"
- Location: Returns "[Location]"
- Interactive: Returns button/list selection

**Returns**

extracted text content for AI processing

---

### `isReply`

```java
default boolean isReply()
```

Checks if this message is a reply to another message.

**Returns**

true if this message has reply context

---

### `repliedToMessageId`

```java
default @Nullable String repliedToMessageId()
```

Returns the ID of the message this is replying to, if any.

**Returns**

the replied message ID, or null

