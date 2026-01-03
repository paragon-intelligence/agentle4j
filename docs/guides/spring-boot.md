# Spring Boot Integration

This guide shows how to integrate Agentle4j into a Spring Boot application with best practices for production.

---

## Overview

Agentle4j integrates seamlessly with Spring Boot:

- **Responder as a Bean** - Thread-safe, reusable HTTP client
- **Agents as Services** - AI-powered business logic
- **REST Controllers** - Expose AI capabilities via API
- **SSE Streaming** - Real-time responses to clients
- **WebSocket** - Bidirectional chat interfaces

---

## Configuration

### Application Properties

```yaml
# application.yml
agentle4j:
  api-key: ${OPENROUTER_API_KEY}
  model: openai/gpt-4o
  temperature: 0.7
  max-retries: 3
```

### Configuration Class

```java
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "agentle4j")
public class Agentle4jProperties {
    private String apiKey;
    private String model = "openai/gpt-4o";
    private double temperature = 0.7;
    private int maxRetries = 3;
    
    // Getters and setters
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
}
```

---

## Dependency Injection

### Responder as a Singleton Bean

```java
import com.paragon.responses.Responder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Agentle4jConfig {

    @Bean
    public Responder responder(Agentle4jProperties props) {
        return Responder.builder()
            .openRouter()
            .apiKey(props.getApiKey())
            .maxRetries(props.getMaxRetries())
            .build();
    }
}
```

!!! tip "Thread Safety"
    `Responder` is thread-safe and reusable. Create it once as a singleton bean.

### Agent as a Service

```java
import com.paragon.agents.Agent;
import org.springframework.stereotype.Service;

@Service
public class CustomerSupportAgent {

    private final Agent agent;
    private final Agentle4jProperties props;

    public CustomerSupportAgent(Responder responder, Agentle4jProperties props) {
        this.props = props;
        this.agent = Agent.builder()
            .name("CustomerSupport")
            .model(props.getModel())
            .instructions("""
                You are a helpful customer support assistant.
                Be concise, friendly, and professional.
                """)
            .responder(responder)
            .build();
    }

    public String chat(String userMessage) {
        return agent.interact(userMessage).join().output();
    }
    
    public CompletableFuture<AgentResult> chatAsync(String userMessage) {
        return agent.interact(userMessage);
    }
}
```

---

## Managing Multiple Agents

As your application grows, you'll likely need multiple agents with different purposes. Instead of creating a separate `@Service` wrapper for each agent, you can use Spring's dependency injection patterns to manage agents more elegantly.

### Prompt Management with PromptProvider

Before defining agents, consider externalizing your prompts. Agentle4j provides the `PromptProvider` interface with built-in implementations:

#### Filesystem-Based Prompts

Store prompts as text files and load them at runtime:

```java
import com.paragon.prompts.PromptProvider;
import com.paragon.prompts.FilesystemPromptProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PromptConfig {

    @Bean
    public PromptProvider filePromptProvider() {
        return FilesystemPromptProvider.create("./prompts");
    }
}
```

Create prompt files in the `prompts/` directory:

```text
# prompts/question-generator.txt
You are an expert question creator. Generate high-quality questions based on the given content.

Requirements:
- Questions should be clear and unambiguous
- Include a mix of difficulty levels
- Cover key concepts from the source material
```

#### Langfuse-Managed Prompts

For production environments with prompt versioning and A/B testing:

```java
import com.paragon.prompts.LangfusePromptProvider;
import okhttp3.OkHttpClient;

@Configuration
public class PromptConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(30))
            .build();
    }

    @Bean
    public PromptProvider langfusePromptProvider(OkHttpClient httpClient) {
        return LangfusePromptProvider.builder()
            .httpClient(httpClient)
            .publicKey(System.getenv("LANGFUSE_PUBLIC_KEY"))
            .secretKey(System.getenv("LANGFUSE_SECRET_KEY"))
            .build();
    }
}
```

Retrieve prompts with version or label filters:

```java
// Get latest version
Prompt prompt = provider.providePrompt("question-generator");

// Get specific version
Prompt v2 = provider.providePrompt("question-generator", Map.of("version", "2"));

// Get production label
Prompt prod = provider.providePrompt("question-generator", Map.of("label", "production"));
```

#### Template Compilation

Prompts support Handlebars-like templating:

```java
Prompt template = Prompt.of("""
    You are a {{role}} assistant for {{company}}.
    
    {{#if includeDisclaimer}}
    Always remind users this is AI-generated content.
    {{/if}}
    
    Topics you handle:
    {{#each topics}}
    - {{this}}
    {{/each}}
    """);

Prompt compiled = template.compile(Map.of(
    "role", "customer support",
    "company", "Acme Corp",
    "includeDisclaimer", true,
    "topics", List.of("billing", "technical issues", "returns")
));
```

