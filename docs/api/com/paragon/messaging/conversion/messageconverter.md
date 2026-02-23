# :material-approximately-equal: MessageConverter

> This docs was updated at: 2026-02-23

`com.paragon.messaging.conversion.MessageConverter` &nbsp;·&nbsp; **Interface**

---

Converts between WhatsApp messages and framework message types.

This interface provides bidirectional conversion between:

  
- `InboundMessage` (from WhatsApp) → `UserMessage` (for AI)
- `OutboundMessage` (to WhatsApp) → `AssistantMessage` (for history)
- AI text response → `OutboundMessage` (for sending)

### Usage Example

```java
MessageConverter converter = DefaultMessageConverter.create();
// Convert incoming WhatsApp message to UserMessage for AI context
InboundMessage webhookMessage = ...;
UserMessage userMessage = converter.toUserMessage(webhookMessage);
// Convert AI response to OutboundMessage for sending
String aiResponse = "Hello! How can I help you today?";
OutboundMessage outbound = converter.toOutboundMessage(aiResponse);
// Convert batch of inbound messages to user messages
List batch = ...;
List userMessages = converter.toUserMessages(batch);
```

**See Also**

- `DefaultMessageConverter`

*Since: 2.1*

## Methods

### `toUserMessage`

```java
UserMessage toUserMessage(@NonNull InboundMessage inbound)
```

Converts an inbound WhatsApp message to a framework UserMessage.

The conversion extracts text content from the inbound message:

  
- Text messages: body text
- Media messages: caption or type placeholder ([Image], [Video], etc.)
- Interactive messages: button/list selection text
- Location messages: coordinates description

**Parameters**

| Name | Description |
|------|-------------|
| `inbound` | the inbound WhatsApp message |

**Returns**

a UserMessage containing the extracted content

---

### `toUserMessage`

```java
UserMessage toUserMessage(@NonNull List<? extends InboundMessage> inboundMessages)
```

Converts multiple inbound WhatsApp messages to a single UserMessage.

When users send multiple messages in quick succession (message batching), they should be
combined into a single UserMessage for the AI context. Messages are joined with newlines in
chronological order.

**Parameters**

| Name | Description |
|------|-------------|
| `inboundMessages` | the list of inbound messages (chronological order) |

**Returns**

a single UserMessage containing combined content

---

### `toUserMessages`

```java
List<UserMessage> toUserMessages(
      @NonNull List<? extends InboundMessage> inboundMessages)
```

Converts multiple inbound messages to individual UserMessages.

Use this when you need to preserve individual message boundaries in conversation history.

**Parameters**

| Name | Description |
|------|-------------|
| `inboundMessages` | the list of inbound messages |

**Returns**

a list of UserMessages, one per inbound message

---

### `toAssistantMessage`

```java
AssistantMessage toAssistantMessage(@NonNull OutboundMessage outbound)
```

Converts an outbound WhatsApp message to an AssistantMessage.

This is useful for building conversation history that includes assistant responses. The
outbound message's text content is extracted and wrapped in an AssistantMessage.

**Parameters**

| Name | Description |
|------|-------------|
| `outbound` | the outbound WhatsApp message |

**Returns**

an AssistantMessage containing the extracted content

---

### `toAssistantMessages`

```java
List<AssistantMessage> toAssistantMessages(
      @NonNull List<? extends OutboundMessage> outboundMessages)
```

Converts multiple outbound messages to individual AssistantMessages.

**Parameters**

| Name | Description |
|------|-------------|
| `outboundMessages` | the list of outbound messages |

**Returns**

a list of AssistantMessages

---

### `toOutboundMessage`

```java
OutboundMessage toOutboundMessage(@NonNull String aiResponse)
```

Converts a plain AI text response to an OutboundMessage.

This is the simplest conversion, wrapping the AI's text response in a TextMessage ready for
sending via WhatsApp.

**Parameters**

| Name | Description |
|------|-------------|
| `aiResponse` | the AI's text response |

**Returns**

an OutboundMessage (typically TextMessage)

---

### `toOutboundMessage`

```java
OutboundMessage toOutboundMessage(
      @NonNull String aiResponse, @NonNull String replyToMessageId)
```

Converts an AI text response to an OutboundMessage with reply context.

The resulting message will quote/reply to the specified message ID when rendered in the
WhatsApp client.

**Parameters**

| Name | Description |
|------|-------------|
| `aiResponse` | the AI's text response |
| `replyToMessageId` | the message ID to reply to |

**Returns**

an OutboundMessage with reply context

