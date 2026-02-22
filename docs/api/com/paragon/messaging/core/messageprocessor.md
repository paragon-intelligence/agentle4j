# :material-approximately-equal: MessageProcessor

`com.paragon.messaging.core.MessageProcessor` &nbsp;·&nbsp; **Interface**

---

Processes batched messages from a user.

Implementations typically:

  
- Combine messages into a single input
- Call AI agent (via `com.paragon.agents.Interactable`)
- Send response via messaging platform
- Optionally convert to audio (TTS)

### Simple Example

```java
MessageProcessor processor = (userId, messages, context) -> {
    // 1. Combine messages
    String input = messages.stream()
        .map(InboundMessage::extractTextContent)
        .collect(Collectors.joining("\n"));
    // 2. Process with AI agent
    AgentResult result = agent.interact(input);
    String response = result.output();
    // 3. Send response
    whatsappProvider.sendText(
        Recipient.ofPhoneNumber(userId),
        new TextMessage(response)
    );
};
```

### Example with TTS

```java
MessageProcessor processor = (userId, messages, context) -> {
    String input = combineMessages(messages);
    String response = agent.interact(input).output();
    // Decide text vs audio
    if (random.nextDouble() < speechPlayChance) {
        byte[] audio = ttsProvider.synthesize(response, ttsConfig);
        sendAudio(userId, audio);
    } else {
        sendText(userId, response);
    }
};
```

### Thread Safety

Processors are called from virtual threads and may be invoked concurrently for different
users. Implementations must be thread-safe if accessing shared mutable state.

### Error Handling

Exceptions thrown are captured by `com.paragon.messaging.batching.MessageBatchingService` and handled according to
`com.paragon.messaging.error.ErrorHandlingStrategy`.

**See Also**

- `com.paragon.messaging.batching.MessageBatchingService`
- `com.paragon.messaging.processor.AIAgentProcessor`

*Since: 1.0*

## Methods

### `noOp`

```java
static MessageProcessor noOp()
```

No-op processor that does nothing.

**Returns**

empty processor

---

### `logging`

```java
static MessageProcessor logging(Consumer<String> logger)
```

Logging processor that only logs messages.

**Parameters**

| Name | Description |
|------|-------------|
| `logger` | consumer for log messages |

**Returns**

logging processor

---

### `process`

```java
void process(
          @NonNull String userId,
          @NonNull List<? extends InboundMessage> messages,
          @NonNull ProcessingContext context)
          throws Exception
```

Processes a batch of messages from a user.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | the user's unique identifier (e.g., WhatsApp phone number) |
| `messages` | batch of messages (guaranteed non-empty and ordered by timestamp) |
| `context` | processing context with metadata about the batch |

**Throws**

| Type | Condition |
|------|-----------|
| `Exception` | if processing fails (will be handled by error strategy) |

---

### `process`

```java
default void process(@NonNull String userId, @NonNull List<? extends InboundMessage> messages)
          throws Exception
```

Simplified processing without context.

Delegates to `List, ProcessingContext)` with an empty context.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | the user's unique identifier |
| `messages` | batch of messages |

**Throws**

| Type | Condition |
|------|-----------|
| `Exception` | if processing fails |

---

### `batch`

```java
TIMEOUT,

    /**
     * User stopped sending for the silence threshold duration.
     */
    SILENCE,

    /**
     * Buffer reached maximum size.
     */
    BUFFER_FULL,

    /**
     * Unknown or unspecified reason.
     */
    UNKNOWN
  }

  /**
   * Context information passed to the processor during batch processing.
   *
   * @param batchId          unique identifier for this batch
   * @param firstMessageId   ID of the first message in the batch (for reply context)
   * @param lastMessageId    ID of the last message in the batch
   * @param processingReason why the batch was trigg
```

Maximum wait time (adaptive timeout) was reached.

---

### `batch`

```java
SILENCE,

    /**
     * Buffer reached maximum size.
     */
    BUFFER_FULL,

    /**
     * Unknown or unspecified reason.
     */
    UNKNOWN
  }

  /**
   * Context information passed to the processor during batch processing.
   *
   * @param batchId          unique identifier for this batch
   * @param firstMessageId   ID of the first message in the batch (for reply context)
   * @param lastMessageId    ID of the last message in the batch
   * @param processingReason why the batch was triggered (timeout, silence, buffer full)
   * @param retryAttempt     current retry attempt (0 for
```

User stopped sending for the silence threshold duration.

---

### `empty`

```java
public static ProcessingContext empty()
```

Creates an empty context for simple processing.

**Returns**

empty context

---

### `create`

```java
public static ProcessingContext create(
            String batchId, String firstMessageId, String lastMessageId, ProcessingReason reason)
```

Creates a context for a new batch.

**Parameters**

| Name | Description |
|------|-------------|
| `batchId` | unique batch identifier |
| `firstMessageId` | first message ID |
| `lastMessageId` | last message ID |
| `reason` | processing trigger reason |

**Returns**

new context

---

### `retry`

```java
public ProcessingContext retry()
```

Creates a retry context from this context.

**Returns**

context with incremented retry attempt

---

### `isRetry`

```java
public boolean isRetry()
```

Checks if this is a retry attempt.

**Returns**

true if retryAttempt > 0