---

### Direct Agent Injection with @Qualifier

For a known, finite set of agents, define each as a Spring bean:

```java
import com.paragon.agents.Agent;
import com.paragon.prompts.Prompt;
import com.paragon.prompts.PromptProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {

    @Bean("questionAgent")
    public Agent questionAgent(Responder responder, 
                               PromptProvider prompts,
                               Agentle4jProperties props) {
        return Agent.builder()
            .name("QuestionGenerator")
            .model(props.getModel())
            .instructions(prompts.providePrompt("question-generator.txt"))
            .responder(responder)
            .build();
    }

    @Bean("summarizationAgent")
    public Agent summarizationAgent(Responder responder, 
                                    PromptProvider prompts,
                                    Agentle4jProperties props) {
        return Agent.builder()
            .name("Summarizer")
            .model(props.getModel())
            .instructions(prompts.providePrompt("summarizer.txt"))
            .responder(responder)
            .build();
    }

    @Bean("reviewerAgent")
    public Agent reviewerAgent(Responder responder, 
                               PromptProvider prompts,
                               Agentle4jProperties props) {
        return Agent.builder()
            .name("Reviewer")
            .model(props.getModel())
            .instructions(prompts.providePrompt("reviewer.txt"))
            .responder(responder)
            .build();
    }
}
```

Now inject agents directly into your use case classes:

```java
import com.paragon.agents.Agent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CriarQuestaoUseCase {

    private final Agent questionAgent;

    public CriarQuestaoUseCase(@Qualifier("questionAgent") Agent questionAgent) {
        this.questionAgent = questionAgent;
    }

    public Questao criarQuestao(CriarQuestaoRequest input) {
        var result = questionAgent.interact(input.content()).join();
        return parseQuestaoFromResult(result);
    }
}
```

```java
@Service
public class SummarizeDocumentUseCase {

    private final Agent summarizationAgent;

    public SummarizeDocumentUseCase(@Qualifier("summarizationAgent") Agent agent) {
        this.summarizationAgent = agent;
    }

    public Summary summarize(Document doc) {
        return summarizationAgent.interact(doc.content())
            .thenApply(result -> new Summary(result.output()))
            .join();
    }
}
```

!!! tip "Thread Safety"
    Agents are **stateless and thread-safe**. The same agent instance can handle concurrent requests because conversation state lives in `AgentContext`, not the agent itself.

---

### Agent Factory Pattern

For dynamic agent creation or when agents are determined at runtime:

```java
import com.paragon.agents.Agent;
import com.paragon.prompts.PromptProvider;
import org.springframework.stereotype.Component;

@Component
public class AgentFactory {

    private final Responder responder;
    private final PromptProvider prompts;
    private final Agentle4jProperties props;

    public AgentFactory(Responder responder, 
                        PromptProvider prompts, 
                        Agentle4jProperties props) {
        this.responder = responder;
        this.prompts = prompts;
        this.props = props;
    }

    /**
     * Creates an agent by loading its prompt from the filesystem.
     * 
     * @param agentName name used for both the agent and prompt file lookup
     * @return configured Agent instance
     */
    public Agent create(String agentName) {
        return Agent.builder()
            .name(agentName)
            .model(props.getModel())
            .instructions(prompts.providePrompt(agentName + ".txt"))
            .responder(responder)
            .build();
    }

    /**
     * Creates an agent with custom configuration.
     */
    public Agent create(String agentName, String model, Prompt instructions) {
        return Agent.builder()
            .name(agentName)
            .model(model)
            .instructions(instructions)
            .responder(responder)
            .build();
    }
}
```

Usage:

```java
@Service
public class DynamicAgentUseCase {

    private final AgentFactory agentFactory;

    public DynamicAgentUseCase(AgentFactory agentFactory) {
        this.agentFactory = agentFactory;
    }

    public String processWithAgent(String agentType, String input) {
        Agent agent = agentFactory.create(agentType);
        return agent.interact(input).join().output();
    }
}
```

---

### Agent Registry Pattern

For centralized management with caching (avoids recreating agents):

```java
import com.paragon.agents.Agent;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentRegistry {

    private final ConcurrentHashMap<String, Agent> agents = new ConcurrentHashMap<>();
    private final AgentFactory factory;

    public AgentRegistry(AgentFactory factory) {
        this.factory = factory;
    }

    /**
     * Gets or creates an agent by name.
     * Agents are cached after first creation.
     */
    public Agent get(String name) {
        return agents.computeIfAbsent(name, factory::create);
    }

    /**
     * Checks if an agent is registered.
     */
    public boolean has(String name) {
        return agents.containsKey(name);
    }

    /**
     * Registers a pre-configured agent.
     */
    public void register(String name, Agent agent) {
        agents.put(name, agent);
    }

    /**
     * Returns all registered agent names.
     */
    public Set<String> registeredAgents() {
        return Set.copyOf(agents.keySet());
    }
}
```

