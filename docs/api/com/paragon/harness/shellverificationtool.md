# :material-code-braces: ShellVerificationTool

> This docs was updated at: 2026-03-08




`com.paragon.harness.tools.ShellVerificationTool` &nbsp;·&nbsp; **Class** (in `com.paragon.harness.tools`)

Extends `FunctionTool<ShellVerificationTool.TriggerRequest>`

---

A `FunctionTool` that runs a fixed shell command and returns the result as a `VerificationResult`.

**Security model:** The command is fixed at construction time by the developer. The agent can only trigger execution — it cannot modify the command. This prevents prompt injection attacks.

**See Also**

- `VerificationResult`
- `Harness`

*Since: 1.0*

## Builder

### `builder`

```java
public static @NonNull ShellVerificationTool.Builder builder()
```

Returns a new builder.

## Builder Methods

| Method | Description |
|--------|-------------|
| `name(String)` | Tool name as seen by the LLM (required) |
| `description(String)` | Tool description |
| `command(String...)` | The command to run, as vararg tokens — no shell expansion |
| `command(List<String>)` | The command as a list |
| `workingDir(Path)` | Working directory for the command |
| `timeoutSeconds(int)` | Timeout in seconds (default: 60) |
| `build()` | Builds the tool |

## Methods

### `runCommand`

```java
public @NonNull VerificationResult runCommand()
```

Runs the configured command and returns the result. Can also be called programmatically outside of an agent.

## Usage

```java
// Let the agent verify its own code by running the test suite
ShellVerificationTool testTool = ShellVerificationTool.builder()
    .name("run_tests")
    .description("Run the project test suite. Use this after writing or modifying code.")
    .command("mvn", "test", "-q")
    .workingDir(Path.of("/my/project"))
    .timeoutSeconds(120)
    .build();

ShellVerificationTool lintTool = ShellVerificationTool.builder()
    .name("run_linter")
    .description("Run checkstyle on the source code.")
    .command("mvn", "checkstyle:check", "-q")
    .workingDir(Path.of("/my/project"))
    .build();

Agent agent = Agent.builder()
    .name("CodeWriter")
    .instructions("""
        You write Java code. After writing or modifying code:
        1. Run run_linter to check style
        2. Run run_tests to verify correctness
        3. If either fails, fix the issues and run again
        """)
    .addTool(testTool)
    .addTool(lintTool)
    .build();
```

## Security

The tool name and parameters visible to the LLM are:
```json
{ "name": "run_tests", "parameters": {} }
```

The agent triggers it with an empty JSON `{}` — it has no way to inject arguments or change the command.
