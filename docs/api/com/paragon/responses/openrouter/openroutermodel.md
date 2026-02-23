# :material-database: OpenRouterModel

> This docs was updated at: 2026-02-23

`com.paragon.responses.openrouter.OpenRouterModel` &nbsp;·&nbsp; **Record**

---

Represents a model from the OpenRouter API.

Only includes fields relevant for pricing and identification. Other fields from the API are
ignored.

## Methods

### `calculateCost`

```java
public @Nullable BigDecimal calculateCost(int inputTokens, int outputTokens)
```

Calculates the cost for a request with this model.

**Parameters**

| Name | Description |
|------|-------------|
| `inputTokens` | number of input tokens |
| `outputTokens` | number of output tokens |

**Returns**

cost in USD, or null if pricing is invalid

---

### `costPer1kInputTokens`

```java
public @Nullable BigDecimal costPer1kInputTokens()
```

Returns the cost per 1000 input tokens for display purposes.

---

### `costPer1kOutputTokens`

```java
public @Nullable BigDecimal costPer1kOutputTokens()
```

Returns the cost per 1000 output tokens for display purposes.