Usage in use cases:

```java
@Service
public class FlexibleProcessingUseCase {

    private final AgentRegistry agentRegistry;

    public FlexibleProcessingUseCase(AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
    }

    public ProcessingResult process(String agentName, String input) {
        Agent agent = agentRegistry.get(agentName);
        AgentResult result = agent.interact(input).join();
        return new ProcessingResult(result.output(), result.turnsUsed());
    }
}
```

---

### RouterAgent Injection

For classification and routing use cases:

```java
import com.paragon.agents.Agent;
import com.paragon.agents.RouterAgent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouterConfig {

    @Bean
    public RouterAgent supportRouter(
            Responder responder,
            Agentle4jProperties props,
            @Qualifier("billingAgent") Agent billing,
            @Qualifier("techSupportAgent") Agent techSupport,
            @Qualifier("salesAgent") Agent sales) {
        
        return RouterAgent.builder()
            .model(props.getModel())
            .responder(responder)
            .addRoute(billing, "billing, invoices, payments, refunds")
            .addRoute(techSupport, "technical issues, bugs, errors, crashes")
            .addRoute(sales, "pricing, demos, upgrades, new features")
            .fallback(techSupport)
            .build();
    }
}
```

Use the router to classify and dispatch:

```java
@Service
public class CustomerInquiryUseCase {

    private final RouterAgent router;

    public CustomerInquiryUseCase(RouterAgent router) {
        this.router = router;
    }

    public InquiryResponse handle(String customerMessage) {
        // Classify the message
        Agent selectedAgent = router.classify(customerMessage).join();
        
        // Process with the appropriate agent
        AgentResult result = selectedAgent.interact(customerMessage).join();
        
        return new InquiryResponse(
            selectedAgent.name(),
            result.output()
        );
    }
}
```

---

### Lazy Agent Initialization

For agents that are rarely used, use `@Lazy` to defer initialization:

```java
@Configuration
public class AgentConfig {

    @Bean("expensiveAgent")
    @Lazy
    public Agent expensiveAgent(Responder responder, PromptProvider prompts) {
        // Only created when first injected/used
        return Agent.builder()
            .name("ExpensiveAnalyzer")
            .model("openai/gpt-4-turbo")  // More expensive model
            .instructions(prompts.providePrompt("expensive-analyzer.txt"))
            .responder(responder)
            .build();
    }
}
```

Or use `ObjectProvider` for optional/conditional injection:

```java
import org.springframework.beans.factory.ObjectProvider;

@Service
public class ConditionalUseCase {

    private final ObjectProvider<Agent> expensiveAgentProvider;

    public ConditionalUseCase(
            @Qualifier("expensiveAgent") ObjectProvider<Agent> expensiveAgentProvider) {
        this.expensiveAgentProvider = expensiveAgentProvider;
    }

    public String process(String input, boolean useExpensiveModel) {
        if (useExpensiveModel) {
            Agent agent = expensiveAgentProvider.getIfAvailable();
            if (agent != null) {
                return agent.interact(input).join().output();
            }
        }
        // Fallback to cheaper processing
        return processWithCheaperMethod(input);
    }
}
```

---

### Best Practices for Organizing Agents

When your application has 10+ agents, organization becomes critical:

#### Package Structure

```
com.yourapp/
├── agents/
│   ├── config/
│   │   ├── AgentConfig.java        # Bean definitions
│   │   ├── RouterConfig.java       # Router definitions
│   │   └── PromptConfig.java       # PromptProvider setup
│   ├── factory/
│   │   ├── AgentFactory.java
│   │   └── AgentRegistry.java
│   └── tools/
│       ├── WeatherTool.java
│       └── SearchTool.java
├── prompts/                         # If using filesystem prompts
│   ├── question-generator.txt
│   ├── summarizer.txt
│   └── reviewer.txt
└── usecases/
    ├── CriarQuestaoUseCase.java
    └── SummarizeDocumentUseCase.java
```

#### Naming Conventions

| Item | Convention | Example |
|------|------------|---------|
| Agent bean | `{purpose}Agent` | `questionAgent`, `summarizationAgent` |
| Prompt file | `{purpose}.txt` or `{purpose}-{version}.txt` | `question-generator.txt`, `summarizer-v2.txt` |
| Router bean | `{domain}Router` | `supportRouter`, `contentRouter` |
| Use case | `{Action}{Entity}UseCase` | `CriarQuestaoUseCase`, `SummarizeDocumentUseCase` |

#### Configuration via YAML

Define agent metadata in `application.yml`:

