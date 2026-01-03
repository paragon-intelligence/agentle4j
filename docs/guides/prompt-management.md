# Prompt Management

Agentle provides a flexible system for managing and retrieving prompts from various sources, including local files and external services like Langfuse.

## Overview

The `PromptProvider` interface abstracts prompt retrieval, allowing you to:

- Load prompts from the filesystem for development
- Fetch versioned prompts from Langfuse for production
- Switch between providers without changing application code

## The Prompt Class

The `Prompt` class is an immutable template container that supports Handlebars-like syntax for dynamic content.

### Creating Prompts

```java
// Simple text
Prompt prompt = Prompt.of("Hello, World!");

// Template with variables
Prompt template = Prompt.of("Hello, {{name}}!");

// Empty prompt
Prompt empty = Prompt.empty();
```

### Variable Interpolation

```java
Prompt prompt = Prompt.of("Hello, {{name}}! You have {{count}} messages.");
Prompt compiled = prompt.compile(Map.of(
    "name", "Alice",
    "count", 5
));
// Result: "Hello, Alice! You have 5 messages."
```

### Nested Properties

```java
Prompt prompt = Prompt.of("Welcome, {{user.profile.name}}!");
Prompt compiled = prompt.compile(Map.of(
    "user", Map.of("profile", Map.of("name", "Bob"))
));
// Result: "Welcome, Bob!"
```

### Conditional Blocks

```java
Prompt prompt = Prompt.of("""
    {{#if premium}}Welcome, Premium Member!{{/if}}
    {{#if items}}You have items in your cart.{{/if}}
    """);

// Truthy values: non-empty strings, non-zero numbers, non-empty collections
prompt.compile(Map.of("premium", true, "items", List.of("apple")));
```

### Iteration Blocks

```java
Prompt prompt = Prompt.of("""
    Items:
    {{#each items}}- {{this.name}}: ${{this.price}}
    {{/each}}
    """);

List<Map<String, Object>> items = List.of(
    Map.of("name", "Apple", "price", 1.50),
    Map.of("name", "Banana", "price", 0.75)
);
Prompt compiled = prompt.compile(Map.of("items", items));
// Result:
// Items:
// - Apple: $1.50
// - Banana: $0.75
```

### Fluent Builder

```java
Prompt compiled = Prompt.of("{{greeting}}, {{name}}!")
    .compile()
    .with("greeting", "Hello")
    .with("name", "World")
    .build();
```

### Varargs Compile

For a cleaner syntax with few variables, use the varargs overload:

```java
// Clean varargs syntax
Prompt compiled = prompt.compile("name", "Alice", "age", 30, "active", true);

// Equivalent to:
prompt.compile(Map.of("name", "Alice", "age", 30, "active", true));
```

### Extracting Variables

```java
Prompt prompt = Prompt.of("{{greeting}}, {{user.name}}!");
Set<String> vars = prompt.extractVariableNames();
// Returns: ["greeting", "user"]
```

### String Operations

```java
Prompt p1 = Prompt.of("Hello, ");
Prompt p2 = Prompt.of("World!");
Prompt combined = p1.append(p2);  // "Hello, World!"

prompt.length();      // Character count
prompt.isEmpty();     // Check if empty
prompt.isBlank();     // Check if blank
prompt.content();     // Get raw content
prompt.isCompiled();  // Check if compiled
```

---

## Template Syntax Reference

Agentle uses a **Handlebars-like** templating syntax that supports variable interpolation, conditional blocks, and iteration. All template expressions are wrapped in double curly braces `{{...}}`.

### Variable Interpolation

Insert values into your template using `{{variable_name}}`:

```java
Prompt prompt = Prompt.of("Hello, {{name}}!");
Prompt compiled = prompt.compile(Map.of("name", "Alice"));
// Result: "Hello, Alice!"
```

### Nested Property Access

Access nested object properties with dot notation `{{object.property.subproperty}}`:

```java
Prompt prompt = Prompt.of("Welcome, {{user.profile.displayName}}!");
Prompt compiled = prompt.compile(Map.of(
    "user", Map.of(
        "profile", Map.of("displayName", "Bob")
    )
));
// Result: "Welcome, Bob!"
```

