# Package `com.paragon.agents.context`

> This docs was updated at: 2026-02-23

---

## :material-code-braces: Classs

| Name | Description |
|------|-------------|
| [`ContextManagementConfig`](contextmanagementconfig.md) | Configuration for context management in agents |
| [`SimpleTokenCounter`](simpletokencounter.md) | A simple token counter that uses character-based estimation |
| [`SlidingWindowStrategy`](slidingwindowstrategy.md) | A context window strategy that removes oldest messages to fit within the token limit |
| [`SummarizationStrategy`](summarizationstrategy.md) | A context window strategy that summarizes older messages when context exceeds the limit |

## :material-approximately-equal: Interfaces

| Name | Description |
|------|-------------|
| [`ContextWindowStrategy`](contextwindowstrategy.md) | Strategy interface for managing conversation context length |
| [`TokenCounter`](tokencounter.md) | Interface for counting tokens in conversation content |
