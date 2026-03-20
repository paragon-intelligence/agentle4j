# :material-approximately-equal: MemoryStore

`com.paragon.responses.tools.memory.MemoryStore` &nbsp;·&nbsp; **Interface**

---

Interface for memory storage operations.

## Methods

### `store`

```java
String store(@NonNull String key, @NonNull String value)
```

Stores a value associated with a key.

**Parameters**

| Name | Description |
|------|-------------|
| `key` | The key to store |
| `value` | The value to store |

**Returns**

A message indicating the result of the operation

---

### `retrieve`

```java
String retrieve(@NonNull String key)
```

Retrieves a value associated with a key.

**Parameters**

| Name | Description |
|------|-------------|
| `key` | The key to retrieve |

**Returns**

The value, or null if not found

---

### `delete`

```java
String delete(@NonNull String key)
```

Deletes a value associated with a key.

**Parameters**

| Name | Description |
|------|-------------|
| `key` | The key to delete |

**Returns**

A message indicating the result of the operation

