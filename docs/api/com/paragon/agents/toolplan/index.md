# Package `com.paragon.agents.toolplan`

> This docs was updated at: 2026-03-21

---

## :material-code-braces: Classs

| Name | Description |
|------|-------------|
| [`PlanReferenceResolver`](planreferenceresolver.md) | Resolves `$ref:step_id` and `$ref:step_id |
| [`ToolPlanException`](toolplanexception.md) | Exception thrown when a tool plan fails validation or execution |
| [`ToolPlanExecutor`](toolplanexecutor.md) | Executes a `ToolPlan` against a `FunctionToolStore` |
| [`ToolPlanTool`](toolplantool.md) | A meta-tool that allows the LLM to batch multiple tool calls into a single declarative execution … |

## :material-database: Records

| Name | Description |
|------|-------------|
| [`ToolPlan`](toolplan.md) | A declarative execution plan for multiple tool calls with data flow between them |
| [`ToolPlanResult`](toolplanresult.md) | The result of executing a `ToolPlan` |
| [`ToolPlanStep`](toolplanstep.md) | A single step in a tool execution plan |