```yaml
agentle4j:
  api-key: ${OPENROUTER_API_KEY}
  default-model: openai/gpt-4o
  
  agents:
    question-generator:
      prompt-file: question-generator.txt
      model: openai/gpt-4o
      temperature: 0.7
    summarizer:
      prompt-file: summarizer.txt
      model: openai/gpt-4o-mini
      temperature: 0.3
    reviewer:
      prompt-file: reviewer.txt
      model: openai/gpt-4o
      temperature: 0.5
```

And create a configuration properties class:

```java
@Configuration
@ConfigurationProperties(prefix = "agentle4j")
public class AgentConfigProperties {
    
    private String defaultModel = "openai/gpt-4o";
    private Map<String, AgentDefinition> agents = new HashMap<>();
    
    // Getters and setters
    
    public static class AgentDefinition {
        private String promptFile;
        private String model;
        private double temperature = 0.7;
        
        // Getters and setters
    }
}
```

#### Key Reminders

!!! important "Thread Safety"
    Agents are **stateless and thread-safe**. Create them once and reuse. State belongs in `AgentContext`.

!!! tip "Prompt Versioning"
    Use Langfuse labels (`production`, `staging`) or git-versioned prompt files for safe deployments.

!!! note "Memory Usage"
    Each `Agent` instance is lightweight (~1KB). Having 50+ agent beans is not a memory concern.

---

## REST API Endpoints

### Simple Chat Controller

```java
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final CustomerSupportAgent agent;

    public ChatController(CustomerSupportAgent agent) {
        this.agent = agent;
    }

    // Synchronous endpoint
    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String response = agent.chat(request.message());
        return new ChatResponse(response);
    }

    // Async endpoint (non-blocking)
    @PostMapping("/async")
    public CompletableFuture<ChatResponse> chatAsync(@RequestBody ChatRequest request) {
        return agent.chatAsync(request.message())
            .thenApply(result -> new ChatResponse(result.output()));
    }

    public record ChatRequest(String message) {}
    public record ChatResponse(String response) {}
}
```

### Structured Output Endpoint

```java
import com.paragon.responses.Responder;
import com.paragon.responses.spec.CreateResponsePayload;

@RestController
@RequestMapping("/api/analyze")
public class AnalysisController {

    private final Responder responder;
    private final Agentle4jProperties props;

    public AnalysisController(Responder responder, Agentle4jProperties props) {
        this.responder = responder;
        this.props = props;
    }

    // Extract structured data
    @PostMapping("/sentiment")
    public CompletableFuture<SentimentResult> analyzeSentiment(@RequestBody TextInput input) {
        var payload = CreateResponsePayload.builder()
            .model(props.getModel())
            .addUserMessage("Analyze sentiment: " + input.text())
            .withStructuredOutput(SentimentResult.class)
            .build();

        return responder.respond(payload)
            .thenApply(response -> response.outputParsed());
    }

    public record TextInput(String text) {}
    public record SentimentResult(
        String sentiment,
        double confidence,
        List<String> keywords
    ) {}
}
```

---

## Server-Sent Events (SSE) Streaming

Stream AI responses in real-time to the client:

```java
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/stream")
public class StreamingController {

    private final Responder responder;
    private final Agentle4jProperties props;

    public StreamingController(Responder responder, Agentle4jProperties props) {
        this.responder = responder;
        this.props = props;
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestParam String message) {
        SseEmitter emitter = new SseEmitter(60_000L); // 60s timeout

        var payload = CreateResponsePayload.builder()
            .model(props.getModel())
            .addUserMessage(message)
            .streaming()
            .build();

        responder.respond(payload)
            .onTextDelta(delta -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("delta")
                        .data(delta));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            .onComplete(response -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("complete")
                        .data(Map.of("tokens", response.usage().totalTokens())));
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            .onError(emitter::completeWithError)
            .start();

        return emitter;
    }
}
```

### Client-Side JavaScript

```javascript
const eventSource = new EventSource('/api/stream/chat?message=Hello');

eventSource.addEventListener('delta', (e) => {
    document.getElementById('output').textContent += e.data;
});

eventSource.addEventListener('complete', (e) => {
    const data = JSON.parse(e.data);
    console.log('Tokens used:', data.tokens);
    eventSource.close();
});

eventSource.onerror = () => eventSource.close();
```

### Streaming Structured Outputs (Partial JSON)

Stream structured data with partial JSON updates as fields are generated:

