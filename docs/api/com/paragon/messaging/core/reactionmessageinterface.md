# :material-approximately-equal: ReactionMessageInterface

> This docs was updated at: 2026-03-21

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

**Returns**

the message ID to react to

---

### `emoji`

```java
Optional<String> emoji()
```

**Returns**

optional emoji (empty means removal)

---

### `isRemoval`

```java
default boolean isRemoval()
```

**Returns**

true if this is a reaction removal

