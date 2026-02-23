# :material-code-braces: MemoryTool

> This docs was updated at: 2026-02-23

`com.paragon.agents.MemoryTool` &nbsp;·&nbsp; **Class**

---

Memory exposed as FunctionTools for agent use.

This class provides 4 tools for memory operations. The userId is set securely by the developer
(not by the LLM) to prevent prompt injection attacks.

Usage:

```java
Memory storage = InMemoryMemory.create();
Agent agent = Agent.builder()
    .addMemoryTools(storage)  // Adds all 4 memory tools
    .build();
```

*Since: 1.0*

## Methods

### `all`

```java
public static @NonNull List<FunctionTool<?>> all(@NonNull Memory memory)
```

Creates all four memory tools.

**Parameters**

| Name | Description |
|------|-------------|
| `memory` | the memory storage |

**Returns**

list of all memory tools

