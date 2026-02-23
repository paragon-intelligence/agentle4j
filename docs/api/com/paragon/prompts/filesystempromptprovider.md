# :material-code-braces: FilesystemPromptProvider

> This docs was updated at: 2026-02-23

`com.paragon.prompts.FilesystemPromptProvider` &nbsp;·&nbsp; **Class**

Implements `PromptProvider`

---

A `PromptProvider` that reads prompts from the local filesystem.

Prompts are stored as text files, where the prompt ID is treated as a relative path from the
configured base directory.

### Usage Example

```java
// Create provider with base directory
PromptProvider provider = FilesystemPromptProvider.create(Path.of("./prompts"));
// Load prompt from ./prompts/greeting.txt
Prompt greeting = provider.providePrompt("greeting.txt");
// Load prompt from subdirectory ./prompts/templates/email.txt
Prompt email = provider.providePrompt("templates/email.txt");
// Compile the prompt with variables
Prompt compiled = greeting.compile(Map.of("name", "World"));
```

**Note:** The filters parameter is ignored by this implementation as the
filesystem does not support versioning or labeling.

*Since: 1.0*

## Methods

### `create`

```java
public static @NonNull FilesystemPromptProvider create(@NonNull Path baseDirectory)
```

Creates a new filesystem prompt provider with the given base directory.

**Parameters**

| Name | Description |
|------|-------------|
| `baseDirectory` | the base directory where prompts are stored |

**Returns**

a new `FilesystemPromptProvider`

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if baseDirectory is null |

---

### `create`

```java
public static @NonNull FilesystemPromptProvider create(@NonNull String baseDirectory)
```

Creates a new filesystem prompt provider from a directory path string.

**Parameters**

| Name | Description |
|------|-------------|
| `baseDirectory` | the base directory path as a string |

**Returns**

a new `FilesystemPromptProvider`

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if baseDirectory is null |

---

### `baseDirectory`

```java
public @NonNull Path baseDirectory()
```

Returns the base directory for this provider.

**Returns**

the base directory path

