# :material-code-braces: ScrollActionDeserializer

`com.paragon.responses.json.ScrollActionDeserializer` &nbsp;·&nbsp; **Class**

Extends `ValueDeserializer<ScrollAction>`

---

Custom deserializer for ScrollAction to handle @JsonUnwrapped Coordinate.

Jackson doesn't support @JsonUnwrapped with @JsonCreator (which records implicitly use), so we
need a custom deserializer to read the unwrapped x and y fields.
