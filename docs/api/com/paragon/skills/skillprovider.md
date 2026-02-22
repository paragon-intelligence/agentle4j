# :material-approximately-equal: SkillProvider

`com.paragon.skills.SkillProvider` &nbsp;·&nbsp; **Interface**

---

Provider interface for loading skills from various sources.

Implementations may load skills from the filesystem, remote URLs, in-memory registries, or any
other storage mechanism. This abstraction allows applications to centralize skill management.

### Usage Examples

```java
// Filesystem provider
SkillProvider provider = FilesystemSkillProvider.create(Path.of("./skills"));
Skill skill = provider.provide("pdf-processor");
// URL provider
SkillProvider urlProvider = UrlSkillProvider.builder()
    .httpClient(httpClient)
    .build();
Skill skill = urlProvider.provide("https://example.com/skills/pdf-processor/SKILL.md");
// In-memory provider
SkillProvider memProvider = InMemorySkillProvider.of(skill1, skill2);
```

**See Also**

- `FilesystemSkillProvider`
- `UrlSkillProvider`
- `InMemorySkillProvider`

*Since: 1.0*

## Methods

### `provide`

```java
Skill provide(@NonNull String skillId, @Nullable Map<String, String> filters)
```

Retrieves a skill by its identifier.

**Parameters**

| Name | Description |
|------|-------------|
| `skillId` | the unique identifier for the skill (e.g., skill name, path) |
| `filters` | optional key-value pairs to filter the skill. Supported filters depend on the implementation (e.g., version, label). |

**Returns**

the retrieved `Skill`

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if skillId is null |
| `SkillProviderException` | if the skill cannot be retrieved |

---

### `provide`

```java
default Skill provide(@NonNull String skillId)
```

Retrieves a skill by its identifier without filters.

**Parameters**

| Name | Description |
|------|-------------|
| `skillId` | the unique identifier for the skill |

**Returns**

the retrieved `Skill`

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if skillId is null |
| `SkillProviderException` | if the skill cannot be retrieved |

---

### `exists`

```java
boolean exists(@NonNull String skillId)
```

Checks if a skill with the given identifier exists.

**Parameters**

| Name | Description |
|------|-------------|
| `skillId` | the unique identifier for the skill |

**Returns**

`true` if the skill exists, `false` otherwise

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if skillId is null |

---

### `listSkillIds`

```java
Set<String> listSkillIds()
```

Lists all available skill identifiers.

**Returns**

an unmodifiable set of all available skill identifiers

**Throws**

| Type | Condition |
|------|-----------|
| `SkillProviderException` | if the listing fails |

