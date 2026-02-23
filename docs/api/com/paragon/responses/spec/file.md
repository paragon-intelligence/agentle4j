# :material-database: File

> This docs was updated at: 2026-02-23

`com.paragon.responses.spec.File` &nbsp;·&nbsp; **Record**

---

A file input to the model.

## Methods

### `fromUrl`

```java
public static File fromUrl(String fileUrl)
```

Creates a file from a URL.

---

### `fromUrl`

```java
public static File fromUrl(String fileUrl, String filename)
```

Creates a file from a URL with filename.

---

### `fromFileId`

```java
public static File fromFileId(String fileId)
```

Creates a file from a file ID.

---

### `fromFileId`

```java
public static File fromFileId(String fileId, String filename)
```

Creates a file from a file ID with filename.

---

### `fromBase64`

```java
public static File fromBase64(String base64Data, String filename)
```

Creates a file from base64-encoded data.
