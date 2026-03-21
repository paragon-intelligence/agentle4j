# Package `com.paragon.agents.toolsearch`

> This docs was updated at: 2026-03-21

---

## :material-code-braces: Classs

| Name | Description |
|------|-------------|
| [`BM25ToolSearchStrategy`](bm25toolsearchstrategy.md) | A tool search strategy that uses BM25 (Best Matching 25) scoring to rank tools by relevance |
| [`EmbeddingToolSearchStrategy`](embeddingtoolsearchstrategy.md) | A tool search strategy that uses embedding vectors for semantic similarity matching |
| [`RegexToolSearchStrategy`](regextoolsearchstrategy.md) | A tool search strategy that uses regex patterns to match tools by name and description |
| [`ToolRegistry`](toolregistry.md) | A container for tools that supports both eager (always included) and deferred (search-discoverabl… |

## :material-approximately-equal: Interfaces

| Name | Description |
|------|-------------|
| [`ToolSearchStrategy`](toolsearchstrategy.md) | Strategy for dynamically selecting which tools to include in an LLM API call |
