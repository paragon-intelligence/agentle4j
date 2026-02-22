# :material-approximately-equal: ReactionMessageInterface

`com.paragon.messaging.core.ReactionMessageInterface` &nbsp;·&nbsp; **Interface**

Extends `OutboundMessage`

---

Sealed sub-interface for reaction messages (emoji reactions).

This interface is part of the `OutboundMessage` sealed hierarchy and is implemented by
concrete reaction message classes.

*Since: 2.1*

## Methods

### `messageId`

```java
String messageId()
```

@return the message ID to react to

---

### `emoji`

```java
Optional<String> emoji()
```

@return optional emoji (empty means removal)

---

### `isRemoval`

```java
default boolean isRemoval()
```

@return true if this is a reaction removal
