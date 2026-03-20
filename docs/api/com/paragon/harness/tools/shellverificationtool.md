# :material-code-braces: ShellVerificationTool

`com.paragon.harness.tools.ShellVerificationTool` &nbsp;·&nbsp; **Class**

Extends `FunctionTool<ShellVerificationTool.TriggerRequest>`

---

A `FunctionTool` that runs a fixed shell command and returns the result.

**Security model:** The command is fixed at construction time by the developer.
The agent can only trigger execution — it cannot modify the command. This prevents
prompt injection attacks.

Example: let the agent run the project's test suite:

```java
ShellVerificationTool testTool = ShellVerificationTool.builder()
    .name("run_tests")
    .description("Run the project test suite and return results")
    .command("mvn", "test", "-q")
    .workingDir(Path.of("/my/project"))
    .timeoutSeconds(120)
    .build();
Agent agent = Agent.builder()
    .addTool(testTool)
    .build();
```

**See Also**

- `VerificationResult`

*Since: 1.0*

## Methods

### `runCommand`

```java
public @NonNull VerificationResult runCommand()
```

Runs the configured command and returns the result. Can also be called programmatically.

**Returns**

the verification result

---

### `builder`

```java
public static @NonNull Builder builder()
```

Returns a new builder.

---

### `name`

```java
public @NonNull Builder name(@NonNull String name)
```

Sets the tool name (as seen by the LLM).

---

### `description`

```java
public @NonNull Builder description(@NonNull String description)
```

Sets the tool description.

---

### `command`

```java
public @NonNull Builder command(@NonNull String... command)
```

Sets the command as a vararg of tokens (no shell expansion).

---

### `command`

```java
public @NonNull Builder command(@NonNull List<String> command)
```

Sets the command as a list.

---

### `workingDir`

```java
public @NonNull Builder workingDir(@NonNull Path workingDir)
```

Sets the working directory for the command.

---

### `timeoutSeconds`

```java
public @NonNull Builder timeoutSeconds(int timeoutSeconds)
```

Sets the command timeout in seconds (default: 60).

---

### `build`

```java
public @NonNull ShellVerificationTool build()
```

Builds the tool.
