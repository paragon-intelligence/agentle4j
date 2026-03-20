# :material-code-braces: DoubleClickActionDeserializer

`com.paragon.responses.json.DoubleClickActionDeserializer` &nbsp;·&nbsp; **Class**

Extends `ValueDeserializer<DoubleClickAction>`

---

Custom deserializer for DoubleClickAction to handle @JsonUnwrapped Coordinate.

Jackson doesn't support @JsonUnwrapped with @JsonCreator (which records implicitly use), so we
need a custom deserializer to read the unwrapped x and y fields.
