# :material-approximately-equal: MessagingProvider

> This docs was updated at: 2026-02-23

`com.paragon.messaging.core.MessagingProvider` &nbsp;·&nbsp; **Interface**

---

Interface for messaging providers (WhatsApp, Facebook Messenger, etc.).

This interface defines the contract for sending messages through different messaging
platforms. With Java virtual threads, the API is synchronous and simple, but highly scalable when
executed in virtual threads.

### Usage with Virtual Threads

```java
// Form 1: Virtual thread per task executor
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> provider.sendMessage(recipient, message));
}
// Form 2: Direct virtual thread
Thread.startVirtualThread(() -> {
    try {
        MessageResponse response = provider.sendMessage(recipient, message);
        System.out.println("Sent: " + response.messageId());
    } catch (MessagingException e) {
        e.printStackTrace();
    }
});
// Form 3: Structured Concurrency (Java 25)
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    var task1 = scope.fork(() -> provider.sendMessage(recipient1, message));
    var task2 = scope.fork(() -> provider.sendMessage(recipient2, message));
    scope.join();
    scope.throwIfFailed();
    MessageResponse r1 = task1.get();
    MessageResponse r2 = task2.get();
}
```

*Since: 2.0*

## Methods

### `getProviderType`

```java
ProviderType getProviderType()
```

Returns the provider type.

**Returns**

the provider type

---

### `isConfigured`

```java
boolean isConfigured()
```

Checks if the provider is configured and ready to send messages.

**Returns**

true if the provider is ready, false otherwise

---

### `sendMessage`

```java
MessageResponse sendMessage(
      @NotNull @Valid Recipient recipient, @NotNull @Valid OutboundMessage message)
      throws MessagingException
```

Sends a message through the provider.

This method is blocking but can be executed efficiently in virtual threads without consuming
platform threads.

**Parameters**

| Name | Description |
|------|-------------|
| `recipient` | message recipient (cannot be null) |
| `message` | message content (cannot be null, will be validated) |

**Returns**

send response containing message ID and status

**Throws**

| Type | Condition |
|------|-----------|
| `MessagingException` | if there is a send error |

---

### `sendText`

```java
default MessageResponse sendText(
      @NotNull @Valid Recipient recipient, @NotNull @Valid TextMessage textMessage)
      throws MessagingException
```

Sends a simple text message.

**Parameters**

| Name | Description |
|------|-------------|
| `recipient` | message recipient |
| `textMessage` | text message |

**Returns**

send response

**Throws**

| Type | Condition |
|------|-----------|
| `MessagingException` | if there is a send error |

---

### `sendMedia`

```java
default MessageResponse sendMedia(
      @NotNull @Valid Recipient recipient, @NotNull @Valid MediaMessage mediaMessage)
      throws MessagingException
```

Sends a media message (image, video, audio, document).

**Parameters**

| Name | Description |
|------|-------------|
| `recipient` | message recipient |
| `mediaMessage` | media message |

**Returns**

send response

**Throws**

| Type | Condition |
|------|-----------|
| `MessagingException` | if there is a send error |

---

### `sendTemplate`

```java
default MessageResponse sendTemplate(
      @NotNull @Valid Recipient recipient, @NotNull @Valid TemplateMessage templateMessage)
      throws MessagingException
```

Sends a template message (for messages outside the 24h window).

**Parameters**

| Name | Description |
|------|-------------|
| `recipient` | message recipient |
| `templateMessage` | template-based message |

**Returns**

send response

**Throws**

| Type | Condition |
|------|-----------|
| `MessagingException` | if there is a send error |

---

### `sendInteractive`

```java
default MessageResponse sendInteractive(
      @NotNull @Valid Recipient recipient, @NotNull @Valid InteractiveMessage interactiveMessage)
      throws MessagingException
```

Sends an interactive message (buttons, lists, etc.).

**Parameters**

| Name | Description |
|------|-------------|
| `recipient` | message recipient |
| `interactiveMessage` | interactive message |

**Returns**

send response

**Throws**

| Type | Condition |
|------|-----------|
| `MessagingException` | if there is a send error |

---

### `sendLocation`

```java
default MessageResponse sendLocation(
      @NotNull @Valid Recipient recipient, @NotNull @Valid LocationMessage locationMessage)
      throws MessagingException
```

Sends a location.

**Parameters**

| Name | Description |
|------|-------------|
| `recipient` | message recipient |
| `locationMessage` | location message |

**Returns**

send response

**Throws**

| Type | Condition |
|------|-----------|
| `MessagingException` | if there is a send error |

---

### `sendContact`

```java
default MessageResponse sendContact(
      @NotNull @Valid Recipient recipient, @NotNull @Valid ContactMessage contactMessage)
      throws MessagingException
```

Sends one or more contacts.

**Parameters**

| Name | Description |
|------|-------------|
| `recipient` | message recipient |
| `contactMessage` | contact message |

**Returns**

send response

**Throws**

| Type | Condition |
|------|-----------|
| `MessagingException` | if there is a send error |

---

### `sendReaction`

```java
default MessageResponse sendReaction(
      @NotNull @Valid Recipient recipient, @NotNull @Valid ReactionMessage reactionMessage)
      throws MessagingException
```

Sends a reaction to an existing message.

**Parameters**

| Name | Description |
|------|-------------|
| `recipient` | reaction recipient |
| `reactionMessage` | reaction (emoji) |

**Returns**

send response

**Throws**

| Type | Condition |
|------|-----------|
| `MessagingException` | if there is a send error |

---

### `sendBatch`

```java
default java.util.List<MessageResponse> sendBatch(
      @NotNull @Valid Recipient recipient,
      @NotNull java.util.List<@Valid ? extends OutboundMessage> messages)
      throws MessagingException
```

Sends multiple messages in parallel using virtual threads.

This method uses Structured Concurrency (Java 25 JEP 505) with the new `Joiner.allSuccessfulOrThrow()` that fails if ANY message fails.

**Behavior:**

  
- All messages are sent in parallel (one virtual thread per message)
- If ONE message fails, ALL others are automatically cancelled
- Returns results in the same order as input messages

**Parameters**

| Name | Description |
|------|-------------|
| `recipient` | message recipient |
| `messages` | list of messages to send |

**Returns**

list of responses in the same order as messages

**Throws**

| Type | Condition |
|------|-----------|
| `MessagingException` | if any message fails |

---

### `sendBroadcast`

```java
default java.util.List<MessageResponse> sendBroadcast(
      @NotNull java.util.List<@Valid Recipient> recipients, @NotNull @Valid OutboundMessage message)
      throws MessagingException
```

Sends to multiple recipients in parallel, returns only successes.

Unlike `.sendBatch`, this method does NOT fail if some messages fail. Use when you
want "best effort" (e.g., mass notifications).

**Behavior:**

  
- All messages are sent in parallel
- Individual failures do NOT cancel others
- Returns only successes

**Parameters**

| Name | Description |
|------|-------------|
| `recipients` | list of recipients |
| `message` | message to send to all |

**Returns**

list of successful responses (may be empty)

**Throws**

| Type | Condition |
|------|-----------|
| `MessagingException` | only if the send process itself fails |

