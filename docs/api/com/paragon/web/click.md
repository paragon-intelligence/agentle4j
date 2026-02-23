# :material-database: Click

> This docs was updated at: 2026-02-23

`com.paragon.web.Click` &nbsp;·&nbsp; **Record**

---

Action to click on an element identified by a CSS selector.

## Methods

### `of`

```java
public static Click of(@NonNull String selector)
```

Creates a Click action with default values.

**Parameters**

| Name | Description |
|------|-------------|
| `selector` | Query selector to find the element by |

**Returns**

A new Click instance

---

### `all`

```java
public static Click all(@NonNull String selector)
```

Creates a Click action that clicks all matching elements.

**Parameters**

| Name | Description |
|------|-------------|
| `selector` | Query selector to find the elements by |

**Returns**

A new Click instance that clicks all matches

