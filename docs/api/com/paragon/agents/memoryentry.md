# :material-database: MemoryEntry

> This docs was updated at: 2026-02-23

`com.paragon.agents.MemoryEntry` &nbsp;·&nbsp; **Record**

---

Represents a single memory entry for long-term agent memory.

Memories are automatically injected into the first user message of each agent run, allowing
the agent to recall relevant context from previous sessions.

*Since: 1.0*

## Methods

### `of`

```java
public static @NonNull MemoryEntry of(@NonNull String content)
```

Creates a new memory entry with auto-generated ID and current timestamp.

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the memory content |

**Returns**

a new memory entry

---

### `of`

```java
public static @NonNull MemoryEntry of(
      @NonNull String content, @NonNull Map<String, Object> metadata)
```

Creates a new memory entry with metadata.

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the memory content |
| `metadata` | key-value metadata |

**Returns**

a new memory entry

---

### `withId`

```java
public static @NonNull MemoryEntry withId(@NonNull String id, @NonNull String content)
```

Creates a new memory entry with a specific ID.

**Parameters**

| Name | Description |
|------|-------------|
| `id` | the memory ID |
| `content` | the memory content |

**Returns**

a new memory entry

---

### `toPromptFormat`

```java
public @NonNull String toPromptFormat()
```

Returns a formatted string representation for injection into prompts.