```java
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/stream")
public class StructuredStreamingController {

    private final Responder responder;
    private final Agentle4jProperties props;

    public StructuredStreamingController(Responder responder, Agentle4jProperties props) {
        this.responder = responder;
        this.props = props;
    }

    // Define your structured output type
    public record Article(
        String title,
        String summary,
        List<String> keyPoints,
        String author
    ) {}

    @GetMapping(value = "/article", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamStructuredArticle(@RequestParam String topic) {
        SseEmitter emitter = new SseEmitter(120_000L); // 2 min timeout for longer content

        var payload = CreateResponsePayload.builder()
            .model(props.getModel())
            .addUserMessage("Write an article about: " + topic)
            .withStructuredOutput(Article.class)
            .streaming()
            .build();

        responder.respond(payload)
            // Stream partial JSON as fields are generated
            .onPartialJson(fields -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("partial")
                        .data(fields));  // Send Map<String, Object> with current fields
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            // Get the final typed object
            .onParsedComplete(response -> {
                try {
                    Article article = response.outputParsed();
                    emitter.send(SseEmitter.event()
                        .name("complete")
                        .data(article));
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            .onError(emitter::completeWithError)
            .start();

        return emitter;
    }
}
```

!!! info "How Partial JSON Works"
    The parser auto-completes incomplete JSON. As the LLM generates text, you'll receive progressive updates:
    
    - First: `{title: "AI in Healthcare"}`
    - Then: `{title: "AI in Healthcare", summary: "Artificial intel..."}`
    - Finally: Complete object with all fields

#### Client-Side JavaScript for Structured Streaming

```javascript
const eventSource = new EventSource('/api/stream/article?topic=AI%20in%20Healthcare');

eventSource.addEventListener('partial', (e) => {
    const fields = JSON.parse(e.data);
    
    // Update UI fields as they arrive
    if (fields.title) {
        document.getElementById('title').textContent = fields.title;
    }
    if (fields.summary) {
        document.getElementById('summary').textContent = fields.summary;
    }
    if (fields.keyPoints) {
        const list = document.getElementById('keyPoints');
        list.innerHTML = fields.keyPoints.map(p => `<li>${p}</li>`).join('');
    }
});

eventSource.addEventListener('complete', (e) => {
    const article = JSON.parse(e.data);
    console.log('Final article:', article);
    eventSource.close();
});

eventSource.onerror = () => {
    console.error('Stream error');
    eventSource.close();
};
```

#### Type-Safe Partial Updates

For more robust type safety, use `onPartialParsed` with a nullable mirror class:

```java
// Nullable mirror class for partial parsing
public record PartialArticle(
    @Nullable String title,
    @Nullable String summary,
    @Nullable List<String> keyPoints,
    @Nullable String author
) {}

@GetMapping(value = "/article/typed", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamTypedArticle(@RequestParam String topic) {
    SseEmitter emitter = new SseEmitter(120_000L);

    var payload = CreateResponsePayload.builder()
        .model(props.getModel())
        .addUserMessage("Write an article about: " + topic)
        .withStructuredOutput(Article.class)
        .streaming()
        .build();

    responder.respond(payload)
        .onPartialParsed(PartialArticle.class, partial -> {
            try {
                // Type-safe access to partial fields
                emitter.send(SseEmitter.event()
                    .name("partial")
                    .data(Map.of(
                        "title", partial.title() != null ? partial.title() : "",
                        "summary", partial.summary() != null ? partial.summary() : "",
                        "keyPoints", partial.keyPoints() != null ? partial.keyPoints() : List.of(),
                        "author", partial.author() != null ? partial.author() : ""
                    )));
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        })
        .onParsedComplete(response -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("complete")
                    .data(response.outputParsed()));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        })
        .onError(emitter::completeWithError)
        .start();

    return emitter;
}
```

### Streaming with Tool Calls

Stream AI responses that include function/tool calls in real-time:

```java
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolStore;
import com.paragon.responses.annotations.FunctionMetadata;

@RestController
@RequestMapping("/api/stream")
public class ToolStreamingController {

    private final Responder responder;
    private final FunctionToolStore toolStore;
    private final Agentle4jProperties props;

    public ToolStreamingController(
            Responder responder,
            FunctionToolStore toolStore,
            Agentle4jProperties props) {
        this.responder = responder;
        this.toolStore = toolStore;
        this.props = props;
    }

    @GetMapping(value = "/assistant", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamWithTools(@RequestParam String message) {
        SseEmitter emitter = new SseEmitter(120_000L);

        var payload = CreateResponsePayload.builder()
            .model(props.getModel())
            .addDeveloperMessage("You are a helpful assistant with access to tools.")
            .addUserMessage(message)
            .addTool(toolStore.get("get_weather"))
            .addTool(toolStore.get("search_products"))
            .addTool(toolStore.get("get_order_status"))
            .streaming()
            .build();

        responder.respond(payload)
            // Stream text as it's generated
            .onTextDelta(delta -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("text")
                        .data(delta));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            // Notify when a tool is called
            .onToolCall((toolName, argsJson) -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("tool_call")
                        .data(Map.of(
                            "tool", toolName,
                            "arguments", argsJson,
                            "status", "executing"
                        )));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            // Auto-execute tools and get results
            .withToolStore(toolStore)
            .onToolResult((toolName, result) -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("tool_result")
                        .data(Map.of(
                            "tool", toolName,
                            "output", result.output(),
                            "status", "completed"
                        )));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            .onComplete(response -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("complete")
                        .data(Map.of(
                            "finalText", response.outputText(),
                            "tokens", response.usage().totalTokens()
                        )));
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            .onError(emitter::completeWithError)
            .start();

        return emitter;
    }
}
```

