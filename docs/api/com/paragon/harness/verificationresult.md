# :material-database: VerificationResult

> This docs was updated at: 2026-03-21

`com.paragon.harness.VerificationResult` &nbsp;·&nbsp; **Record**

---

The result of running a verification command (test suite, linter, etc.).

**See Also**

- `com.paragon.harness.tools.ShellVerificationTool`

*Since: 1.0*

## Methods

### `pass`

```java
public static @NonNull VerificationResult pass(@NonNull String output)
```

Creates a passing result.

---

### `fail`

```java
public static @NonNull VerificationResult fail(@NonNull String output, int exitCode)
```

Creates a failing result with the given exit code.

---

### `toSummary`

```java
public @NonNull String toSummary()
```

Returns a concise summary suitable for injection into agent context.