Works with:
- **Maps** - `Map<String, Object>` with nested structure
- **Java Beans** - Objects with getters (`getProperty()`, `isProperty()`)
- **Records** - Java records with accessor methods

### Conditional Blocks (`#if`)

Conditionally include content based on truthiness:

```
{{#if condition}}
  Content shown if condition is truthy
{{/if}}
```

**Example:**
```java
Prompt prompt = Prompt.of("""
    {{#if premium}}⭐ Premium Member{{/if}}
    {{#if notifications}}You have {{count}} new messages.{{/if}}
    """);

Prompt compiled = prompt.compile(Map.of(
    "premium", true,
    "notifications", true,
    "count", 5
));
// Result: "⭐ Premium Member\nYou have 5 new messages."
```

#### Truthiness Rules

| Value Type | Truthy | Falsy |
|------------|--------|-------|
| `Boolean` | `true` | `false` |
| `String` | Non-empty | Empty `""` |
| `Number` | Non-zero | `0`, `0.0` |
| `Collection` | Non-empty | Empty |
| `Map` | Non-empty | Empty |
| `null` | — | Always falsy |
| Other objects | Always truthy | — |

### Iteration Blocks (`#each`)

Loop over collections with `{{#each collection}}...{{/each}}`:

```
{{#each items}}
  Access current item with: {{this}}
  Or nested properties: {{this.property}}
{{/each}}
```

**Example:**
```java
Prompt prompt = Prompt.of("""
    Shopping List:
    {{#each items}}- {{this.name}}: ${{this.price}}
    {{/each}}
    """);

List<Map<String, Object>> items = List.of(
    Map.of("name", "Apple", "price", 1.50),
    Map.of("name", "Banana", "price", 0.75),
    Map.of("name", "Cherry", "price", 3.00)
);

Prompt compiled = prompt.compile(Map.of("items", items));
// Result:
// Shopping List:
// - Apple: $1.50
// - Banana: $0.75
// - Cherry: $3.00
```

#### The `this` Keyword

Inside an `#each` block, `{{this}}` refers to the current item:

```java
// Simple list
Prompt prompt = Prompt.of("{{#each names}}{{this}}, {{/each}}");
prompt.compile(Map.of("names", List.of("Alice", "Bob", "Carol")));
// Result: "Alice, Bob, Carol, "

// Objects with properties
Prompt prompt = Prompt.of("{{#each users}}{{this.name}} ({{this.email}})\n{{/each}}");
```

#### Supported Collection Types

The `#each` block works with:
- `List<T>` and `Collection<T>`
- `Object[]` arrays
- `Map<K,V>` (iterates over entries)

### Nested Blocks

Blocks can be nested for complex templates:

```java
Prompt prompt = Prompt.of("""
    {{#each categories}}
    ## {{this.name}}
    {{#if this.items}}
    {{#each this.items}}- {{this.title}}
    {{/each}}
    {{/if}}
    {{/each}}
    """);
```

### Template Limits

To prevent resource exhaustion:
- **Maximum nesting depth**: 100 levels
- **Maximum iterations**: 10,000 items per `#each` block

Exceeding these limits throws a `TemplateException`.

### Quick Reference

| Syntax | Description | Example |
|--------|-------------|---------|
| `{{var}}` | Variable | `{{name}}` → `"Alice"` |
| `{{a.b.c}}` | Nested access | `{{user.profile.name}}` |
| `{{#if x}}...{{/if}}` | Conditional | Show if truthy |
| `{{#each list}}...{{/each}}` | Iteration | Loop over collection |
| `{{this}}` | Current item | Inside `#each` blocks |
| `{{this.prop}}` | Item property | `{{this.name}}` |


## PromptProvider Interface

```java
public interface PromptProvider {
    Prompt providePrompt(String promptId, Map<String, String> filters);
    
    // Convenience method without filters
    default Prompt providePrompt(String promptId);
}
```

## Filesystem Provider

Reads prompts from local files. Ideal for development and testing.

```java
// Create provider with base directory
PromptProvider provider = FilesystemPromptProvider.create(Path.of("./prompts"));

// Load prompt from ./prompts/greeting.txt
Prompt greeting = provider.providePrompt("greeting.txt");

// Load from subdirectory
Prompt email = provider.providePrompt("templates/email.txt");

// Use with template variables
Prompt compiled = greeting.compile(Map.of("name", "World"));
System.out.println(compiled.content()); // "Hello, World!"
```