#### Client-Side: Tool Call Progress UI

```javascript
const eventSource = new EventSource('/api/stream/assistant?message=What is the weather in Tokyo and find me running shoes');

eventSource.addEventListener('text', (e) => {
    document.getElementById('response').textContent += e.data;
});

eventSource.addEventListener('tool_call', (e) => {
    const data = JSON.parse(e.data);
    addToolStatus(`🔧 Calling ${data.tool}...`, 'pending');
});

eventSource.addEventListener('tool_result', (e) => {
    const data = JSON.parse(e.data);
    updateToolStatus(data.tool, `✅ ${data.tool}: ${data.output}`, 'success');
});

eventSource.addEventListener('complete', (e) => {
    const data = JSON.parse(e.data);
    console.log('Final response:', data.finalText);
    console.log('Tokens used:', data.tokens);
    eventSource.close();
});

eventSource.onerror = () => {
    console.error('Stream error');
    eventSource.close();
};
```

---

## Agent Streaming (Multi-Turn with Tools)

Stream full agent interactions with tool execution, handoffs, and guardrails:

```java
import com.paragon.agents.Agent;
import com.paragon.agents.AgentResult;

@RestController
@RequestMapping("/api/agent")
public class AgentStreamingController {

    private final Agent customerServiceAgent;

    public AgentStreamingController(Agent customerServiceAgent) {
        this.customerServiceAgent = customerServiceAgent;
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAgentChat(
            @RequestParam String message,
            @RequestParam(required = false) String sessionId) {
        
        SseEmitter emitter = new SseEmitter(180_000L); // 3 min for complex interactions

        customerServiceAgent.interactStream(message)
            // Track multi-turn progress
            .onTurnStart(turn -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("turn_start")
                        .data(Map.of("turn", turn)));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            .onTurnComplete(response -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("turn_complete")
                        .data(Map.of("tokens", response.usage().totalTokens())));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            // Stream text
            .onTextDelta(delta -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("text")
                        .data(delta));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            // Tool execution events
            .onToolCall((name, args) -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("tool_call")
                        .data(Map.of("tool", name, "args", args)));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            .onToolExecuted(exec -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("tool_executed")
                        .data(Map.of(
                            "tool", exec.toolName(),
                            "output", exec.output().toString(),
                            "durationMs", exec.duration().toMillis()
                        )));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            // Handoff events (multi-agent routing)
            .onHandoff(handoff -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("handoff")
                        .data(Map.of(
                            "to", handoff.targetAgent().name(),
                            "description", handoff.description()
                        )));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            // Guardrail events
            .onGuardrailFailed(failure -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("guardrail_blocked")
                        .data(Map.of(
                            "reason", failure.reason()
                        )));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            // Completion
            .onComplete(result -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("complete")
                        .data(Map.of(
                            "output", result.output(),
                            "turnsUsed", result.turnsUsed(),
                            "status", result.isSuccess() ? "success" : "error"
                        )));
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            .onError(error -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of("message", error.getMessage())));
                    emitter.completeWithError(error);
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            .start();

        return emitter;
    }
}
```

---

## Real-World Streaming Examples

### E-Commerce Order Assistant

A production-ready example combining structured output, tool calls, and streaming:

```java
@Service
public class OrderAssistantService {

    private final Agent orderAgent;
    private final ObjectMapper objectMapper;

    public OrderAssistantService(Responder responder, OrderTools orderTools) {
        this.objectMapper = new ObjectMapper();
        this.orderAgent = Agent.builder()
            .name("OrderAssistant")
            .model("openai/gpt-4o")
            .instructions("""
                You are an e-commerce order assistant. Help customers with:
                - Checking order status
                - Processing returns
                - Tracking shipments
                - Answering product questions
                
                Always verify the order ID before taking actions.
                Be concise and helpful.
                """)
            .responder(responder)
            .addTool(orderTools.getOrderStatus())
            .addTool(orderTools.trackShipment())
            .addTool(orderTools.initiateReturn())
            .addTool(orderTools.searchProducts())
            .build();
    }

    public void streamOrderAssistance(String userId, String message, SseEmitter emitter) {
        orderAgent.interactStream(message)
            .onTextDelta(delta -> sendSafe(emitter, "text", delta))
            .onToolCall((name, args) -> {
                sendSafe(emitter, "action", Map.of(
                    "type", "tool_started",
                    "tool", name,
                    "message", getToolMessage(name)
                ));
            })
            .onToolExecuted(exec -> {
                sendSafe(emitter, "action", Map.of(
                    "type", "tool_completed",
                    "tool", exec.toolName(),
                    "success", true
                ));
            })
            .onComplete(result -> {
                sendSafe(emitter, "complete", Map.of(
                    "success", result.isSuccess()
                ));
                emitter.complete();
            })
            .onError(e -> {
                sendSafe(emitter, "error", Map.of("message", e.getMessage()));
                emitter.completeWithError(e);
            })
            .start();
    }

    private String getToolMessage(String toolName) {
        return switch (toolName) {
            case "get_order_status" -> "Looking up your order...";
            case "track_shipment" -> "Checking shipment tracking...";
            case "initiate_return" -> "Processing your return request...";
            case "search_products" -> "Searching our catalog...";
            default -> "Processing...";
        };
    }

    private void sendSafe(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (Exception ignored) {}
    }
}
```

