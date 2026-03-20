# :material-database: Article

`com.paragon.web.Article` &nbsp;·&nbsp; **Record**

---

## Methods

### `create`

```java
public static @NonNull WebExtractor create(@NonNull Responder responder)
```

Creates a new WebExtractor with default settings.

---

### `create`

```java
public static @NonNull WebExtractor create(@NonNull Responder responder, @NonNull String model)
```

Creates a new WebExtractor with a specific model.

---

### `extract`

```java
public @NonNull ExtractionResult extract(@NonNull ExtractPayload payload)
```

Extracts content from web pages without structured output parsing. Returns HTML and Markdown
content for all URLs.

**Parameters**

| Name | Description |
|------|-------------|
| `payload` | the extraction configuration |

**Returns**

the extraction result

---

### `extract`

```java
public <T> ExtractionResult.Structured<T> extract(ExtractPayload.Structured<T> payload)
```

Extracts structured data from web pages using LLM processing. The extracted content is parsed
into the specified output type.

**Parameters**

| Name | Description |
|------|-------------|
| `payload` | the structured extraction configuration |
| `<T>` | the output type |

**Returns**

the structured extraction result

