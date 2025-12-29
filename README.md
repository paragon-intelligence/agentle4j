# Agentle

A Java library for the OpenAI Responses API, with first-class support for agents, streaming, and structured outputs.

Requires Java 21+.

## Installation

Maven:
```xml
<dependency>
    <groupId>io.github.paragon-intelligence</groupId>
    <artifactId>agentle4j</artifactId>
    <version>0.1.0</version>
</dependency>
```

Gradle:
```groovy
implementation 'io.github.paragon-intelligence:agentle4j:0.1.0'
```

## Why Agentle?

Most Java GenAI libraries (LangChain4J, Spring AI) are built around the Chat Completions API. Agentle focuses exclusively on OpenAI's newer [Responses API](https://platform.openai.com/docs/api-reference/responses), which has built-in conversation state and a cleaner item-based design.

The tradeoff is clear: if you need Chat Completions compatibility or extensive RAG infrastructure, use LangChain4J or Spring AI. If you want Responses API support with proper streaming, agents, and observability, Agentle is worth considering.

## Quick examples

### Basic request

```java
Responder responder = Responder.builder()
    .openRouter()
    .apiKey("your-api-key")
    .build();

var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o-mini")
    .addDeveloperMessage("You are a helpful assistant.")
    .addUserMessage("Hello!")
    .build();

Response response = responder.respond(payload).join();
System.out.println(response.outputText());
```

### Streaming

```java
var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Write a poem about Java")
    .streaming()
    .build();

responder.respond(payload)
    .onTextDelta(System.out::print)
    .onComplete(response -> System.out.println("\nDone: " + response.id()))
    .onError(Throwable::printStackTrace)
    .start();
```

### Structured output

```java
public record Person(String name, int age, String occupation) {}

var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Create a fictional software engineer")
    .withStructuredOutput(Person.class)
    .build();

ParsedResponse<Person> response = responder.respond(payload).join();
Person person = response.outputParsed();
```

You can also stream structured output and get partial updates as JSON is generated:

```java
responder.respond(structuredPayload)
    .onPartialJson(fields -> {
        // fields is a Map that updates as JSON streams in
        if (fields.containsKey("name")) {
            updateUI(fields.get("name").toString());
        }
    })
    .onParsedComplete(parsed -> {
        Person p = parsed.outputParsed();
    })
    .start();
```

This is useful for real-time UIs. The parser auto-completes incomplete JSON, so you see fields populate progressively.

## Core concepts

### Responder

The low-level HTTP client. Handles API communication, streaming, retries, and telemetry. Use it directly for simple one-shot requests or when you need fine-grained control.

### Agent

A higher-level abstraction that wraps a Responder with:
- Instructions (system prompt)
- Tools (functions the AI can call)
- Guardrails (input/output validation)
- Memory (cross-conversation persistence)
- Handoffs (routing to other agents)

Agents are stateless and thread-safe. The same instance can handle concurrent conversations because state lives in `AgentContext`.

```java
Agent agent = Agent.builder()
    .name("Assistant")
    .model("openai/gpt-4o")
    .instructions("You are a helpful assistant.")
    .responder(responder)
    .addTool(weatherTool)
    .build();

AgentResult result = agent.interact("What's the weather in Tokyo?").join();
```

### AgentContext

Per-conversation state. Holds message history, turn count, and custom key-value data.

```java
AgentContext context = AgentContext.create();
context.setState("userId", "user-123");

// Later
String userId = context.getState("userId", String.class);
```

This separation means you can serialize context to pause/resume conversations, and agents stay reusable without side effects.

## Function calling

Define tools with a class that extends `FunctionTool`:

```java
public record WeatherParams(String location, String unit) {}

@FunctionMetadata(
    name = "get_weather",
    description = "Gets current weather for a location")
public class WeatherTool extends FunctionTool<WeatherParams> {
    @Override
    public FunctionToolCallOutput call(WeatherParams params) {
        // Your implementation
        return FunctionToolCallOutput.success("25°C and sunny");
    }
}
```

Register and use:

