# :material-code-braces: SelfCorrectionConfig

> This docs was updated at: 2026-03-21

`com.paragon.harness.SelfCorrectionConfig` &nbsp;·&nbsp; **Class**

---

Configuration for the self-correction loop in `SelfCorrectingInteractable`.

Self-correction automatically retries a failed agent run by injecting the error
back as user context, giving the agent a chance to fix its own mistakes.

Example:

```java
SelfCorrectionConfig config = SelfCorrectionConfig.builder()
    .maxRetries(3)
    .retryOn(result -> result.isError() || result.isGuardrailFailed())
    .feedbackTemplate("Your previous response failed with: {error}. Please try again.")
    .build();
```

**See Also**

- `SelfCorrectingInteractable`

*Since: 1.0*

## Fields

### `DEFAULT_FEEDBACK_TEMPLATE`

```java
public static final String DEFAULT_FEEDBACK_TEMPLATE =
      "Your previous response failed with the following error:\n\n"
          + "
```

Default feedback template injected on failure.

## Methods

### `maxRetries`

```java
public int maxRetries()
```

Returns the maximum number of retry attempts.

**Returns**

max retries

---

### `retryOn`

```java
public @NonNull Predicate<AgentResult> retryOn()
```

Returns the predicate that decides whether a result should trigger a retry.

**Returns**

retry predicate

---

### `feedbackTemplate`

```java
public @NonNull String feedbackTemplate()
```

Returns the feedback template. Use `{error`} as a placeholder for the error message.

**Returns**

feedback template

---

### `formatFeedback`

```java
public @NonNull String formatFeedback(@NonNull String errorMessage)
```

Formats the error message into the feedback template.

**Parameters**

| Name | Description |
|------|-------------|
| `errorMessage` | the error message to inject |

**Returns**

the formatted feedback string

---

### `builder`

```java
public static @NonNull Builder builder()
```

Returns a new builder with sensible defaults:

  
- maxRetries = 3
- retryOn = any error result
- feedbackTemplate = `.DEFAULT_FEEDBACK_TEMPLATE`

**Returns**

a new builder

---

### `maxRetries`

```java
public @NonNull Builder maxRetries(int maxRetries)
```

Sets the maximum number of self-correction retries.

**Parameters**

| Name | Description |
|------|-------------|
| `maxRetries` | must be positive |

**Returns**

this builder

---

### `retryOn`

```java
public @NonNull Builder retryOn(@NonNull Predicate<AgentResult> retryOn)
```

Sets the predicate that decides whether to retry.

**Parameters**

| Name | Description |
|------|-------------|
| `retryOn` | predicate returning true when a retry should be attempted |

**Returns**

this builder

---

### `feedbackTemplate`

```java
public @NonNull Builder feedbackTemplate(@NonNull String feedbackTemplate)
```

Sets the feedback template. Use `{error`} where you want the error message injected.

**Parameters**

| Name | Description |
|------|-------------|
| `feedbackTemplate` | the template string |

**Returns**

this builder

---

### `build`

```java
public @NonNull SelfCorrectionConfig build()
```

Builds the configuration.
