# :material-approximately-equal: PromptProvider

`com.paragon.prompts.PromptProvider` &nbsp;·&nbsp; **Interface**

---

Provider interface for retrieving prompts from various sources.

Implementations may fetch prompts from local files, remote services (e.g., Langfuse),
databases, or any other storage mechanism. This abstraction allows applications to centralize
prompt management and version control.

### Usage Examples

```java
// Filesystem provider
PromptProvider fileProvider = FilesystemPromptProvider.create(Path.of("./prompts"));
Prompt prompt = fileProvider.providePrompt("greeting.txt", null);
// Langfuse provider with version filter
PromptProvider langfuseProvider = LangfusePromptProvider.builder()
    .httpClient(okHttpClient)
    .publicKey("pk-xxx")
    .secretKey("sk-xxx")
    .build();
Prompt prompt = langfuseProvider.providePrompt("my-prompt", Map.of("version", "2"));
```

*Since: 1.0*

## Methods

### `providePrompt`

```java
Prompt providePrompt(@NonNull String promptId, @Nullable Map<String, String> filters)
```

Retrieves a prompt by its identifier.

**Parameters**

| Name | Description |
|------|-------------|
| `promptId` | the unique identifier for the prompt (e.g., file path, prompt name) |
| `filters` | optional key-value pairs to filter the prompt (e.g., version, label). Supported filters depend on the implementation. |

**Returns**

the retrieved `Prompt`

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if promptId is null |
| `PromptProviderException` | if the prompt cannot be retrieved |

---

### `providePrompt`

```java
default Prompt providePrompt(@NonNull String promptId)
```

Retrieves a prompt by its identifier without filters.

**Parameters**

| Name | Description |
|------|-------------|
| `promptId` | the unique identifier for the prompt |

**Returns**

the retrieved `Prompt`

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if promptId is null |
| `PromptProviderException` | if the prompt cannot be retrieved |

---

### `exists`

```java
boolean exists(@NonNull String promptId)
```

Checks if a prompt with the given identifier exists.

**Parameters**

| Name | Description |
|------|-------------|
| `promptId` | the unique identifier for the prompt |

**Returns**

`true` if the prompt exists, `false` otherwise

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if promptId is null |
| `PromptProviderException` | if the existence check fails |

---

### `listPromptIds`

```java
Set<String> listPromptIds()
```

Lists all available prompt identifiers.

**Returns**

an unmodifiable set of all available prompt identifiers

**Throws**

| Type | Condition |
|------|-----------|
| `PromptProviderException` | if the listing fails |

