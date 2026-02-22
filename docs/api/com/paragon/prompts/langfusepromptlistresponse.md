# :material-code-braces: LangfusePromptListResponse

`com.paragon.prompts.LangfusePromptListResponse` &nbsp;·&nbsp; **Class**

---

Response from the Langfuse prompts list API (v2).

Represents the paginated response from `GET /api/public/v2/prompts`.

*Since: 1.0*

## Methods

### `LangfusePromptListResponse`

```java
public LangfusePromptListResponse()
```

Default constructor for Jackson deserialization.

---

### `getData`

```java
public List<PromptMeta> getData()
```

Returns the list of prompt metadata entries.

**Returns**

list of prompt metadata

---

### `getMeta`

```java
public PageMeta getMeta()
```

Returns pagination metadata.

**Returns**

page metadata

---

### `PromptMeta`

```java
public PromptMeta()
```

Default constructor for Jackson deserialization.

---

### `getName`

```java
public String getName()
```

Returns the prompt name (used as promptId).

**Returns**

prompt name

---

### `getType`

```java
public String getType()
```

Returns the prompt type ("text" or "chat").

**Returns**

prompt type

---

### `getVersions`

```java
public List<Integer> getVersions()
```

Returns available versions.

**Returns**

list of version numbers

---

### `getLabels`

```java
public List<String> getLabels()
```

Returns labels assigned to this prompt.

**Returns**

list of labels

---

### `getTags`

```java
public List<String> getTags()
```

Returns tags assigned to this prompt.

**Returns**

list of tags

---

### `getLastUpdatedAt`

```java
public String getLastUpdatedAt()
```

Returns the last updated timestamp.

**Returns**

ISO 8601 timestamp string

---

### `PageMeta`

```java
public PageMeta()
```

Default constructor for Jackson deserialization.
