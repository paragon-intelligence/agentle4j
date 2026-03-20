# :material-code-braces: JdbcMemory

`com.paragon.agents.JdbcMemory` &nbsp;·&nbsp; **Class**

Implements `Memory`

---

JDBC-backed durable implementation of `Memory`.

Stores memory entries in any JDBC-compatible database (PostgreSQL, MySQL, H2, SQLite, etc.).
The table is created automatically on first use if it does not exist.

Table schema (auto-created):

```java
CREATE TABLE IF NOT EXISTS agent_memory (
  id        VARCHAR(255) NOT NULL,
  user_id   VARCHAR(255) NOT NULL,
  timestamp TIMESTAMP    NOT NULL,
  content   TEXT         NOT NULL,
  metadata  TEXT         NOT NULL,
  PRIMARY KEY (id, user_id)
)
```

Example usage:

```java
DataSource ds = HikariDataSource(...);
Memory memory = JdbcMemory.create(ds);
Agent agent = Agent.builder()
    .addMemoryTools(memory)
    .build();
```

**See Also**

- `Memory`
- `MemoryEntry`

*Since: 1.0*

## Methods

### `create`

```java
public static @NonNull JdbcMemory create(@NonNull DataSource dataSource)
```

Creates a JdbcMemory using the default table name `agent_memory`.

**Parameters**

| Name | Description |
|------|-------------|
| `dataSource` | the JDBC data source |

**Returns**

a new JdbcMemory instance

---

### `create`

```java
public static @NonNull JdbcMemory create(@NonNull DataSource dataSource, @NonNull String tableName)
```

Creates a JdbcMemory with a custom table name.

**Parameters**

| Name | Description |
|------|-------------|
| `dataSource` | the JDBC data source |
| `tableName` | the table name to use |

**Returns**

a new JdbcMemory instance

