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
