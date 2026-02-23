# :material-approximately-equal: LocationMessageInterface

> This docs was updated at: 2026-02-23

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

@return latitude coordinate

---

### `longitude`

```java
double longitude()
```

@return longitude coordinate

---

### `name`

```java
Optional<String> name()
```

@return optional location name

---

### `address`

```java
Optional<String> address()
```

@return optional location address

---

### `toCoordinatesString`

```java
default @NonNull String toCoordinatesString()
```

@return formatted coordinates string
