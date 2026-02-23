# :material-code-braces: UrlSkillProvider

> This docs was updated at: 2026-02-23

`com.paragon.skills.UrlSkillProvider` &nbsp;·&nbsp; **Class**

Implements `SkillProvider`

---

Loads skills from remote URLs.

This provider fetches SKILL.md files via HTTP/HTTPS. It supports both direct URL loading and
base URL + skill ID patterns.

### Security Warning

**Only load skills from trusted sources.** Remote skills can contain instructions that may
execute tools or access data in unexpected ways.

### Usage Examples

```java
// Load from a direct URL
UrlSkillProvider provider = UrlSkillProvider.builder().build();
Skill skill = provider.loadFromUrl(
    URI.create("https://example.com/skills/pdf-processor/SKILL.md")
);
// With base URL pattern
UrlSkillProvider provider = UrlSkillProvider.builder()
    .baseUrl("https://example.com/skills")
    .build();
Skill skill = provider.provide("pdf-processor");
// Fetches: https://example.com/skills/pdf-processor/SKILL.md
```

**See Also**

- `SkillProvider`

*Since: 1.0*

## Methods

### `builder`

```java
public static @NonNull Builder builder()
```

Creates a new builder for UrlSkillProvider.

**Returns**

a new builder

---

### `loadFromUrl`

```java
public @NonNull Skill loadFromUrl(@NonNull URI url)
```

Loads a skill from a direct URL.

**Parameters**

| Name | Description |
|------|-------------|
| `url` | the URL to the SKILL.md file |

**Returns**

the parsed skill

**Throws**

| Type | Condition |
|------|-----------|
| `SkillProviderException` | if loading fails |

---

### `loadFromUrl`

```java
public @NonNull Skill loadFromUrl(@NonNull String url)
```

Loads a skill from a URL string.

**Parameters**

| Name | Description |
|------|-------------|
| `url` | the URL string to the SKILL.md file |

**Returns**

the parsed skill

**Throws**

| Type | Condition |
|------|-----------|
| `SkillProviderException` | if loading fails |

---

### `clearCache`

```java
public void clearCache()
```

Clears the skill cache.

---

### `httpClient`

```java
public @NonNull Builder httpClient(@NonNull HttpClient httpClient)
```

Sets a custom HttpClient.

**Parameters**

| Name | Description |
|------|-------------|
| `httpClient` | the HTTP client to use |

**Returns**

this builder

---

### `baseUrl`

```java
public @NonNull Builder baseUrl(@NonNull String baseUrl)
```

Sets the base URL for skill lookups.

When set, skill IDs are resolved as: {baseUrl}/{skillId}/SKILL.md

**Parameters**

| Name | Description |
|------|-------------|
| `baseUrl` | the base URL |

**Returns**

this builder

---

### `timeout`

```java
public @NonNull Builder timeout(@NonNull Duration timeout)
```

Sets the request timeout.

Default: 30 seconds

**Parameters**

| Name | Description |
|------|-------------|
| `timeout` | the timeout duration |

**Returns**

this builder

---

### `cacheEnabled`

```java
public @NonNull Builder cacheEnabled(boolean enabled)
```

Enables or disables caching.

Default: true (caching enabled)

**Parameters**

| Name | Description |
|------|-------------|
| `enabled` | whether to cache loaded skills |

**Returns**

this builder

---

### `build`

```java
public @NonNull UrlSkillProvider build()
```

Builds the UrlSkillProvider.

**Returns**

the configured provider

