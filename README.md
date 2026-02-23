# Agentle

> This docs was updated at: 2026-02-23

The Java agent framework built on OpenAI's Responses API.

![Coverage](https://img.shields.io/badge/coverage-88%25-brightgreen)
![Java](https://img.shields.io/badge/java-25%2B-blue)
![License](https://img.shields.io/badge/license-MIT-green)

## The problem

Every Java AI framework — LangChain4j, Spring AI — is built on the Chat Completions API. That API was designed for chatbots. OpenAI replaced it with the [Responses API](https://platform.openai.com/docs/api-reference/responses): item-based conversation state, native tool calling, structured output as a first-class primitive, and a design built for agents, not chat.

Agentle is the only Java framework built exclusively on the Responses API. No wrapping the old API. No compatibility layers. One clean abstraction over the API that was designed for what you're actually building.

If LangChain4j or Spring AI work for your use case, keep using them. If you need tool planning, multi-agent orchestration, structured streaming, or human-in-the-loop workflows — read on.

## Installation

Maven:
```xml
<dependency>
    <groupId>io.github.paragon-intelligence</groupId>
    <artifactId>agentle4j</artifactId>
    <version>0.7.0</version>
</dependency>
```

Gradle:
```groovy
implementation 'io.github.paragon-intelligence:agentle4j:0.7.0'
```

Requires Java 25+ (uses preview features like `StructuredTaskScope`).

## See it in action

### An agent with tools

```java
Agent agent = Agent.builder()
    .name("Assistant")
    .model("openai/gpt-4o")
    .instructions("You are a helpful assistant.")
    .responder(Responder.builder().openRouter().apiKey(key).build())
    .addTool(new GetWeatherTool())
    .build();

AgentResult result = agent.interact("What's the weather in Tokyo?");
System.out.println(result.output());

// Agents never throw. Errors live in the result.
if (result.isError()) {
    System.err.println(result.error().getMessage());
}
```

### Structured streaming with partial JSON

Stream structured output and watch fields populate in real-time. The parser auto-completes incomplete JSON as it arrives, so your UI updates progressively. No other Java framework does this.

```java
record Person(String name, int age, String occupation) {}

var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Create a fictional software engineer")
    .withStructuredOutput(Person.class)
    .streaming()
    .build();

responder.respond(payload)
    .onPartialJson(fields -> {
        // Fields arrive as they generate: first "name", then "age", then "occupation"
        if (fields.containsKey("name"))
            updateUI(fields.get("name").toString());
    })
    .onParsedComplete(parsed -> {
        Person p = parsed.outputParsed();  // Fully typed
    })
    .start();
```

### Tool planning with parallel execution

One line enables tool planning. The LLM batches tool calls into a dependency graph, the framework topologically sorts and executes them in parallel waves, and `$ref` references resolve between steps. One LLM round-trip instead of five.

```java
Agent agent = Agent.builder()
    .name("Researcher")
    .model("openai/gpt-4o")
    .instructions("You gather and compare data from multiple sources.")
    .responder(responder)
    .addTool(new GetWeatherTool())
    .addTool(new GetNewsTool())
    .addTool(new CompareDataTool())
    .enableToolPlanning()
    .build();

// LLM plans: getWeather("Tokyo") || getWeather("London") -> compareData(results)
// Framework executes in parallel, resolves references, returns final output
AgentResult result = agent.interact("Compare weather in Tokyo vs London");
```

### Human-in-the-loop

Agents pause at sensitive tools. State is serializable — save it to any database, resume hours or days later.

```java
@FunctionMetadata(name = "send_email", description = "Sends an email",
    requiresConfirmation = true)
public class SendEmailTool extends FunctionTool<EmailParams> { ... }

AgentResult result = agent.interact("Send the quarterly report to the team");

if (result.isPaused()) {
    AgentRunState state = result.pausedState();
    saveToDatabase(state);  // Serializable — persist anywhere
}

// Hours later, after approval in your web UI...
AgentRunState state = loadFromDatabase(runId);
state.approveToolCall("User approved via dashboard");
AgentResult resumed = agent.resume(state);
```

## Multi-agent patterns

Six patterns, all implementing `Interactable`. Swap any pattern without changing your service code.

![Multi-agent patterns overview](docs/media/multiagent_interaction.png)

| Pattern | Example | Use case |
|---------|---------|----------|
| **Router** | `RouterAgent.builder().addRoute(billing, "invoices, payments")...` | Classify and route to specialists |
| **Supervisor** | `SupervisorAgent.builder().addWorker(writer, "writes content")...` | Central coordinator with workers |
| **Parallel** | `ParallelAgents.of(researcher, analyst).runAll("analyze")` | Concurrent independent work |
| **Network** | `AgentNetwork.builder().addPeer(optimist).addPeer(pessimist)...` | Peer-to-peer multi-round debate |
| **Hierarchical** | `HierarchicalAgents.builder().executive(ceo).addDepartment(...)` | Org-chart workflows |
| **Sub-agent** | `.addSubAgent(analyst, "for deep analysis")` | Delegate, get result, continue |

```java
// Your service works with any pattern — same interface
public class AgentService {
    private final Interactable agent;

    public String process(String input) {
        return agent.interact(input).output();
    }
}

new AgentService(singleAgent);
new AgentService(router);
new AgentService(supervisor);
new AgentService(parallelTeam);
```

All patterns support streaming. See the [Agents Guide](docs/guides/agents.md) for full documentation.

## Dynamic tool selection

50 tools? 500? `ToolRegistry` sends only the relevant ones per request. No context window explosion.

```java
ToolRegistry registry = ToolRegistry.builder()
    .strategy(new BM25ToolSearchStrategy(5))     // Top 5 most relevant
    .eagerTool(helpTool)                          // Always available
    .deferredTools(List.of(tool1, tool2, ...))    // Only when relevant
    .build();

Agent agent = Agent.builder()
    .name("Assistant")
    .toolRegistry(registry)
    .responder(responder)
    .build();
```

Pluggable strategies: BM25, semantic similarity, regex, or write your own. See the [Tool Search Guide](docs/guides/tool-search.md).

## Blueprints — agents as JSON

Serialize any agent (or entire multi-agent constellation) to JSON. Store in a database, version in git, share across services, load at runtime. No recompilation.

```java
// Agent → JSON
String json = agent.toBlueprint().toJson();

// JSON → Agent (API keys auto-resolved from environment variables)
Interactable agent = new ObjectMapper()
    .readValue(json, InteractableBlueprint.class)
    .toInteractable();

agent.interact("Hello!");
```

Works with every pattern — `Agent`, `RouterAgent`, `SupervisorAgent`, `AgentNetwork`, `ParallelAgents`, `HierarchicalAgents`. Nested constellations serialize recursively: a Router containing a Supervisor containing three Agents becomes one JSON file.

### JSON-first agent definitions

Skip Java builders entirely. Write a JSON file, deserialize, run.

```json
{
  "type": "agent",
  "name": "CustomerSupport",
  "model": "openai/gpt-4o",
  "instructions": "You are a professional support agent for Acme Corp.",
  "maxTurns": 15,
  "responder": {
    "provider": "OPEN_ROUTER",
    "apiKeyEnvVar": "OPENROUTER_API_KEY"
  },
  "toolClassNames": ["com.acme.tools.SearchKnowledgeBase", "com.acme.tools.CreateTicket"],
  "handoffs": [],
  "inputGuardrails": [{ "registryId": "profanity_filter" }],
  "outputGuardrails": []
}
```

```java
String json = Files.readString(Path.of("agents/support.json"));
Interactable agent = new ObjectMapper()
    .readValue(json, InteractableBlueprint.class)
    .toInteractable();
```

### LLM-generated agents

`AgentDefinition` is designed for structured output — an LLM creates agents at runtime.

```java
Interactable.Structured<AgentDefinition> metaAgent = Agent.builder()
    .name("AgentFactory")
    .model("openai/gpt-4o")
    .instructions("You create agent definitions. Available tools: search_kb, create_ticket.")
    .structured(AgentDefinition.class)
    .responder(responder)
    .build();

AgentDefinition def = metaAgent.interactStructured(
    "Create a Spanish-speaking support agent"
).output();

// LLM decides behavior — you provide infrastructure
Interactable agent = def.toInteractable(responder, "openai/gpt-4o", availableTools);
agent.interact("¿Cómo puedo recuperar mi contraseña?");
```

See the [Blueprints Guide](docs/guides/blueprints.md) for the full JSON schema reference, multi-agent serialization examples, and Spring Boot integration.

## Everything else

Agentle ships with more than agents. Each feature has a dedicated guide.

- **[MCP Client](docs/guides/mcp-client.md)** — Connect to Model Context Protocol servers via stdio or HTTP. Tools appear as native `FunctionTool`s.
- **[Skills](docs/guides/agents.md)** — Modular expertise (SKILL.md files) injected into agent prompts. Reusable knowledge, not isolated sub-agents.
- **[Web Extraction](docs/guides/web-extraction.md)** — Playwright renders the page, LLM extracts structured data. `WebExtractor.create(responder, model)`.
- **[Guardrails](docs/guides/agents.md#guardrails)** — Input/output validation. Block dangerous prompts, enforce constraints, fail before the LLM runs.
- **[Context Management](docs/guides/context-management.md)** — Sliding window or LLM-powered summarization for long conversations. Pluggable strategies.
- **[Memory](docs/guides/agents.md)** — Persistent cross-conversation memory. `agent.addMemoryTools(memory)` — the agent stores and retrieves on its own.
- **[Prompt Builder](docs/guides/prompt-management.md)** — Fluent API with chain-of-thought, few-shot examples, templates, and multi-language support.
- **[Observability](docs/guides/observability.md)** — Built-in OpenTelemetry. Traces span across agent handoffs and parallel execution. One line: `.addTelemetryProcessor(LangfuseProcessor.fromEnv())`.
- **[Vision](docs/guides/vision.md)** — Multi-modal input with `Image.fromUrl()`, base64, or file ID. Control detail level per image.
- **[Messaging](docs/guides/messaging.md)** — WhatsApp/Telegram bots with adaptive batching, rate limiting, and conversation history.
- **[Embeddings](docs/guides/embeddings.md)** — Text embeddings with automatic retry on 429/5xx and provider fallbacks.
- **[Streaming](docs/guides/streaming.md)** — Text deltas, tool call events, structured output — all via virtual-thread callbacks.
- **[Tools](docs/guides/tools.md)** — Type-safe function tools using Java records. Auto-generated JSON schemas from generics.
- **[Tool Planning](docs/guides/tool-planning.md)** — DAG-based parallel tool execution with reference resolution between steps.

## How it compares

| | **Agentle** | **LangChain4j** | **Spring AI** |
|---|---|---|---|
| API | Responses API | Chat Completions | Chat Completions |
| Java version | 25+ (virtual threads, records, sealed classes) | 17+ | 17+ |
| Multi-agent patterns | 6 built-in | Limited | Limited |
| Structured streaming | Partial JSON as it generates | No | No |
| Tool planning | DAG with parallel execution | No | No |
| Human-in-the-loop | Serializable pause/resume | No | Manual |
| Dynamic tool selection | BM25 / semantic / custom | No | No |
| Streaming model | Virtual thread callbacks | Callbacks | Reactor Flux |
| Error model | `AgentResult` (never throws) | Exceptions | Exceptions |
| MCP support | stdio + HTTP | No | Limited |
| Observability | Built-in OpenTelemetry | Plugin | Plugin |

LangChain4j and Spring AI have broader provider support through native integrations and offer Spring Boot / Quarkus starters. Agentle requires a Responses API-compatible provider (OpenAI, OpenRouter, or any compatible endpoint). Pick based on what your project needs.

## Provider support

Works with any provider that implements the Responses API:

```java
// OpenRouter — 300+ models
Responder.builder().openRouter().apiKey(key).build();

// OpenAI direct
Responder.builder().openAi().apiKey(key).build();

// Groq
Responder.builder()
    .baseUrl(HttpUrl.parse("https://api.groq.com/openai/v1"))
    .apiKey(key).build();

// Local Ollama
Responder.builder()
    .baseUrl(HttpUrl.parse("http://localhost:11434/v1"))
    .build();
```

## Get started

```xml
<dependency>
    <groupId>io.github.paragon-intelligence</groupId>
    <artifactId>agentle4j</artifactId>
    <version>0.7.0</version>
</dependency>
```

Then explore:

- [Getting Started Guide](docs/getting-started.md) — First agent in 5 minutes
- [Full Documentation](docs/index.md) — Guides, API reference, examples
- [Code Examples](docs/examples/code-samples.md) — Copy-paste ready snippets

## Contributing

```bash
make build      # Build
make test       # Run tests
make format     # Format code
```

## License

MIT
