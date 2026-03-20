# :material-approximately-equal: ArtifactStore

`com.paragon.harness.ArtifactStore` &nbsp;·&nbsp; **Interface**

---

Interface for reading and writing named artifacts (documents, scripts, reports) with versioning.

Artifacts are identified by a name and optionally a version. If no version is specified,
the latest version is returned. Implementations may persist artifacts to the filesystem, a
database, or an object store.

Example usage:

```java
ArtifactStore store = FilesystemArtifactStore.create(Path.of("./artifacts"));
// Write a new artifact (version is auto-assigned)
store.write("schema.sql", "CREATE TABLE users ...");
// Read the latest version
Optional schema = store.read("schema.sql");
// List all artifact names
List names = store.list();
```

*Since: 1.0*

## Methods

### `write`

```java
String write(@NonNull String name, @NonNull String content)
```

Writes an artifact, creating a new version.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the artifact name (e.g., "schema.sql", "feature-list.md") |
| `content` | the artifact content |

**Returns**

the version identifier assigned to this write

---

### `read`

```java
Optional<String> read(@NonNull String name)
```

Reads the latest version of an artifact.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the artifact name |

**Returns**

the content, or empty if the artifact does not exist

---

### `read`

```java
Optional<String> read(@NonNull String name, @NonNull String version)
```

Reads a specific version of an artifact.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the artifact name |
| `version` | the version identifier returned from `.write` |

**Returns**

the content for that version, or empty if not found

---

### `list`

```java
List<String> list()
```

Lists all artifact names in the store.

**Returns**

list of artifact names

---

### `versions`

```java
List<String> versions(@NonNull String name)
```

Lists all versions for an artifact, oldest first.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the artifact name |

**Returns**

list of version identifiers

---

### `delete`

```java
boolean delete(@NonNull String name)
```

Deletes all versions of an artifact.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the artifact name |

**Returns**

true if the artifact existed and was deleted

