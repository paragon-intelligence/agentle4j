# :material-code-braces: BlueprintRegistry

`com.paragon.agents.BlueprintRegistry` &nbsp;·&nbsp; **Class**

---

Thread-safe global registry for named `InteractableBlueprint` instances.

Allows blueprints to be registered by a string ID and resolved at deserialization time using
the `source: registry` discriminator in YAML/JSON.

### Usage Example

```java
// At startup, register blueprints
BlueprintRegistry.register("clinico-geral", clinicoGeralBlueprint);
// In YAML, reference by ID:
target:
  source: registry
  id: clinico-geral
```

Follows the same pattern as `GuardrailRegistry` and `PromptProviderRegistry`.

*Since: 1.0*

## Methods

### `register`

```java
public static void register(@NonNull String id, @NonNull InteractableBlueprint blueprint)
```

Registers a blueprint with the given ID.

**Parameters**

| Name | Description |
|------|-------------|
| `id` | the unique identifier |
| `blueprint` | the blueprint to register |

---

### `get`

```java
public static @Nullable InteractableBlueprint get(@NonNull String id)
```

Retrieves a registered blueprint by ID.

**Parameters**

| Name | Description |
|------|-------------|
| `id` | the blueprint ID |

**Returns**

the blueprint, or null if not registered

---

### `contains`

```java
public static boolean contains(@NonNull String id)
```

Returns whether a blueprint with the given ID is registered.

**Parameters**

| Name | Description |
|------|-------------|
| `id` | the blueprint ID |

**Returns**

true if registered, false otherwise

---

### `unregister`

```java
public static void unregister(@NonNull String id)
```

Removes a registered blueprint by ID.

**Parameters**

| Name | Description |
|------|-------------|
| `id` | the blueprint ID to remove |

---

### `clear`

```java
public static void clear()
```

Removes all registered blueprints. Useful for testing.

---

### `registeredIds`

```java
public static Set<String> registeredIds()
```

Returns a snapshot of all registered IDs.

**Returns**

an unmodifiable set of registered IDs

