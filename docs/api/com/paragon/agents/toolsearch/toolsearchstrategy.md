# :material-approximately-equal: ToolSearchStrategy

`com.paragon.agents.toolsearch.ToolSearchStrategy` &nbsp;·&nbsp; **Interface**

---

Strategy for dynamically selecting which tools to include in an LLM API call.

When an agent has many tools, sending all of them in every API call wastes context window
tokens and can degrade tool selection accuracy. A `ToolSearchStrategy` solves this by
searching the available tools based on the user's input and returning only the most relevant
subset.

This is the framework-level equivalent of Anthropic's server-side tool search feature, but
works with **any LLM provider** (OpenAI, OpenRouter, local models) because the selection
happens client-side before the API call.

### Built-in Strategies

  
- `RegexToolSearchStrategy` — pattern-based matching on tool names/descriptions
- `BM25ToolSearchStrategy` — TF-IDF scoring for relevance ranking
- `EmbeddingToolSearchStrategy` — semantic similarity via embedding vectors

### Custom Strategies

```java
ToolSearchStrategy custom = (query, tools) -> tools.stream()
    .filter(t -> mySimilarityModel.score(query, t.getName()) > 0.7)
    .toList();
```

**See Also**

- `ToolRegistry`

*Since: 1.0*

## Methods

### `search`

```java
List<FunctionTool<?>> search(@NonNull String query, @NonNull List<FunctionTool<?>> allTools)
```

Searches the available tools and returns the subset most relevant to the query.

**Parameters**

| Name | Description |
|------|-------------|
| `query` | the user's input text used to determine tool relevance |
| `allTools` | all deferred tools available for selection |

**Returns**

the subset of tools to include in the API call (may be empty)

