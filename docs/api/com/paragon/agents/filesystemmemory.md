# :material-code-braces: FilesystemMemory

> This docs was updated at: 2026-03-09












`com.paragon.agents.FilesystemMemory` &nbsp;·&nbsp; **Class**

Implements `Memory`

---

Durable filesystem-backed implementation of `Memory`.

Persists each user's memories as a JSON file at `baseDir/{userId}.json`. Survives JVM restarts and enables long-running, multi-session agent workflows.

Thread-safe via per-user read/write locks. Writes are atomic (write to temp file, then rename).

**See Also**

- `Memory`
- `MemoryEntry`
- `InMemoryMemory`
- `JdbcMemory`

*Since: 1.0*

## Factory Methods

### `create(Path)`

```java
public static @NonNull FilesystemMemory create(@NonNull Path baseDir)
```

Creates a FilesystemMemory storing data under `baseDir`. The directory is created if it does not exist.

**Parameters**

- `baseDir` — directory where per-user JSON files will be stored

**Returns**

a new FilesystemMemory instance

---

### `create(Path, ObjectMapper)`

```java
public static @NonNull FilesystemMemory create(@NonNull Path baseDir, @NonNull ObjectMapper objectMapper)
```

Creates a FilesystemMemory with a custom Jackson `ObjectMapper`.

**Parameters**

- `baseDir` — directory where per-user JSON files will be stored
- `objectMapper` — the Jackson mapper to use for serialization

**Returns**

a new FilesystemMemory instance

## Usage

```java
Memory memory = FilesystemMemory.create(Path.of("/var/agent-data/memory"));

Agent agent = Agent.builder()
    .name("Assistant")
    .model("openai/gpt-4o")
    .responder(responder)
    .addMemoryTools(memory)
    .build();

// userId is passed by the developer — not by the LLM — to prevent prompt injection
agent.interact("Remember that I prefer dark mode", context, "user-123");

// Memory survives process restarts
Memory reloaded = FilesystemMemory.create(Path.of("/var/agent-data/memory"));
List<MemoryEntry> entries = reloaded.all("user-123"); // restored from disk
```

## Notes

- File naming: userId is sanitized (non-alphanumeric characters replaced with `_`) before use as a filename
- Atomicity: writes go to a `.tmp` file and are renamed atomically to prevent partial reads
- Concurrency: per-user `ReentrantReadWriteLock` allows concurrent access across different users
