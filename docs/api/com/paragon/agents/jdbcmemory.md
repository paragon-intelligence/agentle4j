# :material-code-braces: JdbcMemory

> This docs was updated at: 2026-03-10













`com.paragon.agents.JdbcMemory` &nbsp;·&nbsp; **Class**

Implements `Memory`

---

JDBC-backed durable implementation of `Memory`.

Stores memory entries in any JDBC-compatible database (PostgreSQL, MySQL, H2, SQLite, etc.). The table is created automatically on first use if it does not exist.

**Table schema (auto-created):**

```sql
CREATE TABLE IF NOT EXISTS agent_memory (
  id        VARCHAR(255) NOT NULL,
  user_id   VARCHAR(255) NOT NULL,
  timestamp TIMESTAMP    NOT NULL,
  content   TEXT         NOT NULL,
  metadata  TEXT         NOT NULL,
  PRIMARY KEY (id, user_id)
)
```

**See Also**

- `Memory`
- `MemoryEntry`
- `FilesystemMemory`
- `InMemoryMemory`

*Since: 1.0*

## Factory Methods

### `create(DataSource)`

```java
public static @NonNull JdbcMemory create(@NonNull DataSource dataSource)
```

Creates a JdbcMemory using the default table name `agent_memory`.

**Parameters**

- `dataSource` — the JDBC data source

**Returns**

a new JdbcMemory instance

---

### `create(DataSource, String)`

```java
public static @NonNull JdbcMemory create(@NonNull DataSource dataSource, @NonNull String tableName)
```

Creates a JdbcMemory with a custom table name.

**Parameters**

- `dataSource` — the JDBC data source
- `tableName` — the table name to use

**Returns**

a new JdbcMemory instance

## Usage

```java
// With HikariCP connection pool
HikariConfig config = new HikariConfig();
config.setJdbcUrl("jdbc:postgresql://localhost/mydb");
config.setUsername("user");
config.setPassword("password");
DataSource ds = new HikariDataSource(config);

Memory memory = JdbcMemory.create(ds);

Agent agent = Agent.builder()
    .name("Assistant")
    .model("openai/gpt-4o")
    .responder(responder)
    .addMemoryTools(memory)
    .build();
```

## Notes

- Relevance scoring for `retrieve()` is done in-memory after fetching all user entries; for large datasets consider a vector database
- The `ON CONFLICT` upsert clause uses PostgreSQL syntax; for other databases use `JdbcMemory.create(ds, "custom_table")` and ensure the schema is compatible
