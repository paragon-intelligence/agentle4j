# :material-approximately-equal: TextMessageInterface

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

@return the message body text

---

### `previewUrl`

```java
boolean previewUrl()
```

@return whether to generate URL previews
