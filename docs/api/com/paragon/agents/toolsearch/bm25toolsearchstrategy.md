# :material-code-braces: BM25ToolSearchStrategy

> This docs was updated at: 2026-03-21

`com.paragon.agents.toolsearch.BM25ToolSearchStrategy` &nbsp;·&nbsp; **Class**

Implements `ToolSearchStrategy`

---

A tool search strategy that uses BM25 (Best Matching 25) scoring to rank tools by relevance.

BM25 is a widely-used information retrieval algorithm that computes relevance scores based on
term frequency (TF) and inverse document frequency (IDF). It handles:

  
- **Term frequency saturation** — repeated terms have diminishing returns
- **Document length normalization** — short, focused descriptions aren't penalized
- **Inverse document frequency** — rare terms are weighted higher

### Example

```java
ToolSearchStrategy strategy = new BM25ToolSearchStrategy(5);
List> ranked = strategy.search("search database records", allTools);
// Returns up to 5 most relevant tools, ranked by BM25 score
```

**See Also**

- `ToolSearchStrategy`

*Since: 1.0*

## Methods

### `BM25ToolSearchStrategy`

```java
public BM25ToolSearchStrategy(int maxResults, double k1, double b)
```

Creates a new BM25 strategy with custom parameters.

**Parameters**

| Name | Description |
|------|-------------|
| `maxResults` | maximum number of tools to return |
| `k1` | term frequency saturation parameter (typically 1.2–2.0) |
| `b` | document length normalization factor (0.0 = no normalization, 1.0 = full) |

---

### `BM25ToolSearchStrategy`

```java
public BM25ToolSearchStrategy(int maxResults)
```

Creates a new BM25 strategy with default parameters (k1=1.5, b=0.75).

**Parameters**

| Name | Description |
|------|-------------|
| `maxResults` | maximum number of tools to return |

---

### `BM25ToolSearchStrategy`

```java
public BM25ToolSearchStrategy()
```

Creates a new BM25 strategy with default parameters and a limit of 5.

---

### `maxResults`

```java
public int maxResults()
```

Returns the maximum number of results this strategy returns.

**Returns**

the max results limit

