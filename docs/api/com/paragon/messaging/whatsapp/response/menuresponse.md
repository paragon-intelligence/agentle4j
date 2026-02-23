# :material-database: MenuResponse

> This docs was updated at: 2026-02-23

`com.paragon.messaging.whatsapp.response.MenuResponse` &nbsp;·&nbsp; **Record**

---

## Methods

### `toMessages`

```java
List<OutboundMessage> toMessages()
```

Converts this response to one or more WhatsApp outbound messages.

The returned messages are sent in order. For most responses, a single message is sufficient.
Multiple messages can be used for:

  
- Sending text followed by an image
- Sending multiple media items
- Sending a message then buttons

**Returns**

list of outbound messages to send

---

### `getTextContent`

```java
String getTextContent()
```

Returns the primary text content of this response.

Used for:

  
- Conversation history storage
- TTS synthesis (if enabled)
- Logging and debugging

**Returns**

the text content, or empty string if no text

---

### `getReactionEmoji`

```java
default @Nullable String getReactionEmoji()
```

Returns the emoji to react with, if any.

When set, a reaction message is sent before the main response. The reaction is sent to the
message specified by `.getReactToMessageId()`.

**Returns**

the reaction emoji, or null for no reaction

---

### `getReactToMessageId`

```java
default @Nullable String getReactToMessageId()
```

Returns the message ID to react to, if any.

Required when `.getReactionEmoji()` returns non-null.

**Returns**

the message ID to react to, or null

---

### `getReplyToMessageId`

```java
default @Nullable String getReplyToMessageId()
```

Returns the message ID to reply to (quote), if any.

When set, the response messages will appear as replies/quotes to the specified message in
the WhatsApp client.

**Returns**

the message ID to reply to, or null

---

### `getContext`

```java
default @Nullable ResponseContext getContext()
```

Returns the response context, if any.

The context provides additional metadata about how the response should be sent. If this
returns non-null, the individual getters (getReactionEmoji, getReplyToMessageId, etc.) should
delegate to it.

**Returns**

the response context, or null

---

### `hasReaction`

```java
default boolean hasReaction()
```

Checks if this response has any reaction.

**Returns**

true if getReactionEmoji() and getReactToMessageId() are non-null

---

### `hasReply`

```java
default boolean hasReply()
```

Checks if this response should be sent as a reply.

**Returns**

true if getReplyToMessageId() is non-null