```java
FunctionToolStore store = FunctionToolStore.create(objectMapper);
store.add(new WeatherTool());

var payload = CreateResponsePayload.builder()
    .model("gpt-4o")
    .addUserMessage("What's the weather in Tokyo?")
    .addTool(weatherTool)
    .build();

Response response = responder.respond(payload).join();
for (var toolCall : response.functionToolCalls(store)) {
    System.out.println(toolCall.call());
}
```

Tool calls also work during streaming:

```java
responder.respond(streamingPayload)
    .onToolCall((toolName, argsJson) -> {
        System.out.println("Tool called: " + toolName);
    })
    .withToolStore(toolStore)
    .onToolResult((toolName, result) -> {
        System.out.println(toolName + " returned: " + result.output());
    })
    .start();
```

## Multi-agent patterns

### Handoffs

![Multi-Agent](docs/media/multiagent.png)

Route conversations between specialized agents:

```java
Agent billingAgent = Agent.builder()
    .name("BillingSpecialist")
    .instructions("You handle billing inquiries.")
    // ...
    .build();

Agent frontDesk = Agent.builder()
    .name("FrontDesk")
    .instructions("Route to specialists as needed.")
    .addHandoff(Handoff.to(billingAgent).description("billing issues").build())
    .build();

AgentResult result = frontDesk.interact("I have a question about my invoice").join();
if (result.isHandoff()) {
    System.out.println("Handled by: " + result.handoffAgent().name());
}
```

### RouterAgent

![Routing](docs/media/routing.png)

For pure classification without conversational noise:

```java
RouterAgent router = RouterAgent.builder()
    .model("openai/gpt-4o-mini")
    .responder(responder)
    .addRoute(billingAgent, "billing, invoices, payments")
    .addRoute(techSupport, "technical issues, bugs, errors")
    .addRoute(salesAgent, "pricing, demos, upgrades")
    .fallback(techSupport)
    .build();

Agent selected = router.classify("My app keeps crashing").join();
// selected == techSupport
```

### Parallel execution

![Parallel Agents](docs/media/parallel_agents.png)

Run multiple agents concurrently:

```java
ParallelAgents team = ParallelAgents.of(researcher, analyst);

List<AgentResult> results = team.run("Analyze market trends");

// Or get just the first to complete
AgentResult fastest = team.runFirst("Quick analysis needed");

// Or combine outputs with a synthesizer agent
AgentResult combined = team.runAndSynthesize("What's the outlook?", writerAgent);
```

## Guardrails

![Guardrails](docs/media/guardrails.png)

Validate inputs and outputs:

```java
Agent agent = Agent.builder()
    .name("SafeAssistant")
    .addInputGuardrail((input, ctx) -> {
        if (input.contains("password")) {
            return GuardrailResult.failed("Cannot discuss passwords");
        }
        return GuardrailResult.passed();
    })
    .addOutputGuardrail((output, ctx) -> {
        if (output.length() > 5000) {
            return GuardrailResult.failed("Response too long");
        }
        return GuardrailResult.passed();
    })
    .build();
```

## Human-in-the-loop

![Human-in-the-Loop](docs/media/hitl.png)

Mark sensitive tools that require approval:

```java
@FunctionMetadata(
    name = "send_email",
    description = "Sends an email",
    requiresConfirmation = true)
public class SendEmailTool extends FunctionTool<EmailParams> { ... }
```

When the agent hits this tool, it pauses:

```java
AgentResult result = agent.interact("Send an email to John").join();

if (result.isPaused()) {
    AgentRunState state = result.pausedState();
    FunctionToolCall pending = state.pendingToolCall();
    
    System.out.println("Approval needed: " + pending.name());
    
    if (userApproves()) {
        state.approveToolCall("User approved");
    } else {
        state.rejectToolCall("User denied");
    }
    
    result = agent.resume(state);
}
```

`AgentRunState` is serializable, so you can persist it to a database for async approval workflows that take hours or days.

## Context management

Control conversation length with pluggable strategies:

