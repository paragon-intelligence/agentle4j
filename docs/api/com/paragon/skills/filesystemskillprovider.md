# :material-code-braces: FilesystemSkillProvider

> This docs was updated at: 2026-02-23

`com.paragon.skills.FilesystemSkillProvider` &nbsp;·&nbsp; **Class**

Implements `SkillProvider`

---

Loads skills from the filesystem in SKILL.md format.

This provider expects skills to be organized as directories containing a SKILL.md file:

```
skills/
├── pdf-processor/
│   ├── SKILL.md         (required)
│   └── FORMS.md         (optional resource)
└── data-analyzer/
    └── SKILL.md
```

### Usage Example

```java
SkillProvider provider = FilesystemSkillProvider.create(Path.of("./skills"));
// Load a specific skill
Skill skill = provider.provide("pdf-processor");
// List all available skills
Set skillIds = provider.listSkillIds();
// Check if a skill exists
boolean exists = provider.exists("pdf-processor");
```

**See Also**

- `SkillProvider`
- `SkillMarkdownParser`

*Since: 1.0*

## Methods

### `create`

```java
public static @NonNull FilesystemSkillProvider create(@NonNull Path skillsDirectory)
```

Creates a FilesystemSkillProvider for the given directory.

**Parameters**

| Name | Description |
|------|-------------|
| `skillsDirectory` | the root directory containing skill subdirectories |

**Returns**

a new provider

**Throws**

| Type | Condition |
|------|-----------|
| `SkillProviderException` | if the directory doesn't exist |

---

### `loadFromFile`

```java
public static @NonNull Skill loadFromFile(@NonNull Path skillFile)
```

Loads a skill from a single SKILL.md file (not a directory).

**Parameters**

| Name | Description |
|------|-------------|
| `skillFile` | path to the SKILL.md file |

**Returns**

the parsed skill

**Throws**

| Type | Condition |
|------|-----------|
| `SkillProviderException` | if loading fails |

---

### `parseToBuilder`

```java
private Skill.Builder parseToBuilder(String content, String skillId)
```

Parses SKILL.md content into a Skill.Builder for further modification.

---

### `loadResources`

```java
private void loadResources(Path skillDir, Skill.Builder builder) throws IOException
```

Loads additional .md files as resources.

---

### `skillsDirectory`

```java
public @NonNull Path skillsDirectory()
```

Returns the root skills directory.

**Returns**

the skills directory path

