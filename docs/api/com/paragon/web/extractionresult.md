# :material-code-braces: ExtractionResult

`com.paragon.web.ExtractionResult` &nbsp;·&nbsp; **Class**

---

Result of a web content extraction operation. Contains the extracted HTML, Markdown, and
optionally structured output.

## Methods

### `requireOutput`

```java
public @NonNull T requireOutput()
```

Get the output, throwing if not present.

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalStateException` | if output is null |

