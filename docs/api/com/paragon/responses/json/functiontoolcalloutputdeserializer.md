# :material-code-braces: FunctionToolCallOutputDeserializer

> This docs was updated at: 2026-03-21

`com.paragon.responses.json.FunctionToolCallOutputDeserializer` &nbsp;·&nbsp; **Class**

Extends `ValueDeserializer<FunctionToolCallOutput>`

---

Custom deserializer for `FunctionToolCallOutput`.

The wire format written by `FunctionToolCallOutputSerializer` encodes `output` as
a plain string (via `Text.toString()`). This deserializer wraps that string back into a
`Text` instance.

**Limitation:** `com.paragon.responses.spec.Image` and `com.paragon.responses.spec.File` outputs are serialized via their `toString()` which is not
reversible. Deserializing such outputs produces a `Text` wrapping the `toString()`
representation — structurally safe but semantically lossy.
