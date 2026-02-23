# Prompt Management

> This docs was updated at: 2026-02-23

Agentle provides a flexible system for managing and retrieving prompts from various sources, including local files and external services like Langfuse. It also includes a comprehensive Prompt Builder API for constructing high-quality prompts following best practices.

## Prompt Builder Pattern

The Prompt Builder provides a fluent, order-independent API for creating structured prompts with built-in best practices.

### Key Features

- **Order Independent**: All builder methods can be called in any order
- **60+ Builder Methods**: Comprehensive API for all prompt construction needs
- **Multi-language**: English (US) and Portuguese (BR) support
- **Pre-built Templates**: Domain-specific starting points
- **Reasoning Strategies**: Chain-of-thought, self-verification, etc.
- **Output Controls**: JSON, Markdown, CSV, step-by-step formats

### Basic Usage

```java
// Simple task-based prompt
Prompt prompt = Prompt.builder()
    .role("data analyst")
    .task("Analyze the sales data")
    .instructions("Focus on trends", "Identify anomalies")
    .outputAs(OutputFormat.JSON)
    .build();

// With reasoning strategy
Prompt withReasoning = Prompt.builder()
    .task("Solve the math problem")
    .withChainOfThought()
    .withSelfVerification()
    .build();
```

### Static Factory Methods

```java
// Task-based
Prompt taskPrompt = Prompt.forTask("Summarize the document")
    .input(documentText)
    .concise()
    .build();

// Extraction
Prompt extraction = Prompt.forExtraction("name, email, phone")
    .input(unstructuredText)
    .build();

// Classification
Prompt classifier = Prompt.forClassification("urgent", "normal", "low")
    .input("Customer complaint")
    .build();
```

### Pre-built Templates

```java
// Code review
Prompt codeReview = Prompt.forCodeReview()
    .input(sourceCode)
    .build();

// SWOT analysis
Prompt swot = Prompt.forSWOTAnalysis()
    .input("Product launch strategy")
    .build();

// Data analysis
Prompt analysis = Prompt.forDataAnalysis()
    .input(csvData)
    .build();

// Email drafting
Prompt email = Prompt.forEmailDrafting(EmailTone.PROFESSIONAL)
    .input("Request for product demo")
    .build();
```

### Multi-language Support

```java
// English (default)
Prompt english = Prompt.builder()
    .role("financial analyst")
    .task("Analyze the quarterly report")
    .build();

// Portuguese (Brazil)
Prompt portuguese = Prompt.builder(Language.PT_BR)
    .role("analista financeiro")
    .task("Analise o relatório trimestral")
    .withChainOfThought()
    .build();
```

### Reasoning Strategies

```java
Prompt reasoning = Prompt.builder()
    .task("Complex problem solving")
    .withChainOfThought()        // Step-by-step reasoning
    .withStepBack()              // Consider underlying principles
    .withSelfVerification()      // Verify the answer
    .withDecomposition()         // Break into sub-problems
    .withTreeOfThoughts()        // Explore multiple approaches
    .withMultiplePerspectives()  // Different viewpoints
    .build();
```

### Output Formats

```java
// JSON output with schema
Prompt jsonPrompt = Prompt.builder()
    .task("Extract user data")
    .outputAs(OutputFormat.JSON)
    .outputSchema("{\"name\": \"string\", \"age\": \"number\"}")
    .build();

// Markdown table
Prompt tablePrompt = Prompt.builder()
    .task("Compare products")
    .outputAs(OutputFormat.TABLE)
    .build();

// Step-by-step instructions
Prompt tutorial = Prompt.builder()
    .task("Explain deployment process")
    .stepByStep()
    .build();
```

### Few-Shot Learning

```java
Prompt fewShot = Prompt.builder()
    .task("Classify sentiment")
    .fewShot()
        .example("Great product!", "POSITIVE")
        .example("Terrible experience", "NEGATIVE")
        .example("It's okay", "NEUTRAL")
        .shuffled()  // Randomize example order
    .done()
    .input("This is amazing!")
    .build();
```

### Context & Documents

```java
Prompt withContext = Prompt.builder()
    .role("customer support specialist")
    .context()
        .document("product_manual.md", manualContent)
        .document("faq.md", faqContent)
        .background("Customer: Premium tier, Account: 3 years")
    .done()
    .task("Help customer with their issue")
    .input(customerMessage)
    .build();
```

### Specialized Modes

