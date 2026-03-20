# :material-code-braces: FilesystemMemory

`com.paragon.agents.FilesystemMemory` &nbsp;·&nbsp; **Class**

Implements `Memory`

---

Durable filesystem-backed implementation of `Memory`.

Persists each user's memories as a JSON file at `baseDir/{userId`.json}. Survives JVM
restarts and enables long-running, multi-session agent workflows.

Thread-safe via per-user read/write locks. Writes are atomic (write to temp file, then rename).

Example usage:

```java
Memory memory = FilesystemMemory.create(Path.of("/var/agent-data/memory"));
Agent agent = Agent.builder()
    .addMemoryTools(memory)
    .build();
```

**See Also**

- `Memory`
- `MemoryEntry`

*Since: 1.0*

## Methods

### `create`

```java
public static @NonNull FilesystemMemory create(@NonNull Path baseDir)
```

Creates a FilesystemMemory storing data under `baseDir`.

**Parameters**

| Name | Description |
|------|-------------|
| `baseDir` | directory where per-user JSON files will be stored |

**Returns**

a new FilesystemMemory instance

---

### `create`

```java
public static @NonNull FilesystemMemory create(
      @NonNull Path baseDir, @NonNull ObjectMapper objectMapper)
```

Creates a FilesystemMemory with a custom ObjectMapper.

**Parameters**

| Name | Description |
|------|-------------|
| `baseDir` | directory where per-user JSON files will be stored |
| `objectMapper` | the Jackson mapper to use for serialization |

**Returns**

a new FilesystemMemory instance

