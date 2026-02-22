# :material-code-braces: FileSearchToolCall

`com.paragon.responses.spec.FileSearchToolCall` &nbsp;·&nbsp; **Class**

Extends `ToolCall` &nbsp;·&nbsp; Implements `Item`, `ResponseOutput`

---

The results of a file search tool call. See the file search guide for more
information.

## Methods

### `FileSearchToolCall`

```java
public FileSearchToolCall(
      @NonNull String id,
      @NonNull List<String> queries,
      @NonNull FileSearchToolCallStatus status,
      @Nullable List<FileSearchToolCallResult> fileSearchToolCallResultList)
```

@param id The unique ID of the file search tool call.

**Parameters**

| Name | Description |
|------|-------------|
| `queries` | The queries used to search for files. |
| `status` | The status of the file search tool call. One of in_progress, searching, incomplete or failed, |
| `fileSearchToolCallResultList` | The results of the file search tool call. |

