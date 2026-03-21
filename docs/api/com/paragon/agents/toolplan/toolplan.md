# :material-database: ToolPlan

> This docs was updated at: 2026-03-21

`com.paragon.agents.toolplan.ToolPlan` &nbsp;·&nbsp; **Record**

---

A declarative execution plan for multiple tool calls with data flow between them.

The LLM produces this as the argument to the `ToolPlanTool` meta-tool. The framework
then executes the plan locally — topologically sorting steps, running independent steps in
parallel, resolving `$ref` references, and returning only the designated output steps'
results back to the LLM context.

### Example Plan

```java
{
  "steps": [
    { "id": "s1", "tool": "get_weather", "arguments": "{\"location\": \"Tokyo\"}" },
    { "id": "s2", "tool": "get_weather", "arguments": "{\"location\": \"London\"}" },
    { "id": "s3", "tool": "compare_data", "arguments": "{\"a\": \"$ref:s1\", \"b\": \"$ref:s2\"}" }
  ],
  "output_steps": ["s3"]
}
```
