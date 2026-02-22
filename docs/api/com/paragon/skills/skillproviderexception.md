# :material-code-braces: SkillProviderException

`com.paragon.skills.SkillProviderException` &nbsp;·&nbsp; **Class**

Extends `RuntimeException`

---

Exception thrown when a skill cannot be loaded or parsed.

This exception is thrown by `SkillProvider` implementations when skill retrieval fails
due to I/O errors, parsing errors, or validation failures.

**See Also**

- `SkillProvider`

*Since: 1.0*

## Methods

### `SkillProviderException`

```java
public SkillProviderException(@NonNull String message)
```

Creates a new SkillProviderException.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the error message |

---

### `SkillProviderException`

```java
public SkillProviderException(@NonNull String message, @Nullable Throwable cause)
```

Creates a new SkillProviderException with a cause.

**Parameters**

| Name | Description |
|------|-------------|
| `message` | the error message |
| `cause` | the underlying cause |

---

### `SkillProviderException`

```java
public SkillProviderException(@NonNull String skillId, @NonNull String message)
```

Creates a new SkillProviderException for a specific skill.

**Parameters**

| Name | Description |
|------|-------------|
| `skillId` | the skill identifier that failed |
| `message` | the error message |

---

### `SkillProviderException`

```java
public SkillProviderException(
      @NonNull String skillId, @NonNull String message, @Nullable Throwable cause)
```

Creates a new SkillProviderException for a specific skill with a cause.

**Parameters**

| Name | Description |
|------|-------------|
| `skillId` | the skill identifier that failed |
| `message` | the error message |
| `cause` | the underlying cause |

---

### `skillId`

```java
public @Nullable String skillId()
```

Returns the skill identifier that caused the exception, if available.

**Returns**

the skill ID, or null if not applicable