### Document Analysis with Progress

Stream analysis of documents with detailed progress updates:

```java
@RestController
@RequestMapping("/api/analyze")
public class DocumentAnalysisController {

    private final Responder responder;

    public record AnalysisResult(
        String summary,
        List<String> keyFindings,
        List<String> actionItems,
        String sentiment,
        double confidenceScore
    ) {}

    @PostMapping(value = "/document", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyzeDocument(@RequestBody DocumentRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 min for long documents

        var payload = CreateResponsePayload.builder()
            .model("openai/gpt-4o")
            .addDeveloperMessage("""
                Analyze the provided document and extract:
                - A concise summary
                - Key findings (up to 5)
                - Action items if any
                - Overall sentiment
                - Confidence score (0-1)
                """)
            .addUserMessage("Document to analyze:\n\n" + request.content())
            .withStructuredOutput(AnalysisResult.class)
            .streaming()
            .build();

        // Track which fields have been seen for progress
        Set<String> seenFields = ConcurrentHashMap.newKeySet();

        responder.respond(payload)
            .onPartialJson(fields -> {
                try {
                    // Send progress updates as new fields appear
                    for (String field : fields.keySet()) {
                        if (seenFields.add(field)) {
                            emitter.send(SseEmitter.event()
                                .name("progress")
                                .data(Map.of(
                                    "step", getProgressStep(field),
                                    "message", getProgressMessage(field)
                                )));
                        }
                    }
                    // Send partial data
                    emitter.send(SseEmitter.event()
                        .name("partial")
                        .data(fields));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            .onParsedComplete(response -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("complete")
                        .data(response.outputParsed()));
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            .onError(emitter::completeWithError)
            .start();

        return emitter;
    }

    private int getProgressStep(String field) {
        return switch (field) {
            case "summary" -> 1;
            case "keyFindings" -> 2;
            case "actionItems" -> 3;
            case "sentiment" -> 4;
            case "confidenceScore" -> 5;
            default -> 0;
        };
    }

    private String getProgressMessage(String field) {
        return switch (field) {
            case "summary" -> "Generating summary...";
            case "keyFindings" -> "Identifying key findings...";
            case "actionItems" -> "Extracting action items...";
            case "sentiment" -> "Analyzing sentiment...";
            case "confidenceScore" -> "Calculating confidence...";
            default -> "Processing...";
        };
    }

    public record DocumentRequest(String content) {}
}
```

### Real-Time Translation with Streaming

Stream translations as they're generated for long texts:

```java
@RestController
@RequestMapping("/api/translate")
public class TranslationStreamController {

    private final Responder responder;

    public record Translation(
        String translatedText,
        String detectedSourceLanguage,
        List<String> alternatives
    ) {}

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTranslation(
            @RequestBody TranslationRequest request) {
        
        SseEmitter emitter = new SseEmitter(120_000L);

        var payload = CreateResponsePayload.builder()
            .model("openai/gpt-4o")
            .addDeveloperMessage(String.format(
                "Translate the following text to %s. Detect the source language. " +
                "Provide 2-3 alternative translations if applicable.",
                request.targetLanguage()))
            .addUserMessage(request.text())
            .withStructuredOutput(Translation.class)
            .streaming()
            .build();

        StringBuilder translationBuffer = new StringBuilder();

        responder.respond(payload)
            .onPartialJson(fields -> {
                try {
                    // Stream the translation text progressively
                    if (fields.containsKey("translatedText")) {
                        String current = fields.get("translatedText").toString();
                        String newChunk = current.substring(translationBuffer.length());
                        translationBuffer.setLength(0);
                        translationBuffer.append(current);
                        
                        if (!newChunk.isEmpty()) {
                            emitter.send(SseEmitter.event()
                                .name("translation_chunk")
                                .data(newChunk));
                        }
                    }
                    
                    // Send detected language when available
                    if (fields.containsKey("detectedSourceLanguage")) {
                        emitter.send(SseEmitter.event()
                            .name("language_detected")
                            .data(fields.get("detectedSourceLanguage")));
                    }
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            .onParsedComplete(response -> {
                try {
                    Translation result = response.outputParsed();
                    emitter.send(SseEmitter.event()
                        .name("complete")
                        .data(result));
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            })
            .onError(emitter::completeWithError)
            .start();

        return emitter;
    }

    public record TranslationRequest(String text, String targetLanguage) {}
}
```

