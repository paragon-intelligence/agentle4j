# :material-database: TraceMetadata

> This docs was updated at: 2026-02-23

`com.paragon.responses.TraceMetadata` &nbsp;·&nbsp; **Record**

---

Metadata for OpenRouter trace/observability support.

Enables integration with observability platforms like Langfuse by sending trace information
directly in API requests. All fields are optional to support partial configuration.

### Usage Example

```java
TraceMetadata trace = TraceMetadata.builder()
    .traceId("workflow_12345")
    .traceName("Document Processing Pipeline")
    .spanName("Summarization Step")
    .generationName("Generate Summary")
    .environment("production")
    .build();
```

**See Also**

- `<a href="https://openrouter.ai/docs/guides/features/broadcast/langfuse">OpenRouter Langfuse Documentation</a>`

*Since: 1.0*

## Methods

### `isEmpty`

```java
public boolean isEmpty()
```

Checks if all fields in this trace metadata are null.

**Returns**

true if all fields are null, false otherwise

---

### `orNullIfEmpty`

```java
public @Nullable TraceMetadata orNullIfEmpty()
```

Returns this metadata or null if all fields are null.

**Returns**

this metadata if not empty, null otherwise

---

### `traceId`

```java
public Builder traceId(String traceId)
```

Sets the trace ID to group multiple requests into a single trace.

**Parameters**

| Name | Description |
|------|-------------|
| `traceId` | the trace ID |

**Returns**

this builder

---

### `traceName`

```java
public Builder traceName(String traceName)
```

Sets the custom name displayed in the Langfuse trace list.

**Parameters**

| Name | Description |
|------|-------------|
| `traceName` | the trace name |

**Returns**

this builder

---

### `spanName`

```java
public Builder spanName(String spanName)
```

Sets the name for intermediate spans in the hierarchy.

**Parameters**

| Name | Description |
|------|-------------|
| `spanName` | the span name |

**Returns**

this builder

---

### `generationName`

```java
public Builder generationName(String generationName)
```

Sets the name for the LLM generation observation.

**Parameters**

| Name | Description |
|------|-------------|
| `generationName` | the generation name |

**Returns**

this builder

---

### `parentSpanId`

```java
public Builder parentSpanId(String parentSpanId)
```

Sets the parent observation ID to link to an existing span in the trace hierarchy.

**Parameters**

| Name | Description |
|------|-------------|
| `parentSpanId` | the parent span ID |

**Returns**

this builder

---

### `environment`

```java
public Builder environment(String environment)
```

Sets the environment (e.g., "production", "staging").

**Parameters**

| Name | Description |
|------|-------------|
| `environment` | the environment |

**Returns**

this builder

---

### `metadata`

```java
public Builder metadata(Map<String, Object> metadata)
```

Sets custom metadata as key-value pairs for filtering and analysis.

**Parameters**

| Name | Description |
|------|-------------|
| `metadata` | the metadata map |

**Returns**

this builder

