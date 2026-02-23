# :material-code-braces: AIAgentProcessor

> This docs was updated at: 2026-02-23

`com.paragon.messaging.processor.AIAgentProcessor` &nbsp;·&nbsp; **Class**

Implements `MessageProcessor`

---

AI-powered message processor using the Interactable interface.

Processes incoming WhatsApp messages through an AI agent and sends responses back to the user.
Supports both simple text responses and structured outputs via `Interactable.Structured`.

### Features

  
- Conversation history for multi-turn context
- Message batching (combines multiple messages into one user turn)
- Optional TTS (text-to-speech) responses
- Structured output support for interactive responses
- Reply context preservation

### Simple Usage

```java
// Create a simple text-response processor
AIAgentProcessor processor = AIAgentProcessor.forAgent(myAgent)
    .messagingProvider(whatsappProvider)
    .build();
// Use with MessageBatchingService
MessageBatchingService service = MessageBatchingService.builder()
    .processor(processor)
    .build();
```

### Structured Output Usage

```java
// Create agent with structured output
Interactable.Structured structuredAgent = Agent.builder()
    .name("MenuAssistant")
    .instructions("Help users navigate our menu")
    .structured(MenuResponse.class)
    .responder(responder)
    .build();
// Create processor for structured responses
AIAgentProcessor processor = AIAgentProcessor
    .forStructuredAgent(structuredAgent)
    .messagingProvider(whatsappProvider)
    .historyStore(RedisConversationHistoryStore.create(redis))
    .maxHistoryMessages(20)
    .build();
```

### With TTS

```java
AIAgentProcessor processor = AIAgentProcessor.forAgent(myAgent)
    .messagingProvider(whatsappProvider)
    .ttsConfig(TTSConfig.builder()
        .provider(elevenLabsProvider)
        .speechChance(0.3)
        .build())
    .build();
```

**See Also**

- `Interactable`
- `Interactable.Structured`
- `MessageProcessor`

*Since: 2.1*

## Methods

### `forAgent`

```java
public static Builder<Void> forAgent(@NonNull Interactable agent)
```

Creates a builder for a simple (non-structured) agent processor.

**Parameters**

| Name | Description |
|------|-------------|
| `agent` | the agent to process messages |

**Returns**

a new builder

---

### `forStructuredAgent`

```java
public static <T> Builder<T> forStructuredAgent(Interactable.@NonNull Structured<T> agent)
```

Creates a builder for a structured output agent processor.

**Parameters**

| Name | Description |
|------|-------------|
| `agent` | the structured agent to process messages |
| `<T>` | the structured output type |

**Returns**

a new builder

---

### `messagingProvider`

```java
public Builder<T> messagingProvider(@NonNull MessagingProvider provider)
```

Sets the messaging provider for sending responses.

This is required.

**Parameters**

| Name | Description |
|------|-------------|
| `provider` | the messaging provider |

**Returns**

this builder

---

### `messageConverter`

```java
public Builder<T> messageConverter(@NonNull MessageConverter converter)
```

Sets a custom message converter.

If not set, uses `DefaultMessageConverter`.

**Parameters**

| Name | Description |
|------|-------------|
| `converter` | the message converter |

**Returns**

this builder

---

### `historyStore`

```java
public Builder<T> historyStore(@NonNull ConversationHistoryStore store)
```

Sets the conversation history store.

If not set, no history is maintained between calls. For multi-turn conversations, use an
in-memory or Redis store.

**Parameters**

| Name | Description |
|------|-------------|
| `store` | the history store |

**Returns**

this builder

---

### `withInMemoryHistory`

```java
public Builder<T> withInMemoryHistory()
```

Enables in-memory conversation history with default settings.

**Returns**

this builder

---

### `withInMemoryHistory`

```java
public Builder<T> withInMemoryHistory(int maxPerUser)
```

Enables in-memory conversation history with custom per-user limit.

**Parameters**

| Name | Description |
|------|-------------|
| `maxPerUser` | maximum messages to store per user |

**Returns**

this builder

---

### `ttsConfig`

```java
public Builder<T> ttsConfig(@NonNull TTSConfig config)
```

Sets the TTS configuration for optional audio responses.

**Parameters**

| Name | Description |
|------|-------------|
| `config` | TTS configuration |

**Returns**

this builder

---

### `mediaUploader`

```java
public Builder<T> mediaUploader(@NonNull WhatsAppMediaUploader uploader)
```

Sets the media uploader for sending audio messages.

If not set, TTS audio will fall back to text messages.

**Parameters**

| Name | Description |
|------|-------------|
| `uploader` | the WhatsApp media uploader |

**Returns**

this builder

---

### `maxHistoryMessages`

```java
public Builder<T> maxHistoryMessages(int max)
```

Sets the maximum number of history messages to include in context.

Default is 20.

**Parameters**

| Name | Description |
|------|-------------|
| `max` | maximum history messages |

**Returns**

this builder

---

### `maxHistoryAge`

```java
public Builder<T> maxHistoryAge(@NonNull Duration maxAge)
```

Sets the maximum age of history messages to include.

Default is 24 hours.

**Parameters**

| Name | Description |
|------|-------------|
| `maxAge` | maximum history age |

**Returns**

this builder

---

### `build`

```java
public AIAgentProcessor<T> build()
```

Builds the AIAgentProcessor.

**Returns**

the configured processor

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalStateException` | if required fields are not set |

