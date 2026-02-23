# :material-code-braces: ReactionMessage

> This docs was updated at: 2026-02-23

`com.paragon.messaging.whatsapp.payload.ReactionMessage` &nbsp;·&nbsp; **Class**

Extends `AbstractInboundMessage`

---

Inbound reaction message from WhatsApp webhook.

## Methods

### `isRemoval`

```java
public boolean isRemoval()
```

Returns true if this is a reaction removal (empty emoji).
