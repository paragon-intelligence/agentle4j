# :material-code-braces: SkillMarkdownParser

`com.paragon.skills.SkillMarkdownParser` &nbsp;·&nbsp; **Class**

---

Parser for SKILL.md files using YAML frontmatter and Markdown content.

The parser handles files in Claude's SKILL.md format:

```java
---
name: pdf-processor
description: Process PDF files, extract text, fill forms.
---
# PDF Processing
## Instructions
You are a PDF processing expert...
```

### YAML Frontmatter

The frontmatter section (between --- markers) must contain:

  
- **name** (required): Lowercase letters, numbers, and hyphens only. Max 64 chars.
- **description** (required): When to use this skill. Max 1024 chars.

### Markdown Body

Everything after the frontmatter is treated as the skill's instructions.

**See Also**

- `Skill`
- `FilesystemSkillProvider`

*Since: 1.0*

## Methods

### `SkillMarkdownParser`

```java
public SkillMarkdownParser()
```

Creates a new SkillMarkdownParser.

---

### `parse`

```java
public @NonNull Skill parse(@NonNull String content)
```

Parses a SKILL.md file content into a Skill.

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the file content |

**Returns**

the parsed Skill

**Throws**

| Type | Condition |
|------|-----------|
| `SkillProviderException` | if parsing fails |

---

### `parse`

```java
public @NonNull Skill parse(@NonNull String content, @NonNull String skillId)
```

Parses a SKILL.md file content with a fallback skill ID.

If the name is not found in frontmatter, uses the provided skillId.

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the file content |
| `skillId` | fallback skill ID (used if name not in frontmatter) |

**Returns**

the parsed Skill

**Throws**

| Type | Condition |
|------|-----------|
| `SkillProviderException` | if parsing fails |

---

### `isValid`

```java
public boolean isValid(@NonNull String content)
```

Validates that SKILL.md content is parseable without fully parsing.

**Parameters**

| Name | Description |
|------|-------------|
| `content` | the content to validate |

**Returns**

true if the content appears to be valid SKILL.md format

