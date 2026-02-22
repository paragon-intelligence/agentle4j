# :material-code-braces: AgentService

`com.paragon.agents.AgentService` &nbsp;·&nbsp; **Class**

---

## Methods

### `name`

```java
String name()
```

Returns the name of this interactable.

The name is used for identification in multi-agent systems, logging, and user-facing
messages. It should be concise and descriptive.

**Returns**

the name of this interactable

---

### `toBlueprint`

```java
default InteractableBlueprint toBlueprint()
```

Creates a serializable blueprint of this interactable.

The blueprint captures all declarative configuration and can be serialized to JSON via
Jackson. Use `InteractableBlueprint.toInteractable()` to reconstruct a fully functional
interactable from the blueprint — no external dependencies needed.

```java
InteractableBlueprint blueprint = agent.toBlueprint();
String json = objectMapper.writeValueAsString(blueprint);
InteractableBlueprint restored = objectMapper.readValue(json, InteractableBlueprint.class);
Interactable agent = restored.toInteractable();
```

**Returns**

a serializable blueprint of this interactable

**Throws**

| Type | Condition |
|------|-----------|
| `UnsupportedOperationException` | if the implementation does not support blueprints |

---

### `interact`

```java
default AgentResult interact(@NonNull String input)
```

Interacts with the agent using a text input.

Default implementation creates a fresh context with the input as a user message.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the user's text input |

**Returns**

the agent's result

---

### `interact`

```java
default AgentResult interact(@NonNull String input, @Nullable TraceMetadata trace)
```

Interacts with the agent using a text input with optional trace metadata.

Default implementation creates a fresh context with the input as a user message.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the user's text input |
| `trace` | optional trace metadata (overrides agent-level configuration) |

**Returns**

the agent's result

---

### `interact`

```java
default AgentResult interact(@NonNull Text text)
```

Interacts with the agent using Text content.

Default implementation creates a fresh context with the text as a user message.

**Parameters**

| Name | Description |
|------|-------------|
| `text` | the text content |

**Returns**

the agent's result

---

### `interact`

```java
default AgentResult interact(@NonNull Text text, @Nullable TraceMetadata trace)
```

Interacts with the agent using Text content with optional trace metadata.

Default implementation creates a fresh context with the text as a user message.

**Parameters**

| Name | Description |
|------|-------------|
| `text` | the text content |
| `trace` | optional trace metadata (overrides agent-level configuration) |

**Returns**

the agent's result

---

### `interact`

```java
default AgentResult interact(@NonNull Message message)
```

Interacts with the agent using a Message.

Default implementation creates a fresh context with the message.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the message input |

**Returns**

the agent's result

---

### `interact`

```java
default AgentResult interact(@NonNull Message message, @Nullable TraceMetadata trace)
```

Interacts with the agent using a Message with optional trace metadata.

Default implementation creates a fresh context with the message.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the message input |
| `trace` | optional trace metadata (overrides agent-level configuration) |

**Returns**

the agent's result

---

### `interact`

```java
default AgentResult interact(@NonNull Prompt prompt)
```

Interacts with the agent using a Prompt.

The prompt's text content is extracted and used as the input.

**Parameters**

| Name | Description |
|------|-------------|
| `prompt` | the prompt input |

**Returns**

the agent's result

---

### `interact`

```java
default AgentResult interact(@NonNull Prompt prompt, @Nullable TraceMetadata trace)
```

Interacts with the agent using a Prompt with optional trace metadata.

The prompt's text content is extracted and used as the input.

**Parameters**

| Name | Description |
|------|-------------|
| `prompt` | the prompt input |
| `trace` | optional trace metadata (overrides agent-level configuration) |

**Returns**

the agent's result

---

### `interact`

```java
default AgentResult interact(@NonNull File file)
```

Interacts with the agent using a file. Creates a fresh context.

Default implementation creates a fresh context with the file as a user message.

**Parameters**

| Name | Description |
|------|-------------|
| `file` | the file input |

**Returns**

the agent's result

---

### `interact`

```java
default AgentResult interact(@NonNull File file, @Nullable TraceMetadata trace)
```

Interacts with the agent using a file with optional trace metadata.

Default implementation creates a fresh context with the file as a user message.

