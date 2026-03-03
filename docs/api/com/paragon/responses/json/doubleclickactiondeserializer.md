# :material-code-braces: DoubleClickActionDeserializer

> This docs was updated at: 2026-03-03


`com.paragon.responses.json.DoubleClickActionDeserializer` &nbsp;·&nbsp; **Class**

Extends `JsonDeserializer<DoubleClickAction>`

---

Custom deserializer for DoubleClickAction to handle @JsonUnwrapped Coordinate.

Jackson doesn't support @JsonUnwrapped with @JsonCreator (which records implicitly use), so we
need a custom deserializer to read the unwrapped x and y fields.
