# :material-approximately-equal: Memory

`com.paragon.agents.Memory` &nbsp;·&nbsp; **Interface**

---

Interface for agent long-term memory storage with user isolation.

Memory provides persistent storage for agent knowledge that persists across sessions. All
operations are scoped by userId to ensure data isolation between users.

Memory is exposed to the agent as tools via `MemoryTool`. The userId is passed securely
by the developer in `Agent.interact(input, context, userId)`, NOT by the LLM, to prevent
prompt injection attacks.

Example usage:

```java
Memory storage = InMemoryMemory.create();
Agent agent = Agent.builder()
    .addMemoryTools(storage)  // Adds memory as tools
    .build();
// userId passed by developer - secure!
agent.interact("Remember my preference", context, "user-123");
```

**See Also**

- `MemoryEntry`
- `MemoryTool`
- `InMemoryMemory`

*Since: 1.0*

## Methods

### `add`

```java
void add(@NonNull String userId, @NonNull MemoryEntry entry)
```

Adds a new memory entry for a user.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | the user ID (for isolation) |
| `entry` | the memory to add |

---

### `add`

```java
default void add(@NonNull String userId, @NonNull String content)
```

Adds a memory with just content for a user.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | the user ID |
| `content` | the memory content |

---

### `retrieve`

```java
List<MemoryEntry> retrieve(@NonNull String userId, @NonNull String query, int limit)
```

Retrieves memories relevant to a query for a user.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | the user ID |
| `query` | the search query |
| `limit` | maximum number of memories to return |

**Returns**

list of relevant memories, ordered by relevance

---

### `update`

```java
void update(@NonNull String userId, @NonNull String id, @NonNull MemoryEntry entry)
```

Updates an existing memory entry for a user.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | the user ID |
| `id` | the memory ID to update |
| `entry` | the new memory content |

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if memory with ID doesn't exist for this user |

---

### `delete`

```java
boolean delete(@NonNull String userId, @NonNull String id)
```

Deletes a memory by ID for a user.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | the user ID |
| `id` | the memory ID to delete |

**Returns**

true if memory was deleted, false if not found

---

### `all`

```java
List<MemoryEntry> all(@NonNull String userId)
```

Returns all stored memories for a user.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | the user ID |

**Returns**

list of all memories for this user

---

### `size`

```java
int size(@NonNull String userId)
```

Returns the number of stored memories for a user.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | the user ID |

**Returns**

memory count for this user

---

### `clear`

```java
void clear(@NonNull String userId)
```

Clears all memories for a user.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | the user ID |

---

### `clearAll`

```java
void clearAll()
```

Clears all memories for all users.
