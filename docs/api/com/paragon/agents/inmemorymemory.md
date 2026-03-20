# :material-code-braces: InMemoryMemory

`com.paragon.agents.InMemoryMemory` &nbsp;·&nbsp; **Class**

Implements `Memory`

---

Thread-safe in-memory implementation of `Memory` with user isolation.

Memories are stored per-user in separate maps to ensure complete isolation. For production use
with large amounts of data or semantic search requirements, consider implementations backed by
vector databases.

**See Also**

- `Memory`
- `MemoryEntry`

*Since: 1.0*

## Methods

### `create`

```java
public static @NonNull InMemoryMemory create()
```

Creates a new empty in-memory storage.

**Returns**

a new InMemoryMemory instance

