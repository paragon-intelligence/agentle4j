# :material-code-braces: WebSearchToolCall

> This docs was updated at: 2026-03-21

`com.paragon.responses.spec.WebSearchToolCall` &nbsp;·&nbsp; **Class**

Extends `ToolCall` &nbsp;·&nbsp; Implements `Item`, `ResponseOutput`

---

The results of a web search tool call. See the web search guide for more information.

## Methods

### `WebSearchToolCall`

```java
public WebSearchToolCall(@NonNull WebAction action, @NonNull String id, @NonNull String status)
```

**Parameters**

| Name | Description |
|------|-------------|
| `action` | An object describing the specific action taken in this web search call. Includes details on how the model used the web (search, open_page, find). |
| `id` | The unique ID of the web search tool call. |
| `status` | The status of the web search tool call. |

