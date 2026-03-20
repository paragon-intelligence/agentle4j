# :material-code-braces: InMemorySkillProvider

`com.paragon.skills.InMemorySkillProvider` &nbsp;·&nbsp; **Class**

Implements `SkillProvider`

---

Stores code-defined skills in memory.

This provider is useful for skills defined directly in code without external files. It can be
pre-populated with skills at construction time or skills can be registered dynamically.

### Usage Examples

```java
// Factory method with initial skills
SkillProvider provider = InMemorySkillProvider.of(skill1, skill2, skill3);
// Builder pattern
SkillProvider provider = InMemorySkillProvider.builder()
    .add(pdfSkill)
    .add(dataSkill)
    .build();
// Empty provider with dynamic registration
InMemorySkillProvider provider = InMemorySkillProvider.empty();
provider.register(newSkill);
```

**See Also**

- `SkillProvider`
- `Skill`

*Since: 1.0*

## Methods

### `empty`

```java
public static @NonNull InMemorySkillProvider empty()
```

Creates an empty InMemorySkillProvider.

**Returns**

a new empty provider

---

### `of`

```java
public static @NonNull InMemorySkillProvider of(@NonNull Skill... skills)
```

Creates an InMemorySkillProvider with initial skills.

**Parameters**

| Name | Description |
|------|-------------|
| `skills` | the skills to register |

**Returns**

a new provider with the skills

---

### `builder`

```java
public static @NonNull Builder builder()
```

Creates a new builder for InMemorySkillProvider.

**Returns**

a new builder

---

### `register`

```java
public @NonNull InMemorySkillProvider register(@NonNull Skill skill)
```

Registers a skill in this provider.

If a skill with the same name already exists, it will be replaced.

**Parameters**

| Name | Description |
|------|-------------|
| `skill` | the skill to register |

**Returns**

this provider for chaining

---

### `registerAll`

```java
public @NonNull InMemorySkillProvider registerAll(@NonNull Skill... skills)
```

Registers multiple skills.

**Parameters**

| Name | Description |
|------|-------------|
| `skills` | the skills to register |

**Returns**

this provider for chaining

---

### `remove`

```java
public boolean remove(@NonNull String skillId)
```

Removes a skill from this provider.

**Parameters**

| Name | Description |
|------|-------------|
| `skillId` | the skill name to remove |

**Returns**

true if the skill was removed

---

### `clear`

```java
public void clear()
```

Clears all skills from this provider.

---

### `size`

```java
public int size()
```

Returns the number of registered skills.

**Returns**

skill count

---

### `add`

```java
public @NonNull Builder add(@NonNull Skill skill)
```

Adds a skill to the provider.

**Parameters**

| Name | Description |
|------|-------------|
| `skill` | the skill to add |

**Returns**

this builder

---

### `addAll`

```java
public @NonNull Builder addAll(@NonNull Skill... skills)
```

Adds multiple skills to the provider.

**Parameters**

| Name | Description |
|------|-------------|
| `skills` | the skills to add |

**Returns**

this builder

---

### `build`

```java
public @NonNull InMemorySkillProvider build()
```

Builds the InMemorySkillProvider.

**Returns**

the configured provider

