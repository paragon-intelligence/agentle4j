# :material-database: JacksonJsonSchemaProducer

> This docs was updated at: 2026-02-23

`com.paragon.responses.json.JacksonJsonSchemaProducer` &nbsp;·&nbsp; **Record**

---

Produces JSON schemas compatible with OpenAI's function calling API.

When using strict mode, OpenAI requires:

  
- `type: "object"`
- `properties: {...`}
- `required: [...]` - array of all property names
- `additionalProperties: false`

## Methods

### `addRequiredProperties`

```java
private void addRequiredProperties(Map<String, Object> schema)
```

Recursively adds 'required' arrays to object schemas. OpenAI strict mode requires all
properties to be listed in the 'required' array.
