# :material-database: ToolPlanStep

> This docs was updated at: 2026-03-21

`com.paragon.agents.toolplan.ToolPlanStep` &nbsp;·&nbsp; **Record**

---

A single step in a tool execution plan.

Each step specifies a tool to call and its arguments. Arguments may contain `$ref:step_id` references to inject outputs from previously executed steps.

### Reference Syntax

  
- `"$ref:step_id"` — replaced with the full output of the referenced step
- `"$ref:step_id.field"` — replaced with a specific JSON field from the referenced
      step's output
- `"$ref:step_id.field.nested"` — dot-separated paths for nested JSON field access
