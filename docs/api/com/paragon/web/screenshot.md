# :material-database: Screenshot

`com.paragon.web.Screenshot` &nbsp;·&nbsp; **Record**

---

Action to capture a screenshot of the current page or a specific element.

## Methods

### `of`

```java
public static Screenshot of(int quality)
```

Creates a Screenshot action with default values.

**Parameters**

| Name | Description |
|------|-------------|
| `quality` | The quality of the screenshot (1-100) |

**Returns**

A new Screenshot instance

---

### `fullPage`

```java
public static Screenshot fullPage(int quality)
```

Creates a full-page Screenshot action.

**Parameters**

| Name | Description |
|------|-------------|
| `quality` | The quality of the screenshot (1-100) |

**Returns**

A new full-page Screenshot instance

---

### `builder`

```java
public static Builder builder()
```

Creates a Screenshot action builder.

**Returns**

A new Builder instance

