# :material-code-braces: SkillStore

`com.paragon.skills.SkillStore` &nbsp;·&nbsp; **Class**

---

Registry for managing available skills.

SkillStore provides a centralized place to register and retrieve skills. It can generate a
formatted prompt section listing all available skills for inclusion in agent system prompts.

### Usage Example

```java
SkillStore store = new SkillStore();
store.register(pdfSkill);
store.register(dataAnalyzerSkill);
// Get a specific skill
Optional skill = store.get("pdf-processor");
// Generate prompt section
String skillsSection = store.generatePromptSection();
// Returns:
// ## Available Skills
// - pdf-processor: Process PDF files...
// - data-analyzer: Analyze data...
```

### Integration with Agents

```java
Agent agent = Agent.builder()
    .name("Assistant")
    .skillStore(store)  // Registers all skills as tools
    .build();
```

**See Also**

- `Skill`

*Since: 1.0*

## Methods

### `SkillStore`

```java
public SkillStore()
```

Creates an empty SkillStore.

---

### `SkillStore`

```java
public SkillStore(@NonNull Skill... skills)
```

Creates a SkillStore with initial skills.

**Parameters**

| Name | Description |
|------|-------------|
| `skills` | the skills to register |

---

### `SkillStore`

```java
public SkillStore(@NonNull List<Skill> skills)
```

Creates a SkillStore with initial skills.

**Parameters**

| Name | Description |
|------|-------------|
| `skills` | the skills to register |

---

### `register`

```java
public @NonNull SkillStore register(@NonNull Skill skill)
```

Registers a skill in the store.

If a skill with the same name already exists, it will be replaced.

**Parameters**

| Name | Description |
|------|-------------|
| `skill` | the skill to register |

**Returns**

this store for chaining

---

### `registerAll`

```java
public @NonNull SkillStore registerAll(@NonNull Skill... skills)
```

Registers multiple skills in the store.

**Parameters**

| Name | Description |
|------|-------------|
| `skills` | the skills to register |

**Returns**

this store for chaining

---

### `registerAll`

```java
public @NonNull SkillStore registerAll(@NonNull List<Skill> skills)
```

Registers multiple skills in the store.

**Parameters**

| Name | Description |
|------|-------------|
| `skills` | the skills to register |

**Returns**

this store for chaining

---

### `get`

```java
public @NonNull Optional<Skill> get(@NonNull String name)
```

Retrieves a skill by name.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the skill name |

**Returns**

Optional containing the skill, or empty if not found

---

### `all`

```java
public @NonNull List<Skill> all()
```

Returns all registered skills.

**Returns**

unmodifiable list of skills

---

### `names`

```java
public @NonNull Set<String> names()
```

Returns the names of all registered skills.

**Returns**

unmodifiable set of skill names

---

### `contains`

```java
public boolean contains(@NonNull String name)
```

Checks if a skill is registered.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the skill name |

**Returns**

true if the skill exists

---

### `size`

```java
public int size()
```

Returns the number of registered skills.

**Returns**

skill count

---

### `isEmpty`

```java
public boolean isEmpty()
```

Returns whether the store is empty.

**Returns**

true if no skills are registered

---

### `remove`

```java
public @NonNull Optional<Skill> remove(@NonNull String name)
```

Removes a skill from the store.

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the skill name |

**Returns**

Optional containing the removed skill, or empty if not found

---

### `clear`

```java
public void clear()
```

Clears all skills from the store.

---

### `generatePromptSection`

```java
public @NonNull String generatePromptSection()
```

Generates a formatted prompt section listing all available skills.

This can be appended to an agent's system prompt to inform the LLM about available skills.

**Returns**

formatted skills section

---

### `generateCompactPromptSection`

```java
public @NonNull String generateCompactPromptSection()
```

Generates a compact prompt section (one line per skill).

**Returns**

compact skills section

