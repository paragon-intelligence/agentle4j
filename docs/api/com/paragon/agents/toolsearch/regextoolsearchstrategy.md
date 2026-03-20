# :material-code-braces: RegexToolSearchStrategy

`com.paragon.agents.toolsearch.RegexToolSearchStrategy` &nbsp;·&nbsp; **Class**

Implements `ToolSearchStrategy`

---

A tool search strategy that uses regex patterns to match tools by name and description.

The strategy generates regex patterns from the input query by splitting it into words and
matching each word (case-insensitively) against tool names and descriptions. A tool is considered
a match if **any** query word appears in its name or description.

Results are limited to `.maxResults` matches.

### Example

```java
ToolSearchStrategy strategy = new RegexToolSearchStrategy(5);
List> matches = strategy.search("weather forecast", allTools);
// Returns tools whose name/description contains "weather" or "forecast"
```

**See Also**

- `ToolSearchStrategy`

*Since: 1.0*

## Methods

### `RegexToolSearchStrategy`

```java
public RegexToolSearchStrategy(int maxResults)
```

Creates a new regex tool search strategy.

**Parameters**

| Name | Description |
|------|-------------|
| `maxResults` | maximum number of tools to return |

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if maxResults is less than 1 |

---

### `RegexToolSearchStrategy`

```java
public RegexToolSearchStrategy()
```

Creates a new regex tool search strategy with a default limit of 5.

---

### `maxResults`

```java
public int maxResults()
```

Returns the maximum number of results this strategy returns.

**Returns**

the max results limit