**Parameters**

| Name | Description |
|------|-------------|
| `file` | the file input |
| `trace` | optional trace metadata (overrides agent-level configuration) |

**Returns**

the agent's result

---

### `interact`

```java
default AgentResult interact(@NonNull Image image)
```

Interacts with the agent using an image. Creates a fresh context.

Default implementation creates a fresh context with the image as a user message.

**Parameters**

| Name | Description |
|------|-------------|
| `image` | the image input |

**Returns**

the agent's result

---

### `interact`

```java
default AgentResult interact(@NonNull Image image, @Nullable TraceMetadata trace)
```

Interacts with the agent using an image with optional trace metadata.

Default implementation creates a fresh context with the image as a user message.

**Parameters**

| Name | Description |
|------|-------------|
| `image` | the image input |
| `trace` | optional trace metadata (overrides agent-level configuration) |

**Returns**

the agent's result

---

### `interact`

```java
default AgentResult interact(@NonNull ResponseInputItem input)
```

Interacts with the agent using a ResponseInputItem. Creates a fresh context.

Default implementation creates a fresh context with the input item.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the input item |

**Returns**

the agent's result

---

### `interact`

```java
default AgentResult interact(@NonNull ResponseInputItem input, @Nullable TraceMetadata trace)
```

Interacts with the agent using a ResponseInputItem with optional trace metadata.

Default implementation creates a fresh context with the input item.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the input item |
| `trace` | optional trace metadata (overrides agent-level configuration) |

**Returns**

the agent's result

---

### `interact`

```java
default AgentResult interact(java.util.@NonNull List<ResponseInputItem> input)
```

Interacts with the agent using multiple inputs. Creates a fresh context.

Default implementation creates a fresh context and adds all input items.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the input items |

**Returns**

the agent's result

---

### `interact`

```java
default AgentResult interact(
      java.util.@NonNull List<ResponseInputItem> input, @Nullable TraceMetadata trace)
```

Interacts with the agent using multiple inputs with optional trace metadata.

Default implementation creates a fresh context and adds all input items.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the input items |
| `trace` | optional trace metadata (overrides agent-level configuration) |

**Returns**

the agent's result

---

### `interact`

```java
default AgentResult interact(@NonNull AgenticContext context)
```

Interacts with the agent using an existing context.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | the conversation context containing history |

**Returns**

the agent's result

---

### `interact`

```java
AgentResult interact(@NonNull AgenticContext context, @Nullable TraceMetadata trace)
```

Interacts with the agent using an existing context with optional trace metadata.

This is the main interact method that all other overloads delegate to.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | the conversation context containing history |
| `trace` | optional trace metadata (overrides agent-level configuration) |

**Returns**

the agent's result

---

### `interactStream`

```java
default AgentStream interactStream(@NonNull String input)
```

Interacts with the agent with streaming support.

Default implementation creates a fresh context with the input as a user message.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the user's text input |

**Returns**

an AgentStream for processing streaming events

---

### `interactStream`

```java
default AgentStream interactStream(@NonNull String input, @Nullable TraceMetadata trace)
```

Interacts with the agent with streaming support and trace metadata.

Default implementation creates a fresh context with the input as a user message.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the user's text input |
| `trace` | optional trace metadata (overrides agent-level configuration) |

**Returns**

an AgentStream for processing streaming events

---

### `interactStream`

```java
default AgentStream interactStream(@NonNull Prompt prompt)
```

Interacts with the agent with streaming using a Prompt.

The prompt's text content is extracted and used as the input.

**Parameters**

| Name | Description |
|------|-------------|
| `prompt` | the prompt input |

**Returns**

an AgentStream for processing streaming events

---

### `interactStream`

```java
default AgentStream interactStream(@NonNull Prompt prompt, @Nullable TraceMetadata trace)
```

Interacts with the agent with streaming using a Prompt and trace metadata.

The prompt's text content is extracted and used as the input.

**Parameters**

| Name | Description |
|------|-------------|
| `prompt` | the prompt input |
| `trace` | optional trace metadata (overrides agent-level configuration) |

**Returns**

an AgentStream for processing streaming events

---

### `interactStream`

