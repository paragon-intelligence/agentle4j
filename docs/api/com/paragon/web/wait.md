# :material-database: Wait

> This docs was updated at: 2026-02-23

`com.paragon.web.Wait` &nbsp;·&nbsp; **Record**

---

Action to wait for an element to be visible.

## Methods

### `forSelector`

```java
public static Wait forSelector(@NonNull String selector, int milliseconds)
```

Creates a Wait action.

**Parameters**

| Name | Description |
|------|-------------|
| `selector` | Query selector for the element to wait for |
| `milliseconds` | Timeout in milliseconds |

**Returns**

A new Wait instance

---

### `forSelector`

```java
public static Wait forSelector(@NonNull String selector)
```

Creates a Wait action with a default timeout of 30 seconds.

**Parameters**

| Name | Description |
|------|-------------|
| `selector` | Query selector for the element to wait for |

**Returns**

A new Wait instance with 30 second timeout

