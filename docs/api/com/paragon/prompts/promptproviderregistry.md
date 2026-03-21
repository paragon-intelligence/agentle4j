# :material-code-braces: PromptProviderRegistry

> This docs was updated at: 2026-03-21

`com.paragon.prompts.PromptProviderRegistry` &nbsp;·&nbsp; **Class**

---

Global registry for named `PromptProvider` instances.

Similar to `com.paragon.agents.GuardrailRegistry`, this registry allows prompt providers
to be registered at application startup and referenced by ID from agent blueprints. This enables
agent definitions (JSON/YAML) to reference prompts stored in external systems like Langfuse
without embedding the provider configuration in the blueprint itself.

### Usage Example

```java
// At application startup — register providers
PromptProviderRegistry.register("langfuse",
    LangfusePromptProvider.builder()
        .httpClient(httpClient)
        .publicKey("pk-xxx")
        .secretKey("sk-xxx")
        .build());
PromptProviderRegistry.register("local",
    FilesystemPromptProvider.create(Path.of("./prompts")));
// Later — blueprints reference providers by ID
// "instructions": { "source": "provider", "providerId": "langfuse", "promptId": "my-prompt" }
PromptProvider provider = PromptProviderRegistry.get("langfuse");
Prompt prompt = provider.providePrompt("my-prompt");
```

**See Also**

- `PromptProvider`

*Since: 1.0*

## Methods

### `register`

```java
public static void register(@NonNull String id, @NonNull PromptProvider provider)
```

Registers a prompt provider with the given identifier.

If a provider is already registered with the same ID, it is silently replaced.

**Parameters**

| Name | Description |
|------|-------------|
| `id` | the unique identifier for this provider (e.g., "langfuse", "local", "db") |
| `provider` | the prompt provider instance |

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if id or provider is null |
| `IllegalArgumentException` | if id is empty |

---

### `get`

```java
public static @Nullable PromptProvider get(@NonNull String id)
```

Retrieves a registered prompt provider by its identifier.

**Parameters**

| Name | Description |
|------|-------------|
| `id` | the provider identifier |

**Returns**

the registered provider, or `null` if not found

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if id is null |

---

### `contains`

```java
public static boolean contains(@NonNull String id)
```

Checks whether a provider with the given ID is registered.

**Parameters**

| Name | Description |
|------|-------------|
| `id` | the provider identifier |

**Returns**

`true` if a provider is registered with this ID

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if id is null |

---

### `registeredIds`

```java
public static @NonNull Set<String> registeredIds()
```

Returns an unmodifiable view of all registered provider IDs.

**Returns**

the set of registered provider IDs

---

### `clear`

```java
public static void clear()
```

Removes all registered providers.

Primarily useful for testing.

---

### `unregister`

```java
public static void unregister(@NonNull String id)
```

Removes a specific provider registration.

**Parameters**

| Name | Description |
|------|-------------|
| `id` | the provider identifier to remove |

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if id is null |