```java
default AgentStream interactStream(@NonNull AgenticContext context)
```

Interacts with the agent with streaming using an existing context.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | the conversation context containing history |

**Returns**

an AgentStream for processing streaming events

---

### `interactStream`

```java
AgentStream interactStream(
      @NonNull AgenticContext context, @Nullable TraceMetadata trace)
```

Interacts with the agent with streaming using an existing context and trace metadata.

This is the main streaming method that all other streaming overloads delegate to.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | the conversation context containing history |
| `trace` | optional trace metadata (overrides agent-level configuration) |

**Returns**

an AgentStream for processing streaming events

---

### `interactStructured`

```java
default StructuredAgentResult<T> interactStructured(@NonNull String input)
```

Interacts with the agent and returns a structured result.

Default implementation creates a fresh context with the input as a user message.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the user's text input |

**Returns**

the structured result with parsed output

---

### `interactStructured`

```java
default StructuredAgentResult<T> interactStructured(
        @NonNull String input, @Nullable TraceMetadata trace)
```

Interacts with the agent and returns a structured result with trace metadata.

Default implementation creates a fresh context with the input as a user message.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the user's text input |
| `trace` | optional trace metadata (overrides agent-level configuration) |

**Returns**

the structured result with parsed output

---

### `interactStructured`

```java
default StructuredAgentResult<T> interactStructured(@NonNull Text text)
```

Interacts with the agent with Text content and returns a structured result.

Default implementation creates a fresh context with the text as a user message.

**Parameters**

| Name | Description |
|------|-------------|
| `text` | the text content |

**Returns**

the structured result with parsed output

---

### `interactStructured`

```java
default StructuredAgentResult<T> interactStructured(
        @NonNull Text text, @Nullable TraceMetadata trace)
```

Interacts with the agent with Text content and returns a structured result with trace
metadata.

Default implementation creates a fresh context with the text as a user message.

**Parameters**

| Name | Description |
|------|-------------|
| `text` | the text content |
| `trace` | optional trace metadata (overrides agent-level configuration) |

**Returns**

the structured result with parsed output

---

### `interactStructured`

```java
default StructuredAgentResult<T> interactStructured(@NonNull Message message)
```

Interacts with the agent with a Message and returns a structured result.

Default implementation creates a fresh context with the message.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the message input |

**Returns**

the structured result with parsed output

---

### `interactStructured`

```java
default StructuredAgentResult<T> interactStructured(
        @NonNull Message message, @Nullable TraceMetadata trace)
```

Interacts with the agent with a Message and returns a structured result with trace metadata.

Default implementation creates a fresh context with the message.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the message input |
| `trace` | optional trace metadata (overrides agent-level configuration) |

**Returns**

the structured result with parsed output

---

### `interactStructured`

```java
default StructuredAgentResult<T> interactStructured(@NonNull Prompt prompt)
```

Interacts with the agent with a Prompt and returns a structured result.

The prompt's text content is extracted and used as the input.

**Parameters**

| Name | Description |
|------|-------------|
| `prompt` | the prompt input |

**Returns**

the structured result with parsed output

---

### `interactStructured`

```java
default StructuredAgentResult<T> interactStructured(
        @NonNull Prompt prompt, @Nullable TraceMetadata trace)
```

Interacts with the agent with a Prompt and returns a structured result with trace metadata.

The prompt's text content is extracted and used as the input.

**Parameters**

| Name | Description |
|------|-------------|
| `prompt` | the prompt input |
| `trace` | optional trace metadata (overrides agent-level configuration) |

**Returns**

the structured result with parsed output

---

### `interactStructured`

```java
default StructuredAgentResult<T> interactStructured(@NonNull AgenticContext context)
```

Interacts with the agent with an existing context and returns a structured result.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | the conversation context |

**Returns**

the structured result with parsed output

---

### `interactStructured`

```java
StructuredAgentResult<T> interactStructured(
        @NonNull AgenticContext context, @Nullable TraceMetadata trace)
```

Interacts with the agent with an existing context and returns a structured result with trace
metadata.

This is the main structured method that all other structured overloads delegate to.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | the conversation context |
| `trace` | optional trace metadata (overrides agent-level configuration) |

**Returns**

the structured result with parsed output

