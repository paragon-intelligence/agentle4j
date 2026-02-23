# :material-code-braces: ToolChoiceDeserializer

> This docs was updated at: 2026-02-23

`com.paragon.responses.spec.ToolChoiceDeserializer` &nbsp;·&nbsp; **Class**

Extends `JsonDeserializer<ToolChoice>`

---

Custom deserializer for `ToolChoice` to handle polymorphic deserialization.

The OpenAI API returns tool_choice as either:

  
- A string: "none", "auto", or "required" (mapped to `ToolChoiceMode`)
- An object with "mode" and "tools" fields (mapped to `AllowedTools`)