```java
// Code debugging
Prompt debug = Prompt.forCodeDebugging("NullPointerException at line 42")
    .input(sourceCode)
    .build();

// Code refactoring
Prompt refactor = Prompt.forCodeRefactoring("improve readability")
    .input(legacyCode)
    .build();

// Decision making
Prompt decision = Prompt.forDecisionMaking()
    .input("Should we migrate to microservices?")
    .build();

// Root cause analysis
Prompt rootCause = Prompt.forRootCauseAnalysis()
    .input("Production outage on 2026-01-20")
    .build();
```

### Audience Targeting

```java
// For beginners
Prompt beginner = Prompt.forELI5()
    .input("Explain quantum computing")
    .build();

// For experts
Prompt expert = Prompt.forExpertExplanation()
    .input("Explain quantum computing")
    .difficultyLevel(DifficultyLevel.EXPERT)
    .build();

// Custom audience
Prompt custom = Prompt.builder()
    .forAudience("software engineering managers")
    .audienceLevel("intermediate technical knowledge")
    .task("Explain CI/CD benefits")
    .build();
```

### Response Control

```java
// Concise responses
Prompt brief = Prompt.builder()
    .task("Summarize the article")
    .concise()
    .maxTokens(100)
    .build();

// Detailed responses
Prompt detailed = Prompt.builder()
    .task("Explain the algorithm")
    .detailed()
    .comprehensive()
    .build();

// Word count limit
Prompt limited = Prompt.builder()
    .task("Write a summary")
    .wordCount(200, 300)
    .build();

// With TL;DR
Prompt withTldr = Prompt.builder()
    .task("Analyze the report")
    .withTLDR()
    .build();
```

### Order Independence Example

All builder methods can be called in any order:

```java
// These are equivalent
Prompt p1 = Prompt.builder()
    .task("Analyze data")
    .role("analyst")
    .withChainOfThought()
    .outputAs(OutputFormat.JSON)
    .build();

Prompt p2 = Prompt.builder()
    .outputAs(OutputFormat.JSON)
    .withChainOfThought()
    .role("analyst")
    .task("Analyze data")
    .build();
```

### Conditional Configuration

```java
Prompt conditional = Prompt.builder()
    .task("Process user request")
    .when(userIsPremium, b -> b.addPersonality("VIP-focused"))
    .when(requiresAnalysis, b -> b.withChainOfThought())
    .build();
```

### Available Templates

**Analysis & Decision Making**:
- `forSWOTAnalysis()`, `forProsConsAnalysis()`, `forDecisionMaking()`
- `forRootCauseAnalysis()`, `forComparison()`, `forDataAnalysis()`

**Code & Development**:
- `forCode(lang)`, `forCodeReview()`, `forCodeDebugging(error)`
- `forCodeRefactoring(goal)`, `forCodeTranslation(from, to)`
- `forAPIDocumentation()`, `forChangelog()`, `forUserStories()`

**Writing & Content**:
- `forEmailDrafting(tone)`, `forTechnicalWriting()`, `forAcademicWriting(style)`
- `forMarketingCopy()`, `forStorytelling()`, `forSummarization()`
- `forTranslation(from, to)`, `forBrainstorming()`

**Education & Learning**:
- `forELI5()`, `forExpertExplanation()`, `forTutorial()`
- `forLanguageLearning(target, native)`, `forQuizGeneration(n, type)`
- `asSocraticTutor()`

**Specialized**:
- `forExtraction(fields)`, `forClassification(categories)`
- `forRAG()`, `forQA()`, `forFactChecking()`
- `forAccessibilityReview()`, `forSecurityReview()`, `forPerformanceOptimization()`

---

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
    // Retrieve prompts
    Prompt providePrompt(String promptId, Map<String, String> filters);
    default Prompt providePrompt(String promptId);
    
    // Discovery
    boolean exists(String promptId);
    Set<String> listPromptIds();
}
```

## PromptStore Interface

For implementations that support write operations (e.g., database-backed storage):

```java
public interface PromptStore {
    void save(String promptId, Prompt prompt);
    default void save(String promptId, String content);  // Convenience
    void delete(String promptId);
}
```

**Interface Segregation:**
- `PromptProvider` — Read-only operations for agents and most consumers
- `PromptStore` — Write operations for admin dashboards and management
- Implementations needing full CRUD can implement both interfaces


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
    System.err.println("Prompt ID: " + e.promptId().orElse("unknown"));
    System.err.println("Error: " + e.getMessage());
    
    if (e.isRetryable()) {
        // Transient error (network, rate limit) - retry logic
    }
}
```

### Exception Properties

| Property | Description |
|----------|-------------|
| `promptId()` | The ID of the prompt that failed (returns `Optional<String>`) |
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
