# :material-code-braces: MessageContentDeserializer

> This docs was updated at: 2026-03-21

`com.paragon.responses.json.MessageContentDeserializer` &nbsp;·&nbsp; **Class**

Extends `ValueDeserializer<MessageContent>`

---

Custom deserializer for `MessageContent` that is tolerant of plain string values.

The Responses API primarily uses discriminated union objects with a `type` field. Some
providers and internal helpers, however, may represent simple text content as a bare JSON
string. This deserializer accepts both formats:

  
- If the node is a string, it is wrapped as a `Text` instance.
- If the node is an object with a `type` field, normal polymorphic resolution is used.
