# :material-code-braces: AgenticFileParser

`com.paragon.parsing.AgenticFileParser` &nbsp;·&nbsp; **Class**

---

Agentic file parser that uses LLM to convert documents to markdown.

**Virtual Thread Design:** Uses synchronous API optimized for Java 21+ virtual threads.

## Methods

### `parse`

```java
public @NonNull MarkdownResult parse(@NonNull Path path) throws IOException
```

Parses a file from the given path into markdown.

**Parameters**

| Name | Description |
|------|-------------|
| `path` | the path to the file |

**Returns**

the markdown result

**Throws**

| Type | Condition |
|------|-----------|
| `IOException` | if the file cannot be read |

---

### `parse`

```java
public @NonNull MarkdownResult parse(@NonNull URI uri) throws IOException
```

Parses a file from a URI into markdown.

**Parameters**

| Name | Description |
|------|-------------|
| `uri` | the URI of the file |

**Returns**

the markdown result

**Throws**

| Type | Condition |
|------|-----------|
| `IOException` | if the file cannot be read |

---

### `parse`

```java
public @NonNull MarkdownResult parse(@NonNull String base64) throws IOException
```

Parses a base64-encoded file into markdown.

**Parameters**

| Name | Description |
|------|-------------|
| `base64` | the base64-encoded file content |

**Returns**

the markdown result

**Throws**

| Type | Condition |
|------|-----------|
| `IOException` | if processing fails |

---

### `parse`

```java
public @NonNull MarkdownResult parse(@NonNull File file) throws IOException
```

Parses a File object into markdown.

**Parameters**

| Name | Description |
|------|-------------|
| `file` | the file to parse |

**Returns**

the markdown result

**Throws**

| Type | Condition |
|------|-----------|
| `IOException` | if processing fails |

