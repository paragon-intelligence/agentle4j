# :material-code-braces: ToolPlanExecutor

`com.paragon.agents.toolplan.ToolPlanExecutor` &nbsp;·&nbsp; **Class**

---

Executes a `ToolPlan` against a `FunctionToolStore`.

The executor:

  
- Validates the plan (duplicate IDs, unknown tools, cycles, self-references)
- Builds a dependency graph from `$ref` references in step arguments
- Topologically sorts steps into execution "waves" using Kahn's algorithm
- Executes each wave in parallel using `StructuredTaskScope` (virtual threads)
- Resolves `$ref` references between waves
- Collects and returns results

Error strategy is **fail-forward**: if a step fails, dependent steps are skipped, but
independent steps continue executing.

## Methods

### `execute`

```java
public @NonNull ToolPlanResult execute(@NonNull ToolPlan plan)
```

Executes the given plan and returns a `ToolPlanResult`.

**Parameters**

| Name | Description |
|------|-------------|
| `plan` | the plan to execute |

**Returns**

the execution result

**Throws**

| Type | Condition |
|------|-----------|
| `ToolPlanException` | if the plan fails validation (duplicate IDs, cycles, unknown tools) |

---

### `topologicalSort`

```java
List<List<ToolPlanStep>> topologicalSort(
      List<ToolPlanStep> steps, Map<String, Set<String>> dependencies)
```

Topologically sorts steps into execution waves using Kahn's algorithm. Each wave contains steps
that can execute in parallel.

---

### `executeWave`

```java
private List<ToolPlanResult.StepResult> executeWave(
      List<ToolPlanStep> wave,
      Map<String, String> resolvedOutputs,
      Set<String> failedSteps,
      Map<String, String> errors)
```

Executes all steps in a wave in parallel using StructuredTaskScope.
