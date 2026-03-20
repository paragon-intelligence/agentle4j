# :material-database: Scroll

`com.paragon.web.Scroll` &nbsp;·&nbsp; **Record**

---

Action to scroll the page or a specific element.

## Methods

### `of`

```java
public static Scroll of(
      @NonNull String selector, @NonNull ScrollDirection direction, int amount)
```

Creates a Scroll action.

**Parameters**

| Name | Description |
|------|-------------|
| `selector` | Query selector for the element to scroll |
| `direction` | The direction to scroll |
| `amount` | The amount to scroll in pixels (0-1000) |

**Returns**

A new Scroll instance

---

### `down`

```java
public static Scroll down(@NonNull String selector, int amount)
```

Creates a Scroll action that scrolls down.

**Parameters**

| Name | Description |
|------|-------------|
| `selector` | Query selector for the element to scroll |
| `amount` | The amount to scroll in pixels |

**Returns**

A new Scroll instance that scrolls down

---

### `up`

```java
public static Scroll up(@NonNull String selector, int amount)
```

Creates a Scroll action that scrolls up.

**Parameters**

| Name | Description |
|------|-------------|
| `selector` | Query selector for the element to scroll |
| `amount` | The amount to scroll in pixels |

**Returns**

A new Scroll instance that scrolls up

---

### `left`

```java
public static Scroll left(@NonNull String selector, int amount)
```

Creates a Scroll action that scrolls left.

**Parameters**

| Name | Description |
|------|-------------|
| `selector` | Query selector for the element to scroll |
| `amount` | The amount to scroll in pixels |

**Returns**

A new Scroll instance that scrolls left

---

### `right`

```java
public static Scroll right(@NonNull String selector, int amount)
```

Creates a Scroll action that scrolls right.

**Parameters**

| Name | Description |
|------|-------------|
| `selector` | Query selector for the element to scroll |
| `amount` | The amount to scroll in pixels |

**Returns**

A new Scroll instance that scrolls right

