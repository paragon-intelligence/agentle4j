# :material-database: OpenRouterModelPricing

`com.paragon.responses.openrouter.OpenRouterModelPricing` &nbsp;·&nbsp; **Record**

---

Pricing information for an OpenRouter model.

Prices are in USD per token. The OpenRouter API returns these as strings to handle arbitrary
precision.

## Methods

### `calculateCost`

```java
public @Nullable BigDecimal calculateCost(int inputTokens, int outputTokens)
```

Calculates the total cost for a request given token counts.

**Parameters**

| Name | Description |
|------|-------------|
| `inputTokens` | number of input/prompt tokens |
| `outputTokens` | number of output/completion tokens |

**Returns**

total cost in USD, or null if pricing data is invalid

---

### `promptAsBigDecimal`

```java
public @Nullable BigDecimal promptAsBigDecimal()
```

Returns the prompt cost as a BigDecimal.

---

### `completionAsBigDecimal`

```java
public @Nullable BigDecimal completionAsBigDecimal()
```

Returns the completion cost as a BigDecimal.
