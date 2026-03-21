# :material-code-braces: SkillReaderTool

> This docs was updated at: 2026-03-21

`com.paragon.skills.SkillReaderTool` &nbsp;·&nbsp; **Class**

Extends `FunctionTool<SkillReaderTool.Params>`

---

A tool that lets the agent read the full instructions and resources for a specific skill.

Instead of injecting every skill's full content into the system prompt (wasting context
tokens), this tool implements **progressive disclosure**: the agent sees a concise catalog of
available skills (name + description) in its prompt, and calls this tool only when it decides a
skill is relevant to the current task.

### How It Works

  
- The agent's system prompt includes a catalog listing each skill's name and description
- When the agent determines a skill is relevant, it calls `read_skill(skillName)`
- This tool looks up the skill in the `SkillStore` and returns its full content
- The agent can then apply the skill's expertise with full instructions available

This tool is registered automatically when skills are added to an agent via `Agent.Builder.addSkill()`. It should not be created manually.

**See Also**

- `Skill`
- `SkillStore`

*Since: 1.0*

## Methods

### `SkillReaderTool`

```java
public SkillReaderTool(@NonNull SkillStore skillStore)
```

Creates a SkillReaderTool backed by the given skill store.

**Parameters**

| Name | Description |
|------|-------------|
| `skillStore` | the store containing available skills |

