# :material-database: GeneratePdf

`com.paragon.web.GeneratePdf` &nbsp;·&nbsp; **Record**

---

Action to generate a PDF of the current page.

The PDF will be returned as a byte array.

## Methods

### `defaults`

```java
public static GeneratePdf defaults()
```

Creates a GeneratePdf action with default values.

**Returns**

A new GeneratePdf instance with Letter format, portrait orientation, and scale 1.0

---

### `of`

```java
public static GeneratePdf of(@NonNull PdfFormat format)
```

Creates a GeneratePdf action with the specified format.

**Parameters**

| Name | Description |
|------|-------------|
| `format` | The format of the PDF |

**Returns**

A new GeneratePdf instance

---

### `builder`

```java
public static Builder builder()
```

Creates a GeneratePdf action builder.

**Returns**

A new Builder instance

