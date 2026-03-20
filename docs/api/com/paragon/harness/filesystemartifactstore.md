# :material-code-braces: FilesystemArtifactStore

`com.paragon.harness.FilesystemArtifactStore` &nbsp;·&nbsp; **Class**

Implements `ArtifactStore`

---

Filesystem-backed implementation of `ArtifactStore`.

Artifacts are stored as files in the pattern:

```java
baseDir/{name}/{version}.txt
```

Versions are epoch-millisecond timestamps. The "latest" version is the file with the
highest timestamp. Atomic writes are used to prevent partial file reads.

**See Also**

- `ArtifactStore`

*Since: 1.0*

## Methods

### `create`

```java
public static @NonNull FilesystemArtifactStore create(@NonNull Path baseDir)
```

Creates a FilesystemArtifactStore rooted at `baseDir`.

**Parameters**

| Name | Description |
|------|-------------|
| `baseDir` | the directory to store artifacts in |

**Returns**

a new store instance