### Directory Structure

```
prompts/
├── greeting.txt          # "Hello, {{name}}!"
├── templates/
│   ├── email.txt
│   └── notification.txt
└── system/
    └── assistant.txt
```

## Langfuse Provider

Retrieves prompts from [Langfuse](https://langfuse.com) with version control and labeling support.

### Basic Usage

```java
PromptProvider provider = LangfusePromptProvider.builder()
    .httpClient(new OkHttpClient())
    .publicKey("pk-lf-xxx")
    .secretKey("sk-lf-xxx")
    .build();

// Retrieve default (production) version
Prompt prompt = provider.providePrompt("my-prompt");
```

### Version and Label Filters

```java
// Get specific version
Prompt v2 = provider.providePrompt("my-prompt", Map.of("version", "2"));

// Get by label (e.g., staging, production)
Prompt staging = provider.providePrompt("my-prompt", Map.of("label", "staging"));
```

### Configuration

```java
LangfusePromptProvider provider = LangfusePromptProvider.builder()
    .httpClient(new OkHttpClient())
    .publicKey("pk-lf-xxx")
    .secretKey("sk-lf-xxx")
    
    // Optional: Custom base URL (default: https://cloud.langfuse.com)
    .baseUrl("https://self-hosted.langfuse.com")
    
    // Optional: Custom retry policy
    .retryPolicy(RetryPolicy.builder()
        .maxRetries(5)
        .initialDelay(Duration.ofMillis(500))
        .build())
    
    .build();
```

### Environment Variables

```java
// Load from LANGFUSE_PUBLIC_KEY, LANGFUSE_SECRET_KEY, LANGFUSE_HOST
LangfusePromptProvider provider = LangfusePromptProvider.builder()
    .httpClient(new OkHttpClient())
    .fromEnv()
    .build();
```

## Chat vs Text Prompts

Langfuse supports both text and chat prompt types. The provider handles both:

```java
// Text prompt: Returns content directly
// Langfuse: { "type": "text", "prompt": "Hello {{name}}" }
Prompt text = provider.providePrompt("text-prompt");
// text.content() -> "Hello {{name}}"

// Chat prompt: Concatenates messages with roles
// Langfuse: { "type": "chat", "prompt": [{"role": "system", "content": "Be helpful"}] }
Prompt chat = provider.providePrompt("chat-prompt");
// chat.content() -> "system: Be helpful"
```

## Error Handling

Both providers throw `PromptProviderException` on failure:

```java
try {
    Prompt prompt = provider.providePrompt("missing-prompt");
} catch (PromptProviderException e) {
    System.err.println("Prompt ID: " + e.promptId());
    System.err.println("Error: " + e.getMessage());
    
    if (e.isRetryable()) {
        // Transient error (network, rate limit) - retry logic
    }
}
```

### Exception Properties

| Property | Description |
|----------|-------------|
| `promptId()` | The ID of the prompt that failed |
| `isRetryable()` | Whether the error is transient |
| `getCause()` | Underlying exception |

## Retry Behavior

The Langfuse provider automatically retries on:

- **429** - Rate limit exceeded
- **500, 502, 503, 504** - Server errors
- **529** - Provider overloaded (OpenRouter)
- Network errors (IOException)

Non-retryable errors (404, 401, 403) are thrown immediately.

## Integration with Agents

Use prompts as agent instructions:

```java
PromptProvider prompts = LangfusePromptProvider.builder()
    .httpClient(httpClient)
    .fromEnv()
    .build();

// Load and compile system prompt
Prompt instructions = prompts.providePrompt("assistant-v2")
    .compile(Map.of("company", "Acme Corp"));

Agent agent = Agent.builder()
    .name("Assistant")
    .instructions(instructions.content())
    .responder(responder)
    .build();
```

## Best Practices

1. **Use FilesystemPromptProvider for development** - Faster iteration, no network calls
2. **Use LangfusePromptProvider for production** - Version control, A/B testing, analytics
3. **Use labels** - Label prompts as "staging" or "production" for safe deployments
4. **Compile templates** - Use Handlebars-style variables for dynamic prompts
5. **Handle errors gracefully** - Always catch `PromptProviderException`
