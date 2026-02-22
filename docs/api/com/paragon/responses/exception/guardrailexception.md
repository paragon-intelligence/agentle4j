# :material-code-braces: GuardrailException

`com.paragon.responses.exception.GuardrailException` &nbsp;·&nbsp; **Class**

Extends `AgentleException`

---

Exception thrown when a guardrail validation fails.

Provides guardrail-specific context:

  
- `.guardrailName()` - Which guardrail failed
- `.violationType()` - INPUT or OUTPUT
- `.reason()` - Human-readable reason for failure

Example usage:

```java
if (result.isError() && result.error() instanceof GuardrailException e) {
    log.warn("Blocked by {}: {}", e.guardrailName(), e.reason());
    if (e.violationType() == ViolationType.INPUT) {
        // Prompt user to rephrase
    }
}
```

## Methods

### `GuardrailException`

```java
INPUT,
    /** Output guardrail blocked the response after LLM call. */
    OUTPUT
  }

  /**
   * Creates a new GuardrailException.
   *
   * @param guardrailName the name of the guardrail that failed
   * @param violationType whether this was an input or output violation
   * @param reason the human-readable reason for failure
   */
  public GuardrailException(
      @Nullable String guardrailName,
      @NonNull ViolationType violationType,
      @NonNull String reason)
```

Input guardrail blocked the request before LLM call.

---

### `GuardrailException`

```java
OUTPUT
  }

  /**
   * Creates a new GuardrailException.
   *
   * @param guardrailName the name of the guardrail that failed
   * @param violationType whether this was an input or output violation
   * @param reason the human-readable reason for failure
   */
  public GuardrailException(
      @Nullable String guardrailName,
      @NonNull ViolationType violationType,
      @NonNull String reason)
```

Output guardrail blocked the response after LLM call.

---

### `GuardrailException`

```java
public GuardrailException(
      @Nullable String guardrailName,
      @NonNull ViolationType violationType,
      @NonNull String reason)
```

Creates a new GuardrailException.

**Parameters**

| Name | Description |
|------|-------------|
| `guardrailName` | the name of the guardrail that failed |
| `violationType` | whether this was an input or output violation |
| `reason` | the human-readable reason for failure |

---

### `inputViolation`

```java
public static GuardrailException inputViolation(@NonNull String reason)
```

Creates an input guardrail exception.

**Parameters**

| Name | Description |
|------|-------------|
| `reason` | the reason for failure |

**Returns**

a new GuardrailException

---

### `inputViolation`

```java
public static GuardrailException inputViolation(
      @NonNull String guardrailName, @NonNull String reason)
```

Creates an input guardrail exception with a name.

**Parameters**

| Name | Description |
|------|-------------|
| `guardrailName` | the guardrail name |
| `reason` | the reason for failure |

**Returns**

a new GuardrailException

---

### `outputViolation`

```java
public static GuardrailException outputViolation(@NonNull String reason)
```

Creates an output guardrail exception.

**Parameters**

| Name | Description |
|------|-------------|
| `reason` | the reason for failure |

**Returns**

a new GuardrailException

---

### `outputViolation`

```java
public static GuardrailException outputViolation(
      @NonNull String guardrailName, @NonNull String reason)
```

Creates an output guardrail exception with a name.

**Parameters**

| Name | Description |
|------|-------------|
| `guardrailName` | the guardrail name |
| `reason` | the reason for failure |

**Returns**

a new GuardrailException

---

### `guardrailName`

```java
public @Nullable String guardrailName()
```

Returns the name of the guardrail that failed.

**Returns**

the guardrail name, or null if anonymous

---

### `violationType`

```java
public @NonNull ViolationType violationType()
```

Returns whether this was an input or output violation.

**Returns**

the violation type

---

### `reason`

```java
public @NonNull String reason()
```

Returns the human-readable reason for failure.

**Returns**

the reason

