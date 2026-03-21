# :material-code-braces: ToolRegistry

> This docs was updated at: 2026-03-21

`com.paragon.agents.toolsearch.ToolRegistry` &nbsp;·&nbsp; **Class**

---

A container for tools that supports both eager (always included) and deferred (search-discoverable)
tools.

This mirrors Anthropic's `defer_loading` concept: eager tools are sent in every API call,
while deferred tools are only included when a `ToolSearchStrategy` determines they are
relevant to the user's input.

The registry is used by the Agent to build each API payload:

```java
ToolRegistry registry = ToolRegistry.builder()
    .strategy(new BM25ToolSearchStrategy(5))
    .eagerTool(criticalTool)        // always in every API call
    .deferredTool(rarelyUsedTool)   // only when search finds it relevant
    .deferredTools(List.of(tool1, tool2, tool3))
    .build();
Agent agent = Agent.builder()
    .name("Assistant")
    .model("openai/gpt-4o")
    .instructions("You help with everything")
    .responder(responder)
    .toolRegistry(registry)
    .build();
```

**See Also**

- `ToolSearchStrategy`

*Since: 1.0*

## Methods

### `builder`

```java
public static @NonNull Builder builder()
```

Creates a new ToolRegistry builder.

**Returns**

a new builder

---

### `resolveTools`

```java
public @NonNull List<FunctionTool<?>> resolveTools(@NonNull String query)
```

Resolves which tools to include in the next API call.

Returns all eager tools plus any deferred tools that the search strategy deems relevant
to the query. Deduplication is handled — a tool will not appear twice even if it would be
returned by both eager and search.

**Parameters**

| Name | Description |
|------|-------------|
| `query` | the user's input text |

**Returns**

the combined list of relevant tools

---

### `allTools`

```java
public @NonNull List<FunctionTool<?>> allTools()
```

Returns all tools (both eager and deferred).

This is used by the tool store to register all tools for execution, since the LLM might
reference a previously discovered tool.

**Returns**

all registered tools

---

### `eagerTools`

```java
public @NonNull List<FunctionTool<?>> eagerTools()
```

Returns the eager tools (always included in every API call).

**Returns**

unmodifiable list of eager tools

---

### `deferredTools`

```java
public @NonNull List<FunctionTool<?>> deferredTools()
```

Returns the deferred tools (only included via search).

**Returns**

unmodifiable list of deferred tools

---

### `strategy`

```java
public @NonNull ToolSearchStrategy strategy()
```

Returns the search strategy used by this registry.

**Returns**

the search strategy

---

### `strategy`

```java
public @NonNull Builder strategy(@NonNull ToolSearchStrategy strategy)
```

Sets the search strategy for discovering deferred tools.

**Parameters**

| Name | Description |
|------|-------------|
| `strategy` | the search strategy |

**Returns**

this builder

---

### `eagerTool`

```java
public @NonNull Builder eagerTool(@NonNull FunctionTool<?> tool)
```

Adds a tool that is always included in every API call (eager loading).

**Parameters**

| Name | Description |
|------|-------------|
| `tool` | the tool to always include |

**Returns**

this builder

---

### `eagerTools`

```java
public @NonNull Builder eagerTools(@NonNull List<? extends FunctionTool<?>> tools)
```

Adds multiple eager tools.

**Parameters**

| Name | Description |
|------|-------------|
| `tools` | the tools to always include |

**Returns**

this builder

---

### `deferredTool`

```java
public @NonNull Builder deferredTool(@NonNull FunctionTool<?> tool)
```

Adds a tool that is only included when the search strategy finds it relevant (deferred
loading).

**Parameters**

| Name | Description |
|------|-------------|
| `tool` | the tool to defer |

**Returns**

this builder

---

### `deferredTools`

```java
public @NonNull Builder deferredTools(@NonNull List<? extends FunctionTool<?>> tools)
```

Adds multiple deferred tools.

**Parameters**

| Name | Description |
|------|-------------|
| `tools` | the tools to defer |

**Returns**

this builder

---

### `build`

```java
public @NonNull ToolRegistry build()
```

Builds the ToolRegistry.

**Returns**

a new ToolRegistry

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalStateException` | if deferred tools are present but no strategy was set |

