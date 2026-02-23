# :material-database: LangfusePromptResponse

> This docs was updated at: 2026-02-23

`com.paragon.prompts.LangfusePromptResponse` &nbsp;·&nbsp; **Record**

---

Response DTO for Langfuse prompt API.

Handles both "text" and "chat" prompt types from the Langfuse API.

*Since: 1.0*

## Fields

### `TYPE_TEXT`

```java
public static final String TYPE_TEXT = "text"
```

Constant for text prompt type.

---

### `TYPE_CHAT`

```java
public static final String TYPE_CHAT = "chat"
```

Constant for chat prompt type.

## Methods

### `isTextPrompt`

```java
public boolean isTextPrompt()
```

Returns whether this is a text prompt.

**Returns**

true if text prompt, false otherwise

---

### `isChatPrompt`

```java
public boolean isChatPrompt()
```

Returns whether this is a chat prompt.

**Returns**

true if chat prompt, false otherwise

---

### `getPromptContent`

```java
public @NonNull String getPromptContent()
```

Returns the prompt content as a string.

For text prompts, returns the prompt string directly. For chat prompts, concatenates all
message contents with newlines.

**Returns**

the prompt content as a string

---

### `getChatMessages`

```java
public @NonNull List<ChatMessage> getChatMessages()
```

Returns the chat messages if this is a chat prompt.

**Returns**

list of chat messages, or empty list if not a chat prompt

---

### `isPlaceholder`

```java
public boolean isPlaceholder()
```

Returns whether this is a placeholder message.

**Returns**

true if placeholder, false otherwise

