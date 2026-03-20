# :material-code-braces: ArtifactStoreTool

`com.paragon.harness.tools.ArtifactStoreTool` &nbsp;·&nbsp; **Class**

---

Exposes an `ArtifactStore` as `FunctionTool`s for read/write/list operations.

Usage:

```java
ArtifactStore store = FilesystemArtifactStore.create(Path.of("./artifacts"));
Agent agent = Agent.builder()
    .addTools(ArtifactStoreTool.all(store).toArray(new FunctionTool[0]))
    .build();
```

*Since: 1.0*

## Methods

### `all`

```java
public static @NonNull List<FunctionTool<?>> all(@NonNull ArtifactStore store)
```

Creates all artifact store tools (read, write, list).

**Parameters**

| Name | Description |
|------|-------------|
| `store` | the artifact store to expose |

**Returns**

list of tools

