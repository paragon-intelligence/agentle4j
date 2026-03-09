# :material-code-braces: MessageDeserializer

> This docs was updated at: 2026-03-09










`com.paragon.responses.json.MessageDeserializer` &nbsp;·&nbsp; **Class**

Extends `JsonDeserializer<Message>`

---

Custom deserializer for Message that uses the 'role' field to determine which concrete subclass
to instantiate. Also handles OutputMessage which has an 'id' field.
