# :material-database: Location

> This docs was updated at: 2026-02-23

`com.paragon.web.Location` &nbsp;·&nbsp; **Record**

---

Represents a geographic location for geo-targeting requests.

## Methods

### `of`

```java
public static Location of(@NonNull String country)
```

Creates a Location with just a country code.

**Parameters**

| Name | Description |
|------|-------------|
| `country` | The country code |

**Returns**

A new Location instance

---

### `of`

```java
public static Location of(@NonNull String country, @NonNull String language)
```

Creates a Location with country and language codes.

**Parameters**

| Name | Description |
|------|-------------|
| `country` | The country code |
| `language` | The language code |

**Returns**

A new Location instance

