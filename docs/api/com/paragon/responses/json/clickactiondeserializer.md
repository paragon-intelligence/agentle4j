# :material-code-braces: ClickActionDeserializer

`com.paragon.responses.json.ClickActionDeserializer` &nbsp;·&nbsp; **Class**

Extends `ValueDeserializer<ClickAction>`

---

Custom deserializer for ClickAction to handle @JsonUnwrapped Coordinate.

Jackson doesn't support @JsonUnwrapped with @JsonCreator (which records implicitly use), so we
need a custom deserializer to read the unwrapped x and y fields.