```java
Agent agent = Agent.builder()
    .contextManagement(ContextManagementConfig.builder()
        .strategy(new SlidingWindowStrategy())
        .maxTokens(4000)
        .build())
    .build();

// Or use summarization
Agent agent = Agent.builder()
    .contextManagement(ContextManagementConfig.builder()
        .strategy(SummarizationStrategy.withResponder(responder, "gpt-4o-mini"))
        .maxTokens(4000)
        .build())
    .build();
```

## Memory

![Memory](docs/media/memory.png)

Persistent memory across conversations:

```java
Memory memory = InMemoryMemory.create();

Agent agent = Agent.builder()
    .addMemoryTools(memory)  // Adds store/retrieve tools
    .build();

agent.interact("My favorite color is blue");
// Later...
agent.interact("What's my favorite color?");
// -> "Your favorite color is blue"
```

## Observability

Built-in OpenTelemetry support:

```java
Responder responder = Responder.builder()
    .openRouter()
    .apiKey(apiKey)
    .addTelemetryProcessor(LangfuseProcessor.fromEnv())
    .build();
```

Traces automatically span across agent handoffs and parallel executions.

## Provider support

Agentle works with any provider that implements the Responses API:

```java
// OpenRouter (300+ models)
Responder.builder().openRouter().apiKey(key).build();

// OpenAI direct
Responder.builder().openAi().apiKey(key).build();

// Groq
Responder.builder()
    .baseUrl(HttpUrl.parse("https://api.groq.com/openai/v1"))
    .apiKey(key)
    .build();

// Local Ollama
Responder.builder()
    .baseUrl(HttpUrl.parse("http://localhost:11434/v1"))
    .build();
```

The tradeoff vs. LangChain4J/Spring AI: they have native integrations per provider (more work to maintain, but works with any API). Agentle relies on Responses API compatibility (less maintenance, but limited to providers that support it).

## Error handling

Typed exceptions for different failure modes:

```java
try {
    Response response = responder.respond(payload).join();
} catch (CompletionException e) {
    switch (e.getCause()) {
        case RateLimitException ex -> {
            System.err.println("Rate limited. Retry after: " + ex.retryAfter());
        }
        case AuthenticationException ex -> {
            System.err.println("Auth failed: " + ex.suggestion());
        }
        case ServerException ex -> {
            System.err.println("Server error: " + ex.statusCode());
        }
        default -> throw e;
    }
}
```

All exceptions have `isRetryable()`. The library also has built-in retry with exponential backoff for 429s and 5xx errors:

```java
Responder.builder()
    .maxRetries(5)
    .retryPolicy(RetryPolicy.builder()
        .initialDelay(Duration.ofMillis(500))
        .maxDelay(Duration.ofSeconds(30))
        .build())
    .build();
```

## Configuration

```java
CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .temperature(0.7)
    .maxOutputTokens(1000)
    .toolChoice(ToolChoiceMode.REQUIRED)
    .reasoning(new ReasoningConfig(...))
    .build();
```

## Comparison with alternatives

| | Agentle | LangChain4J | Spring AI |
|---|---|---|---|
| API | Responses API only | Chat Completions | Custom abstraction |
| Java version | 21+ | 17+ | 17+ |
| Streaming | Virtual thread callbacks | Callbacks | Reactor Flux |
| Structured streaming | Yes (`onPartialJson`) | No | No |
| Multi-provider | Via OpenRouter or Responses API compat | Native integrations | Native integrations |
| RAG | Via tool calling | Built-in | Built-in |
| Spring/Quarkus starters | No | Yes | Yes |

LangChain4J and Spring AI are more mature and have broader provider support. Agentle's advantages are:

1. **Responses API focus** — cleaner API design, built-in conversation state
2. **Structured streaming** — partial JSON parsing during generation
3. **Streaming tool calls** — `onToolCall` callbacks during streaming
4. **Simpler async model** — virtual thread callbacks instead of Reactor

Pick based on what matters for your use case.

## Not yet supported

- Spring Boot / Quarkus starters
- Built-in vector store integrations (use tool calling instead)
- Document loaders
- Chat Completions API fallback

## Development

```bash
make build      # Build
make test       # Run tests
make format     # Format code
make benchmark  # Performance benchmarks
```

## License

MIT