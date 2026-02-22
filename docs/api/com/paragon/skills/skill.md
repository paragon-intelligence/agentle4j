# :material-code-braces: Skill

`com.paragon.skills.Skill` &nbsp;·&nbsp; **Class**

---

Represents a modular expertise that augments an agent's capabilities.

A Skill packages instructions and resources that are injected into the agent's system prompt.
When a skill is added to an agent, its instructions become part of the agent's knowledge,
allowing the LLM to automatically apply the skill's expertise when relevant.

Unlike sub-agents, skills share the main agent's context window. They extend the agent's
capabilities without creating separate execution contexts.

### Usage Examples

#### Via Code

```java
Skill pdfSkill = Skill.builder()
    .name("pdf-processor")
    .description("Process PDF files, extract text, fill forms")
    .instructions("""
        You are a PDF processing expert. When working with PDFs:
        1. Analyze the document structure
        2. Extract or modify content as requested
        3. Return well-formatted results
        """)
    .build();
```

#### Simple Factory

```java
Skill skill = Skill.of(
    "greeting",
    "Generate personalized greetings",
    "You create warm, personalized greetings..."
);
```

### Integration with Agents

```java
Agent agent = Agent.builder()
    .name("DocumentAssistant")
    .instructions("You help users with document tasks.")
    .addSkill(pdfSkill)  // Skill instructions are added to agent's prompt
    .responder(responder)
    .build();
```

**See Also**

- `SkillProvider`
- `SkillStore`

*Since: 1.0*

## Methods

### `builder`

```java
public static @NonNull Builder builder()
```

Creates a new Skill builder.

**Returns**

a new builder instance

---

### `of`

```java
public static @NonNull Skill of(
      @NonNull String name, @NonNull String description, @NonNull String instructions)
```

Creates a simple Skill with name, description, and instructions.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the skill name (lowercase, numbers, hyphens) |
| `description` | when to use this skill |
| `instructions` | the skill's instructions |

**Returns**

a new Skill instance

---

### `of`

```java
public static @NonNull Skill of(
      @NonNull String name, @NonNull String description, @NonNull Prompt instructions)
```

Creates a simple Skill with name, description, and instructions.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the skill name (lowercase, numbers, hyphens) |
| `description` | when to use this skill |
| `instructions` | the skill's instructions as a Prompt |

**Returns**

a new Skill instance

---

### `name`

```java
public @NonNull String name()
```

Returns the skill's unique name.

**Returns**

the skill name

---

### `description`

```java
public @NonNull String description()
```

Returns the skill's description.

This description helps the LLM understand when to apply this skill's expertise.

**Returns**

the skill description

---

### `instructions`

```java
public @NonNull Prompt instructions()
```

Returns the skill's instructions.

These instructions are injected into the agent's system prompt.

**Returns**

the skill instructions

---

### `resources`

```java
public @NonNull Map<String, Prompt> resources()
```

Returns additional resources (context files) for this skill.

Resources are named Prompts that provide supplementary context. For example, a PDF skill
might have a "FORMS.md" resource with form-filling guidance.

**Returns**

unmodifiable map of resource name to content

---

### `hasResources`

```java
public boolean hasResources()
```

Returns whether this skill has any additional resources.

**Returns**

true if the skill has resources

---

### `toPromptSection`

```java
public @NonNull String toPromptSection()
```

Generates the prompt section for this skill.

This produces a formatted text block that can be appended to an agent's system prompt. The
format includes the skill name, description, instructions, and any resources.

**Returns**

the formatted skill prompt section

---

### `name`

```java
public @NonNull Builder name(@NonNull String name)
```

Sets the skill's unique name.

Must contain only lowercase letters, numbers, and hyphens. Maximum 64 characters.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the skill name |

**Returns**

this builder

---

### `description`

```java
public @NonNull Builder description(@NonNull String description)
```

Sets the skill's description.

This should explain what the skill does and when to use it. Maximum 1024 characters.

**Parameters**

| Name | Description |
|------|-------------|
| `description` | the skill description |

**Returns**

this builder

---

### `instructions`

```java
public @NonNull Builder instructions(@NonNull Prompt instructions)
```

Sets the skill's instructions.

**Parameters**

| Name | Description |
|------|-------------|
| `instructions` | the instructions as a Prompt |

**Returns**

this builder

---

### `instructions`

```java
public @NonNull Builder instructions(@NonNull String instructions)
```

Sets the skill's instructions.

**Parameters**

| Name | Description |
|------|-------------|
| `instructions` | the instructions as a string |

**Returns**

this builder

---

### `addResource`

```java
public @NonNull Builder addResource(@NonNull String name, @NonNull Prompt content)
```

Adds a resource (additional context file) to the skill.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the resource name (e.g., "FORMS.md") |
| `content` | the resource content |

**Returns**

this builder

---

### `addResource`

```java
public @NonNull Builder addResource(@NonNull String name, @NonNull String content)
```

Adds a resource (additional context file) to the skill.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the resource name (e.g., "FORMS.md") |
| `content` | the resource content as a string |

**Returns**

this builder

---

### `build`

```java
public @NonNull Skill build()
```

Builds the Skill instance.

**Returns**

the configured Skill

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if required fields are missing |
| `IllegalArgumentException` | if validation fails |

