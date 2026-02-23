# :material-code-braces: ScrollActionDeserializer

> This docs was updated at: 2026-02-23

`com.paragon.responses.json.ScrollActionDeserializer` &nbsp;·&nbsp; **Class**

Extends `JsonDeserializer<ScrollAction>`

---

Custom deserializer for ScrollAction to handle @JsonUnwrapped Coordinate.

Jackson doesn't support @JsonUnwrapped with @JsonCreator (which records implicitly use), so we
need a custom deserializer to read the unwrapped x and y fields.