---

## WebSocket Chat

For bidirectional real-time chat:

### WebSocket Configuration

```java
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatHandler;

    public WebSocketConfig(ChatWebSocketHandler chatHandler) {
        this.chatHandler = chatHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatHandler, "/ws/chat")
            .setAllowedOrigins("*");
    }
}
```

### WebSocket Handler

```java
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final CustomerSupportAgent agent;

    public ChatWebSocketHandler(CustomerSupportAgent agent) {
        this.agent = agent;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String userInput = message.getPayload();
        
        agent.chatAsync(userInput)
            .thenAccept(result -> {
                try {
                    session.sendMessage(new TextMessage(result.output()));
                } catch (Exception e) {
                    // Log error
                }
            });
    }
}
```

---

## Error Handling

### Global Exception Handler

```java
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestControllerAdvice
public class AIExceptionHandler {

    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<ErrorResponse> handleAIError(CompletionException e) {
        Throwable cause = e.getCause();
        
        if (cause.getMessage().contains("429")) {
            return ResponseEntity.status(429)
                .body(new ErrorResponse("Rate limited. Please try again later."));
        }
        
        if (cause.getMessage().contains("401")) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("AI service configuration error."));
        }
        
        return ResponseEntity.status(500)
            .body(new ErrorResponse("AI service temporarily unavailable."));
    }

    public record ErrorResponse(String error) {}
}
```

---

## Production Best Practices

### 1. Environment Variables

```yaml
# Never hardcode API keys!
agentle4j:
  api-key: ${OPENROUTER_API_KEY:}
```

### 2. Health Check

```java
import org.springframework.boot.actuate.health.*;

@Component
public class AIHealthIndicator implements HealthIndicator {

    private final Responder responder;
    private final Agentle4jProperties props;

    public AIHealthIndicator(Responder responder, Agentle4jProperties props) {
        this.responder = responder;
        this.props = props;
    }

    @Override
    public Health health() {
        try {
            var payload = CreateResponsePayload.builder()
                .model(props.getModel())
                .addUserMessage("ping")
                .maxTokens(1)
                .build();
            
            responder.respond(payload).join();
            return Health.up().build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### 3. Rate Limiting

```java
import io.github.bucket4j.*;

@Service
public class RateLimitedChatService {

    private final CustomerSupportAgent agent;
    private final Bucket bucket;

    public RateLimitedChatService(CustomerSupportAgent agent) {
        this.agent = agent;
        this.bucket = Bucket.builder()
            .addLimit(Bandwidth.simple(100, Duration.ofMinutes(1)))
            .build();
    }

    public String chat(String message) {
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Too many requests");
        }
        return agent.chat(message);
    }
}
```

### 4. Caching Responses

```java
import org.springframework.cache.annotation.*;

@Service
@CacheConfig(cacheNames = "ai-responses")
public class CachedAnalysisService {

    private final Responder responder;

    // Cache identical analysis requests
    @Cacheable(key = "#input.hashCode()")
    public SentimentResult analyzeSentiment(String input) {
        // AI call...
    }
}
```

---

## Complete Example Application

Here's a minimal complete Spring Boot application:

```java
@SpringBootApplication
@EnableConfigurationProperties(Agentle4jProperties.class)
public class AIApplication {
    public static void main(String[] args) {
        SpringApplication.run(AIApplication.class, args);
    }
}
```

```yaml
# application.yml
server:
  port: 8080

agentle4j:
  api-key: ${OPENROUTER_API_KEY}
  model: openai/gpt-4o-mini
  temperature: 0.7
  max-retries: 3
```

```xml
<!-- pom.xml additions -->
<dependency>
    <groupId>io.github.paragon-intelligence</groupId>
    <artifactId>agentle4j</artifactId>
    <version>0.1.0</version>
</dependency>
```

---

## Use Cases

| Use Case | Approach |
|----------|----------|
| **Chatbot API** | Agent as @Service + REST Controller |
| **Real-time Chat** | WebSocket + streaming |
| **Data Extraction** | Structured output endpoint |
| **Document Analysis** | Async processing with queues |
| **Multi-tenant SaaS** | Per-request Agent with tenant context |
| **Batch Processing** | @Async + CompletableFuture |

---

## Next Steps

- [Agents Guide](agents.md) - Advanced agent patterns
- [Streaming Guide](streaming.md) - Streaming details
- [Observability Guide](observability.md) - Monitoring in production
