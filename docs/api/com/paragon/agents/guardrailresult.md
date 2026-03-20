# :material-approximately-equal: GuardrailResult

`com.paragon.agents.GuardrailResult` &nbsp;·&nbsp; **Interface**

---

Result of a guardrail validation check.

Guardrails validate input before processing or output before returning to the user. This
sealed interface represents the two possible outcomes: passed or failed.

**See Also**

- `InputGuardrail`
- `OutputGuardrail`

*Since: 1.0*

## Methods

### `passed`

```java
static GuardrailResult passed()
```

Returns a passed result. Uses a cached singleton instance.

**Returns**

a passed result

---

### `failed`

```java
static GuardrailResult failed(String reason)
```

Returns a failed result with the given reason.

**Parameters**

| Name | Description |
|------|-------------|
| `reason` | the failure reason |

**Returns**

a failed result

---

### `isPassed`

```java
default boolean isPassed()
```

Checks if this result represents a successful validation.

**Returns**

true if passed, false if failed

---

### `isFailed`

```java
default boolean isFailed()
```

Checks if this result represents a failed validation.

**Returns**

true if failed, false if passed

