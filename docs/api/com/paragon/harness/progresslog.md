# :material-code-braces: ProgressLog

`com.paragon.harness.ProgressLog` &nbsp;·&nbsp; **Class**

---

Append-only log of work items for tracking agent progress across sessions.

Each entry records a work item with its status (DONE, FAILED, IN_PROGRESS) and optional
notes. The log is append-only to prevent accidental overwrites of completed work.

Use `FilesystemArtifactStore` or a custom store to persist the log across sessions.

Example usage:

```java
ProgressLog log = ProgressLog.create();
log.append("Analyzed requirements", Status.DONE, "Extracted 12 features");
log.append("Write database schema", Status.IN_PROGRESS, null);
// In another session:
List pending = log.byStatus(Status.IN_PROGRESS);
```

*Since: 1.0*

## Methods

### `of`

```java
public static @NonNull Entry of(
        @NonNull String description, @NonNull Status status, @Nullable String notes)
```

Creates an entry with an auto-generated ID and current timestamp.

---

### `create`

```java
public static @NonNull ProgressLog create()
```

Creates an empty progress log.

---

### `from`

```java
public static @NonNull ProgressLog from(@NonNull List<Entry> entries)
```

Creates a progress log from an existing list of entries (e.g., loaded from storage).

**Parameters**

| Name | Description |
|------|-------------|
| `entries` | previously persisted entries |

**Returns**

a new ProgressLog pre-populated with those entries

---

### `append`

```java
public @NonNull Entry append(
      @NonNull String description, @NonNull Status status, @Nullable String notes)
```

Appends a new entry to the log.

**Parameters**

| Name | Description |
|------|-------------|
| `description` | what was done |
| `status` | the status of the work item |
| `notes` | optional notes |

**Returns**

the entry that was added

---

### `all`

```java
public @NonNull List<Entry> all()
```

Returns all entries in append order.

**Returns**

unmodifiable view of all entries

---

### `byStatus`

```java
public @NonNull List<Entry> byStatus(@NonNull Status status)
```

Returns entries filtered by status.

**Parameters**

| Name | Description |
|------|-------------|
| `status` | the status to filter by |

**Returns**

list of entries with the given status

---

### `size`

```java
public int size()
```

Returns the total number of entries.

**Returns**

entry count

---

### `toSummary`

```java
public @NonNull String toSummary()
```

Returns a formatted summary of the log suitable for injection into agent context.

**Returns**

a multi-line string summarizing all entries

