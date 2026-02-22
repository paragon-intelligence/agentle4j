# :material-approximately-equal: PromptStore

`com.paragon.prompts.PromptStore` &nbsp;·&nbsp; **Interface**

---

Store interface for persisting prompts to various storage backends.

This interface provides write operations for prompt management, complementing the read-only
`PromptProvider` interface. Implementations may persist prompts to databases, file systems,
or other storage mechanisms.

### Usage Examples

```java
// Database-backed store
PromptStore store = new DatabasePromptStore(dataSource);
store.save("greeting", Prompt.of("Hello, {{name}}!"));
// Later, delete the prompt
store.delete("greeting");
```

### Interface Segregation

The prompt management system follows the Interface Segregation Principle:

  
- `PromptProvider` - Read-only operations (retrieve, exists, list)
- `PromptStore` - Write operations (save, delete)

Implementations that need full CRUD capabilities can implement both interfaces.

**See Also**

- `PromptProvider`

*Since: 1.0*

## Methods

### `save`

```java
void save(@NonNull String promptId, @NonNull Prompt prompt)
```

Saves a prompt with the given identifier.

If a prompt with the same identifier already exists, it will be overwritten.

**Parameters**

| Name | Description |
|------|-------------|
| `promptId` | the unique identifier for the prompt |
| `prompt` | the prompt to save |

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if promptId or prompt is null |
| `PromptProviderException` | if the save operation fails |

---

### `save`

```java
default void save(@NonNull String promptId, @NonNull String content)
```

Saves a prompt with the given identifier from raw content.

Convenience method that creates a `Prompt` from the content string. If a prompt with
the same identifier already exists, it will be overwritten.

**Parameters**

| Name | Description |
|------|-------------|
| `promptId` | the unique identifier for the prompt |
| `content` | the raw prompt content (may contain template variables) |

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if promptId or content is null |
| `PromptProviderException` | if the save operation fails |

---

### `delete`

```java
void delete(@NonNull String promptId)
```

Deletes a prompt by its identifier.

If the prompt does not exist, this method does nothing (no-op).

**Parameters**

| Name | Description |
|------|-------------|
| `promptId` | the unique identifier for the prompt to delete |

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if promptId is null |
| `PromptProviderException` | if the delete operation fails |

