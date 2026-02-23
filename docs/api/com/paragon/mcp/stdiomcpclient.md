# :material-code-braces: StdioMcpClient

> This docs was updated at: 2026-02-23

`com.paragon.mcp.StdioMcpClient` &nbsp;·&nbsp; **Class**

Extends `McpClient`

---

MCP client that communicates with a subprocess via stdio.

This client launches an MCP server as a subprocess and communicates using stdin/stdout. This
is the standard transport for local MCP servers.

### Example Usage

```java
try (var mcp = StdioMcpClient.builder()
        .command("npx", "-y", "@modelcontextprotocol/server-filesystem", "/tmp")
        .build()) {
    mcp.connect();
    Agent agent = Agent.builder()
        .name("FileAssistant")
        .tools(mcp.asTools())
        .build();
    var result = agent.interact("List files in the directory");
}
```

**See Also**

- `McpClient`

## Methods

### `builder`

```java
public static @NonNull Builder builder()
```

Creates a new builder for StdioMcpClient.

**Returns**

a new builder

---

### `command`

```java
public @NonNull Builder command(@NonNull String... command)
```

Sets the command to run.

**Parameters**

| Name | Description |
|------|-------------|
| `command` | the command and arguments |

**Returns**

this builder

---

### `command`

```java
public @NonNull Builder command(@NonNull List<String> command)
```

Sets the command to run.

**Parameters**

| Name | Description |
|------|-------------|
| `command` | the command and arguments |

**Returns**

this builder

---

### `environment`

```java
public @NonNull Builder environment(@NonNull String name, @NonNull String value)
```

Adds an environment variable.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the variable name |
| `value` | the variable value |

**Returns**

this builder

---

### `environment`

```java
public @NonNull Builder environment(@NonNull Map<String, String> environment)
```

Sets environment variables.

**Parameters**

| Name | Description |
|------|-------------|
| `environment` | the environment variables |

**Returns**

this builder

---

### `workingDirectory`

```java
public @NonNull Builder workingDirectory(@NonNull Path directory)
```

Sets the working directory for the subprocess.

**Parameters**

| Name | Description |
|------|-------------|
| `directory` | the working directory |

**Returns**

this builder

---

### `objectMapper`

```java
public @NonNull Builder objectMapper(@NonNull ObjectMapper objectMapper)
```

Sets the ObjectMapper for JSON serialization.

**Parameters**

| Name | Description |
|------|-------------|
| `objectMapper` | the object mapper |

**Returns**

this builder

---

### `build`

```java
public @NonNull StdioMcpClient build()
```

Builds the StdioMcpClient.

**Returns**

a new StdioMcpClient

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | if command is empty |

