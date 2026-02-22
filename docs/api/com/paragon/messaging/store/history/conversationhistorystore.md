# :material-approximately-equal: ConversationHistoryStore

`com.paragon.messaging.store.history.ConversationHistoryStore` &nbsp;·&nbsp; **Interface**

---

Interface for storing and retrieving conversation history.

Implementations provide persistence for conversation messages, enabling AI agents to maintain
context across multiple user interactions. Messages are stored per user ID (typically the
WhatsApp phone number).

### Features

  
- Per-user message storage and retrieval
- Configurable history limits (count and age)
- Support for building AI context from history
- Automatic cleanup of expired messages

### Usage Example

```java
// Create in-memory store
ConversationHistoryStore store = InMemoryConversationHistoryStore.create(100);
// Or create Redis-backed store
ConversationHistoryStore store = RedisConversationHistoryStore.create(redisClient);
// Add messages to history
store.addMessage(userId, Message.user("Hello!"));
store.addMessage(userId, Message.assistant("Hi! How can I help?"));
// Get recent history for AI context
List history = store.getHistory(userId, 20, Duration.ofHours(24));
// Clear user's history
store.clearHistory(userId);
```

**See Also**

- `InMemoryConversationHistoryStore`
- `RedisConversationHistoryStore`

*Since: 2.1*

## Methods

### `addMessage`

```java
void addMessage(@NonNull String userId, @NonNull Message message)
```

Adds a message to the user's conversation history.

Messages are stored with a timestamp for age-based filtering. If the store has a maximum
capacity, older messages may be evicted.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | the user's unique identifier (e.g., WhatsApp phone number) |
| `message` | the message to store (UserMessage or AssistantMessage) |

---

### `addMessages`

```java
default void addMessages(@NonNull String userId, @NonNull List<? extends Message> messages)
```

Adds multiple messages to the user's conversation history.

Messages are added in order (first message is oldest).

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | the user's unique identifier |
| `messages` | the messages to store |

---

### `getHistory`

```java
List<ResponseInputItem> getHistory(
      @NonNull String userId, int maxMessages, @NonNull Duration maxAge)
```

Retrieves the user's conversation history.

Returns messages in chronological order (oldest first), filtered by both count and age
limits. Messages older than `maxAge` are excluded, then the most recent `maxMessages` are returned.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | the user's unique identifier |
| `maxMessages` | maximum number of messages to return |
| `maxAge` | maximum age of messages to include |

**Returns**

list of messages as ResponseInputItem (for AgentContext)

---

### `getHistory`

```java
default @NonNull List<ResponseInputItem> getHistory(@NonNull String userId, int maxMessages)
```

Retrieves the user's conversation history with default age limit (24 hours).

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | the user's unique identifier |
| `maxMessages` | maximum number of messages to return |

**Returns**

list of messages as ResponseInputItem

---

### `getMessageCount`

```java
int getMessageCount(@NonNull String userId)
```

Gets the number of messages stored for a user.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | the user's unique identifier |

**Returns**

the message count

---

### `clearHistory`

```java
void clearHistory(@NonNull String userId)
```

Clears all conversation history for a specific user.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | the user's unique identifier |

---

### `clearAll`

```java
void clearAll()
```

Clears all conversation history for all users.

Use with caution - this removes all stored data.

---

### `cleanupExpired`

```java
int cleanupExpired(@NonNull Duration maxAge)
```

Removes messages older than the specified age from all users.

This method can be called periodically to clean up stale data. Some implementations may
perform this automatically.

**Parameters**

| Name | Description |
|------|-------------|
| `maxAge` | maximum age of messages to retain |

**Returns**

the number of messages removed

---

### `hasHistory`

```java
default boolean hasHistory(@NonNull String userId)
```

Checks if any history exists for the specified user.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | the user's unique identifier |

**Returns**

true if the user has stored messages

