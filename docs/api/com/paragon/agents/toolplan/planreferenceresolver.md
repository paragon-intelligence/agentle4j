# :material-code-braces: PlanReferenceResolver

`com.paragon.agents.toolplan.PlanReferenceResolver` &nbsp;·&nbsp; **Class**

---

Resolves `$ref:step_id` and `$ref:step_id.field.nested` references in tool plan step
argument strings.

This is a stateless utility class. All methods are static.

### Reference Syntax

  
- `"$ref:step_id"` — replaced with the full output string of the referenced step. If
      the output is valid JSON, it is inserted unquoted. If plain text, it is inserted quoted.
- `"$ref:step_id.field"` — the step output is parsed as JSON and the field is
      extracted. The extracted value is inserted (unquoted if JSON, quoted if string).
- `"$ref:step_id.field.nested"` — dot-separated paths for deep JSON field access,
      translated to JSON Pointer (e.g., `/field/nested`).

## Methods

### `compile`

```java
static final Pattern REF_PATTERN =
      Pattern.compile("\"\\$ref:([a-zA-Z0-9_]+)(?:\\.([a-zA-Z0-9_.]+))?\"")
```

Matches: "$ref:step_id" or "$ref:step_id.field.nested" The entire match includes the
surrounding quotes. Group 1 = step_id, Group 2 = optional dot-separated field path.

---

### `resolve`

```java
public static @NonNull String resolve(
      @NonNull String arguments, @NonNull Map<String, String> resolvedOutputs)
```

Resolves all `$ref` references in the given arguments string.

**Parameters**

| Name | Description |
|------|-------------|
| `arguments` | the raw JSON arguments string from a `ToolPlanStep` |
| `resolvedOutputs` | map of step_id to output string for each completed step |

**Returns**

the arguments string with all references replaced

**Throws**

| Type | Condition |
|------|-----------|
| `ToolPlanException` | if a reference points to an unresolved step |

---

### `extractDependencies`

```java
public static @NonNull Set<String> extractDependencies(@NonNull String arguments)
```

Extracts dependency step IDs from the arguments string by scanning for `$ref:step_id`
patterns.

**Parameters**

| Name | Description |
|------|-------------|
| `arguments` | the raw JSON arguments string |

**Returns**

set of step IDs that this arguments string depends on

---

### `extractField`

```java
private static String extractField(String stepId, String output, String fieldPath)
```

Extracts a field from a JSON output string using a dot-separated path.

**Returns**

the field value formatted for insertion into JSON

---

### `formatOutput`

```java
private static String formatOutput(String output)
```

Formats a step output for insertion into a JSON arguments string. If the output is valid JSON,
it is inserted as-is. If plain text, it is wrapped in quotes.
