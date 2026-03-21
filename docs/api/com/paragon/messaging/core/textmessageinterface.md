# :material-approximately-equal: TextMessageInterface

> This docs was updated at: 2026-03-21

`com.paragon.messaging.core.TextMessageInterface` &nbsp;·&nbsp; **Interface**

Extends `OutboundMessage`

---

Sealed sub-interface for text messages.

This interface is part of the `OutboundMessage` sealed hierarchy and is implemented by
concrete text message classes.

*Since: 2.1*

## Methods

### `body`

```java
String body()
```

**Returns**

the message body text

---

### `previewUrl`

```java
boolean previewUrl()
```

**Returns**

whether to generate URL previews

