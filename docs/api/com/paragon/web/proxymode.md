# :material-format-list-bulleted-type: ProxyMode

`com.paragon.web.ProxyMode` &nbsp;·&nbsp; **Enum**

---

Proxy mode for web extraction requests.

## Methods

### `BASIC`

```java
BASIC("basic"),

  /** Stealth proxy with anti-detection measures. */
  STEALTH("stealth"),

  /** Automatically choose the best proxy mode. */
  AUTO("auto")
```

Basic proxy without special handling.

---

### `STEALTH`

```java
STEALTH("stealth"),

  /** Automatically choose the best proxy mode. */
  AUTO("auto")
```

Stealth proxy with anti-detection measures.

---

### `AUTO`

```java
AUTO("auto")
```

Automatically choose the best proxy mode.
