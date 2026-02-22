# :material-code-braces: WebSearchToolCall

`com.paragon.responses.spec.WebSearchToolCall` &nbsp;·&nbsp; **Class**

Extends `ToolCall` &nbsp;·&nbsp; Implements `Item`, `ResponseOutput`

---

The results of a web search tool call. See the web search guide for more information.

## Methods

### `WebSearchToolCall`

```java
public WebSearchToolCall(@NonNull WebAction action, @NonNull String id, @NonNull String status)
```

@param action An object describing the specific action taken in this web search call. Includes
    details on how the model used the web (search, open_page, find).

**Parameters**

| Name | Description |
|------|-------------|
| `id` | The unique ID of the web search tool call. |
| `status` | The status of the web search tool call. |

