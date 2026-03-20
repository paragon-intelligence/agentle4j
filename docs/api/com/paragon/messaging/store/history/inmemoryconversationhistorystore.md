# :material-code-braces: InMemoryConversationHistoryStore

`com.paragon.messaging.store.history.InMemoryConversationHistoryStore` &nbsp;·&nbsp; **Class**

Implements `ConversationHistoryStore`

---

In-memory implementation of `ConversationHistoryStore`.

Stores conversation history in memory with configurable per-user limits and LRU eviction.
Suitable for development, testing, and single-instance deployments where persistence across
restarts is not required.

### Features

  
- Thread-safe with fine-grained locking per user
- LRU eviction when per-user limit is reached
- Timestamped messages for age-based filtering
- Automatic cleanup of expired messages

### Usage Example

```java
// Create with default settings (100 messages per user)
ConversationHistoryStore store = InMemoryConversationHistoryStore.create();
// Create with custom per-user limit
ConversationHistoryStore store = InMemoryConversationHistoryStore.create(50);
// Create with builder for full customization
ConversationHistoryStore store = InMemoryConversationHistoryStore.builder()
    .maxMessagesPerUser(200)
    .build();
```

### Thread Safety

This implementation is thread-safe. Operations on different users can proceed concurrently.
Operations on the same user are serialized using per-user read-write locks for optimal concurrent
read performance.

**See Also**

- `ConversationHistoryStore`

*Since: 2.1*

## Methods

### `create`

```java
public static InMemoryConversationHistoryStore create()
```

Creates a new store with default settings (100 messages per user).

**Returns**

a new InMemoryConversationHistoryStore

---

### `create`

```java
public static InMemoryConversationHistoryStore create(int maxMessagesPerUser)
```

Creates a new store with the specified per-user message limit.

**Parameters**

| Name | Description |
|------|-------------|
| `maxMessagesPerUser` | maximum messages to store per user |

**Returns**

a new InMemoryConversationHistoryStore

---

### `builder`

```java
public static Builder builder()
```

Creates a builder for customizing the store.

**Returns**

a new builder

---

### `getUserCount`

```java
public int getUserCount()
```

Returns the current number of users with stored history.

**Returns**

the number of users

---

### `getMaxMessagesPerUser`

```java
public int getMaxMessagesPerUser()
```

Returns the maximum messages stored per user.

**Returns**

the per-user limit

---

### `maxMessagesPerUser`

```java
public Builder maxMessagesPerUser(int max)
```

Sets the maximum number of messages to store per user.

When this limit is reached, older messages are evicted (LRU).

**Parameters**

| Name | Description |
|------|-------------|
| `max` | the maximum messages per user |

**Returns**

this builder

---

### `build`

```java
public InMemoryConversationHistoryStore build()
```

Builds the configured InMemoryConversationHistoryStore.

**Returns**

the store instance

