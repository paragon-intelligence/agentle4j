# :material-code-braces: PartialJsonParser

> This docs was updated at: 2026-02-23

`com.paragon.responses.streaming.PartialJsonParser` &nbsp;·&nbsp; **Class**

---

A lenient JSON parser that attempts to parse incomplete JSON strings. Used for real-time partial
parsing during structured output streaming.

The parser "completes" incomplete JSON by:

  
- Closing unclosed strings with quotes
- Adding missing closing braces and brackets
- Handling trailing commas

**Note:** The target class must have all fields as `@Nullable` or use wrapper types
(e.g., `Integer` instead of `int`) to accept partially-filled objects.

## Methods

### `PartialJsonParser`

```java
public PartialJsonParser(@NonNull ObjectMapper objectMapper, @NonNull Class<T> targetType)
```

Creates a new PartialJsonParser for the given type.

**Parameters**

| Name | Description |
|------|-------------|
| `objectMapper` | the ObjectMapper to use for parsing |
| `targetType` | the class to parse into |

---

### `parsePartial`

```java
public @Nullable T parsePartial(@NonNull String incompleteJson)
```

Attempts to parse partial/incomplete JSON, returning an instance with available fields.

**Parameters**

| Name | Description |
|------|-------------|
| `incompleteJson` | the incomplete JSON string (may be cut off mid-stream) |

**Returns**

a partially-filled instance, or null if not enough data to parse

---

### `parseAsMap`

```java
public static @Nullable Map<String, Object> parseAsMap(
      @NonNull ObjectMapper objectMapper, @NonNull String incompleteJson)
```

Static utility method to parse incomplete JSON directly to a Map.

This is the "zero-class" approach - no need to define a partial class with nullable fields.

**Parameters**

| Name | Description |
|------|-------------|
| `objectMapper` | the ObjectMapper to use |
| `incompleteJson` | the incomplete JSON string |

**Returns**

a Map containing available fields, or null if not parseable yet

---

### `completeJsonStatic`

```java
private static @Nullable String completeJsonStatic(@NonNull String partial)
```

Static version of completeJson for use in parseAsMap.

**Parameters**

| Name | Description |
|------|-------------|
| `partial` | the incomplete JSON |

**Returns**

a potentially valid JSON string, or null if not completeable

---

### `completeJson`

```java
private @Nullable String completeJson(@NonNull String partial)
```

Attempts to "complete" an incomplete JSON string by closing any open structures.

**Parameters**

| Name | Description |
|------|-------------|
| `partial` | the incomplete JSON |

**Returns**

a potentially valid JSON string, or null if not completeable

