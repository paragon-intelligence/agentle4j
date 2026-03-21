# :material-code-braces: EmbeddingToolSearchStrategy

> This docs was updated at: 2026-03-21

`com.paragon.agents.toolsearch.EmbeddingToolSearchStrategy` &nbsp;·&nbsp; **Class**

Implements `ToolSearchStrategy`

---

A tool search strategy that uses embedding vectors for semantic similarity matching.

This strategy embeds both the query and tool metadata (name + description) using an `EmbeddingProvider`, then ranks tools by cosine similarity to the query embedding. This enables
semantic matching — e.g., matching "temperature outside" to a tool named "get_weather" even though
they share no keywords.

**Note:** This strategy makes an API call to the embedding provider on each search, so it
has higher latency than `BM25ToolSearchStrategy` or `RegexToolSearchStrategy`. For
best performance, tool embeddings are computed once and cached.

### Example

```java
EmbeddingProvider provider = new OpenRouterEmbeddingProvider(apiKey);
ToolSearchStrategy strategy = new EmbeddingToolSearchStrategy(provider, "text-embedding-3-small", 5);
Agent agent = Agent.builder()
    .name("Assistant")
    .toolRegistry(ToolRegistry.builder()
        .strategy(strategy)
        .deferredTools(hundredsOfTools)
        .build())
    .build();
```

**See Also**

- `ToolSearchStrategy`
- `EmbeddingProvider`

*Since: 1.0*

## Methods

### `EmbeddingToolSearchStrategy`

```java
public EmbeddingToolSearchStrategy(
      @NonNull EmbeddingProvider embeddingProvider, @NonNull String model, int maxResults)
```

Creates a new embedding-based tool search strategy.

**Parameters**

| Name | Description |
|------|-------------|
| `embeddingProvider` | the provider for creating embeddings |
| `model` | the embedding model identifier (e.g., "text-embedding-3-small") |
| `maxResults` | maximum number of tools to return |

---

### `EmbeddingToolSearchStrategy`

```java
public EmbeddingToolSearchStrategy(
      @NonNull EmbeddingProvider embeddingProvider, @NonNull String model)
```

Creates a new embedding-based tool search strategy with a default limit of 5.

**Parameters**

| Name | Description |
|------|-------------|
| `embeddingProvider` | the provider for creating embeddings |
| `model` | the embedding model identifier |

---

### `maxResults`

```java
public int maxResults()
```

Returns the maximum number of results this strategy returns.

**Returns**

the max results limit

---

### `invalidateCache`

```java
public synchronized void invalidateCache()
```

Invalidates the cached tool embeddings, forcing recomputation on the next search.

Call this if the set of deferred tools changes after construction.
