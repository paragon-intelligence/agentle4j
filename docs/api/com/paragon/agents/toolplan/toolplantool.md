# :material-code-braces: ToolPlanTool

`com.paragon.agents.toolplan.ToolPlanTool` &nbsp;·&nbsp; **Class**

Extends `FunctionTool<ToolPlan>`

---

A meta-tool that allows the LLM to batch multiple tool calls into a single declarative execution
plan.

When the LLM calls this tool, the framework locally executes the plan — topologically sorting
steps, running independent steps in parallel, resolving `$ref` references — and returns
only the designated output steps' results. Intermediate results never touch the LLM context,
saving tokens and reducing latency.

### How It Works

  
- The LLM produces a `ToolPlan` as the argument to this tool
- The framework validates the plan, builds a dependency graph, and sorts steps into waves
- Each wave of independent steps executes in parallel using virtual threads
- `$ref:step_id` references are resolved between waves with actual outputs
- Only the designated `output_steps` results are returned to the LLM

### Usage

This tool is registered automatically when `Agent.Builder.enableToolPlanning()` is
called. It should not be created manually.

**See Also**

- `ToolPlan`
- `ToolPlanExecutor`

## Methods

### `ToolPlanTool`

```java
public ToolPlanTool(@NonNull FunctionToolStore toolStore)
```

Creates a ToolPlanTool that executes plans against the given tool store.

The tool store reference is used lazily at execution time (in `.call`), not at
construction time. This allows the ToolPlanTool itself to be registered in the same store after
construction.

**Parameters**

| Name | Description |
|------|-------------|
| `toolStore` | the store containing all available tools |

