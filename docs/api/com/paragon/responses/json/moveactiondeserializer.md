# :material-code-braces: MoveActionDeserializer

> This docs was updated at: 2026-03-05



`com.paragon.responses.json.MoveActionDeserializer` &nbsp;·&nbsp; **Class**

Extends `JsonDeserializer<MoveAction>`

---

Custom deserializer for MoveAction to handle @JsonUnwrapped Coordinate.

Jackson doesn't support @JsonUnwrapped with @JsonCreator (which records implicitly use), so we
need a custom deserializer to read the unwrapped x and y fields.
