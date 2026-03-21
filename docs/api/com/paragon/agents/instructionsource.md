# :material-approximately-equal: InstructionSource

> This docs was updated at: 2026-03-21

`com.paragon.agents.InstructionSource` &nbsp;·&nbsp; **Interface**

---

A sealed interface representing the source of an agent's instructions.

Instructions can come from three sources:

  
- `Inline` — raw text embedded directly in the blueprint
- `FileRef` — a path to a text file on disk
- `ProviderRef` — a reference to a prompt in a `PromptProvider` (e.g., Langfuse)

### JSON/YAML Format

**Inline (backward-compatible)** — a plain string is automatically wrapped:

```java
"instructions": "You are a helpful assistant."
```

**File reference:**

```java
"instructions": {
  "source": "file",
  "path": "./prompts/support-agent.txt"
}
```

**Provider reference (e.g., Langfuse):**

```java
"instructions": {
  "source": "provider",
  "providerId": "langfuse",
  "promptId": "support-agent-v2",
  "filters": { "label": "production" }
}
```

**See Also**

- `PromptProviderRegistry`

*Since: 1.0*

## Methods

### `resolve`

```java
String resolve()
```

Resolves this instruction source to its actual text content.

For `Inline`, returns the text directly. For `FileRef`, reads the file. For
`ProviderRef`, fetches from the registered `PromptProvider`.

**Returns**

the resolved instruction text

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalStateException` | if resolution fails (file not found, provider not registered, etc.) |

