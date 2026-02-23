# :material-approximately-equal: InteractiveMessageInterface

> This docs was updated at: 2026-02-23

`com.paragon.messaging.core.InteractiveMessageInterface` &nbsp;·&nbsp; **Interface**

Extends `OutboundMessage`

---

Sealed sub-interface for interactive messages (buttons, lists, CTA URLs).

This interface is part of the `OutboundMessage` sealed hierarchy and is implemented by
concrete interactive message classes.

*Since: 2.1*

## Methods

### `body`

```java
String body()
```

@return the message body text
