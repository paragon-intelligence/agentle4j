# :material-approximately-equal: LocationMessageInterface

`com.paragon.messaging.core.LocationMessageInterface` &nbsp;·&nbsp; **Interface**

Extends `OutboundMessage`

---

Sealed sub-interface for location messages.

This interface is part of the `OutboundMessage` sealed hierarchy and is implemented by
concrete location message classes.

*Since: 2.1*

## Methods

### `latitude`

```java
double latitude()
```

**Returns**

latitude coordinate

---

### `longitude`

```java
double longitude()
```

**Returns**

longitude coordinate

---

### `name`

```java
Optional<String> name()
```

**Returns**

optional location name

---

### `address`

```java
Optional<String> address()
```

**Returns**

optional location address

---

### `toCoordinatesString`

```java
default @NonNull String toCoordinatesString()
```

**Returns**

formatted coordinates string

