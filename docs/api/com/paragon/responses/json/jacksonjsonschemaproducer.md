# :material-database: JacksonJsonSchemaProducer

`com.paragon.responses.json.JacksonJsonSchemaProducer` &nbsp;·&nbsp; **Record**

---

Produces JSON schemas compatible with OpenAI's function calling API.

When using strict mode, OpenAI requires:

  
- `type: "object"`
- `properties: {...`}
- `required: [...]` - array of all property names
- `additionalProperties: false`

## Methods

### `collectIds`

```java
private void collectIds(Object node, Map<String, Map<String, Object>> idToSchema)
```

Pass 1: walks the entire schema tree and registers every sub-schema that carries an `id`
field (jackson-module-jsonSchema URN style, e.g. `"urn:jsonschema:com:example:Foo"`).

---

### `resolveRefs`

```java
private void resolveRefs(
      Map<String, Object> node,
      Map<String, Map<String, Object>> idToSchema,
      Set<String> resolving)
```

Pass 2: replaces every `{"$ref": "urn:..."`} node with a deep copy of the referenced
schema, and strips `id` and `$schema` from every node (unsupported by OpenAI).

`resolving` tracks URNs currently on the DFS stack. When a `$ref` points to a
URN that is already being resolved (circular reference), we replace it with an empty-object
fallback instead of recursing, breaking the cycle. OpenAI strict mode cannot represent
recursive schemas, so the fallback is the only safe option.

---

### `deepCopy`

```java
private Map<String, Object> deepCopy(Map<String, Object> source)
```

Shallow-enough deep copy: new HashMap at every level, preserving leaf values.

---

### `addRequiredProperties`

```java
private void addRequiredProperties(Map<String, Object> schema)
```

Recursively adds `required` arrays to object schemas and sets `additionalProperties: false`. OpenAI strict mode requires all properties to be listed in the
`required` array.
