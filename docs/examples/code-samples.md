# Code Examples

> This docs was updated at: 2026-02-23

Practical, copy-paste ready examples for common Agentle4j use cases.

---

## Basic Examples

### Simple Chat Completion

```java
import com.paragon.responses.Responder;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.Response;

public class SimpleChatExample {
    public static void main(String[] args) {
        Responder responder = Responder.builder()
            .openRouter()
            .apiKey(System.getenv("OPENROUTER_API_KEY"))
            .build();

        var payload = CreateResponsePayload.builder()
            .model("openai/gpt-4o-mini")
            .addDeveloperMessage("You are a helpful assistant.")
            .addUserMessage("Explain Java streams in 3 sentences.")
            .build();

        Response response = responder.respond(payload);
        System.out.println(response.outputText());
    }
}
```

### With Temperature Control

```java
// Creative writing (high temperature)
var creativePayload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Write a creative story opening about a space explorer")
    .temperature(1.2)  // More creative/random
    .build();

// Factual response (low temperature)  
var factualPayload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("What is the formula for calculating compound interest?")
    .temperature(0.1)  // More deterministic
    .build();
```

### Limiting Response Length

```java
var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Explain quantum computing")
    .maxOutputTokens(150)  // Limit response to ~150 tokens
    .build();
```

---

## Streaming Examples

### Basic Streaming with Progress

```java
import java.util.concurrent.atomic.AtomicInteger;

var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Write a detailed explanation of microservices architecture")
    .streaming()
    .build();

AtomicInteger charCount = new AtomicInteger(0);
long startTime = System.currentTimeMillis();

responder.respond(payload)
    .onTextDelta(delta -> {
        System.out.print(delta);
        System.out.flush();
        charCount.addAndGet(delta.length());
    })
    .onComplete(response -> {
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("\n\n--- Stats ---");
        System.out.println("Characters: " + charCount.get());
        System.out.println("Time: " + elapsed + "ms");
        System.out.println("Speed: " + (charCount.get() * 1000 / elapsed) + " chars/sec");
    })
    .onError(e -> System.err.println("Error: " + e.getMessage()))
    .start();
```

### Streaming to a File

```java
import java.io.FileWriter;
import java.io.PrintWriter;

var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Write a comprehensive guide to REST API design")
    .streaming()
    .build();

try (PrintWriter writer = new PrintWriter(new FileWriter("output.md"))) {
    responder.respond(payload)
        .onTextDelta(delta -> {
            writer.print(delta);
            writer.flush();
            System.out.print(delta);  // Also show in console
            System.out.flush();
        })
        .onComplete(r -> {
            System.out.println("\n\n✅ Saved to output.md");
        })
        .start();
}
```

### Streaming with Abort Capability

```java
import java.util.concurrent.atomic.AtomicBoolean;

AtomicBoolean shouldStop = new AtomicBoolean(false);
StringBuilder buffer = new StringBuilder();

var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Write a very long story")
    .streaming()
    .build();

var stream = responder.respond(payload)
    .onTextDelta(delta -> {
        if (shouldStop.get()) {
            return;  // Stop processing
        }
        buffer.append(delta);
        System.out.print(delta);
        
        // Auto-stop after 500 characters
        if (buffer.length() > 500) {
            shouldStop.set(true);
            System.out.println("\n\n[Stopped after 500 chars]");
        }
    })
    .start();
```

---

## Structured Output Examples

### Simple Data Extraction

```java
public record ProductInfo(
    String name,
    double price,
    String currency,
    boolean inStock
) {}

String productDescription = """
    The Sony WH-1000XM5 wireless headphones are available for $399.99.
    These premium noise-cancelling headphones are currently in stock.
    """;

var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Extract product info: " + productDescription)
    .withStructuredOutput(ProductInfo.class)
    .build();

ProductInfo product = responder.respond(payload).outputParsed();
System.out.println("Product: " + product.name());
System.out.println("Price: " + product.currency() + product.price());
System.out.println("In Stock: " + product.inStock());
```

### Sentiment Analysis

```java
public record SentimentAnalysis(
    String sentiment,        // "positive", "negative", "neutral"
    double confidence,       // 0.0 to 1.0
    List<String> keywords,   // Key emotional words
    String summary           // Brief explanation
) {}

String review = """
    I absolutely love this product! It exceeded all my expectations.
    The build quality is fantastic and customer service was helpful.
    Only minor issue is the slightly high price, but worth it!
    """;

var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addDeveloperMessage("Analyze the sentiment of customer reviews.")
    .addUserMessage(review)
    .withStructuredOutput(SentimentAnalysis.class)
    .build();

SentimentAnalysis analysis = responder.respond(payload).outputParsed();
System.out.println("Sentiment: " + analysis.sentiment());
System.out.println("Confidence: " + (analysis.confidence() * 100) + "%");
System.out.println("Keywords: " + String.join(", ", analysis.keywords()));
```

### Code Review Analysis

```java
public record CodeIssue(
    String type,           // "bug", "style", "performance", "security"
    String severity,       // "low", "medium", "high", "critical"
    int lineNumber,
    String description,
    String suggestion
) {}

public record CodeReview(
    int overallScore,      // 1-10
    List<CodeIssue> issues,
    List<String> positives,
    String summary
) {}

String code = """
    public class UserService {
        private String password = "admin123";  // Hardcoded password
        
        public void deleteUser(int id) {
            String sql = "DELETE FROM users WHERE id = " + id;  // SQL injection
            // execute sql...
        }
    }
    """;

var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addDeveloperMessage("You are a senior code reviewer. Analyze code for issues.")
    .addUserMessage("Review this code:\n```java\n" + code + "\n```")
    .withStructuredOutput(CodeReview.class)
    .build();

CodeReview review = responder.respond(payload).outputParsed();
System.out.println("Score: " + review.overallScore() + "/10");
System.out.println("\nIssues found:");
for (CodeIssue issue : review.issues()) {
    System.out.println("  [" + issue.severity().toUpperCase() + "] " + issue.type());
    System.out.println("  Line " + issue.lineNumber() + ": " + issue.description());
    System.out.println("  Fix: " + issue.suggestion());
    System.out.println();
}
```

### JSON Schema Generation

```java
// Complex nested structures
public record Address(
    String street,
    String city,
    String state,
    String zipCode,
    String country
) {}

public record Company(
    String name,
    String industry,
    int employeeCount,
    Address headquarters,
    List<String> products
) {}

public record MarketAnalysis(
    Company company,
    List<Company> competitors,
    double marketShare,
    List<String> strengths,
    List<String> weaknesses,
    String recommendation
) {}

var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Provide a market analysis for Apple Inc.")
    .withStructuredOutput(MarketAnalysis.class)
    .build();

MarketAnalysis analysis = responder.respond(payload).outputParsed();
```

---

## Multi-Turn Conversation Examples

### Stateful Conversation Manager

```java
import java.util.ArrayList;
import java.util.List;

public class ConversationManager {
    private final Responder responder;
    private final String model;
    private final String systemPrompt;
    private final List<ResponseInputItem> history = new ArrayList<>();
    
    public ConversationManager(Responder responder, String model, String systemPrompt) {
        this.responder = responder;
        this.model = model;
        this.systemPrompt = systemPrompt;
    }
    
    public String chat(String userMessage) {
        // Add user message to history
        history.add(Message.user(userMessage));

        // Build payload with full history
        var payload = CreateResponsePayload.builder()
            .model(model)
            .addDeveloperMessage(systemPrompt)
            .input(history)
            .build();

        // Get response
        Response response = responder.respond(payload);
        String assistantMessage = response.outputText();

        // Add assistant response to history
        history.add(Message.assistant(assistantMessage));
        
        return assistantMessage;
    }
    
    public void clearHistory() {
        history.clear();
    }
    
    public int getTurnCount() {
        return history.size() / 2;
    }
}

// Usage
ConversationManager chat = new ConversationManager(
    responder, 
    "openai/gpt-4o-mini",
    "You are a helpful coding tutor."
);

System.out.println(chat.chat("What is a Java interface?"));
System.out.println(chat.chat("Can you show me an example?"));
System.out.println(chat.chat("How is it different from an abstract class?"));
```

### Context Window Management

```java
public class SmartConversation {
    private final Responder responder;
    private final List<ResponseInputItem> history = new ArrayList<>();
    private static final int MAX_HISTORY = 20;  // Keep last 20 messages
    
    public String chat(String userMessage) {
        history.add(Message.user(userMessage));

        // Trim history if too long (keep most recent)
        if (history.size() > MAX_HISTORY) {
            history.subList(0, history.size() - MAX_HISTORY).clear();
        }

        var payload = CreateResponsePayload.builder()
            .model("openai/gpt-4o-mini")
            .input(history)
            .build();

        Response response = responder.respond(payload);
        history.add(Message.assistant(response.outputText()));
        
        return response.outputText();
    }
}
```

---

## Batch Processing Examples

### Parallel API Calls with Virtual Threads

```java
import java.util.List;
import java.util.concurrent.*;

List<String> topics = List.of(
    "Machine Learning",
    "Blockchain",
    "Quantum Computing",
    "Edge Computing",
    "5G Networks"
);

// Run requests in parallel with virtual threads
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    var futures = topics.stream()
        .map(topic -> executor.submit(() -> {
            var payload = CreateResponsePayload.builder()
                .model("openai/gpt-4o-mini")
                .addUserMessage("Explain " + topic + " in 2 sentences.")
                .build();
            return responder.respond(payload).outputText();
        }))
        .toList();
    
    // Process results
    for (int i = 0; i < topics.size(); i++) {
        System.out.println("## " + topics.get(i));
        System.out.println(futures.get(i).get());
        System.out.println();
    }
}
```

### Rate-Limited Batch Processing

```java
import java.util.concurrent.*;

// Limit to 5 concurrent requests
Semaphore rateLimiter = new Semaphore(5);
List<String> prompts = List.of(/* many prompts */);

try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    var results = prompts.stream()
        .map(prompt -> executor.submit(() -> {
            try {
                rateLimiter.acquire();
                var payload = CreateResponsePayload.builder()
                    .model("openai/gpt-4o-mini")
                    .addUserMessage(prompt)
                    .build();
                return responder.respond(payload).outputText();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                rateLimiter.release();
            }
        }))
        .toList();
    
    // Process results...
}
```

---

## Error Handling Examples

### Comprehensive Error Handling

```java
try {
    Response response = responder.respond(payload);
    System.out.println(response.outputText());
} catch (java.net.SocketTimeoutException e) {
    System.err.println("Request timed out. Please try again.");
} catch (java.net.UnknownHostException e) {
    System.err.println("Network error. Check your internet connection.");
} catch (RateLimitException e) {
    System.err.println("Rate limited. Please wait and try again.");
} catch (AuthenticationException e) {
    System.err.println("Invalid API key. Please check your credentials.");
} catch (ServerException e) {
    System.err.println("Server error. The API is temporarily unavailable.");
} catch (RuntimeException e) {
    System.err.println("Unexpected error: " + e.getMessage());
}
```

### Retry with Exponential Backoff

```java
import java.util.concurrent.TimeUnit;

public Response callWithRetry(Responder responder, CreateResponsePayload payload, int maxRetries) {
    int retries = 0;
    long delay = 1000;  // Start with 1 second
    
    while (retries < maxRetries) {
        try {
            return responder.respond(payload);
        } catch (RuntimeException e) {
            retries++;
            if (retries >= maxRetries) {
                throw e;
            }
            
            System.out.println("Retry " + retries + " after " + delay + "ms");
            try {
                TimeUnit.MILLISECONDS.sleep(delay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ie);
            }
            
            delay *= 2;  // Exponential backoff
        }
    }
    
    throw new RuntimeException("Max retries exceeded");
}
```

---

## Real-World Application Examples

### AI-Powered REST API Endpoint (Spring Boot)

```java
@RestController
@RequestMapping("/api/ai")
public class AIController {
    
    private final Responder responder;
    
    public AIController() {
        this.responder = Responder.builder()
            .openRouter()
            .apiKey(System.getenv("OPENROUTER_API_KEY"))
            .build();
    }
    
    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody ChatRequest request) {
        var payload = CreateResponsePayload.builder()
            .model("openai/gpt-4o-mini")
            .addUserMessage(request.message())
            .build();
        
        Response response = responder.respond(payload);
        return Map.of(
            "response", response.outputText()
        );
    }
    
    @PostMapping("/analyze")
    public SentimentAnalysis analyzeSentiment(@RequestBody TextRequest request) {
        var payload = CreateResponsePayload.builder()
            .model("openai/gpt-4o")
            .addUserMessage("Analyze sentiment: " + request.text())
            .withStructuredOutput(SentimentAnalysis.class)
            .build();
        
        return responder.respond(payload).outputParsed();
    }
}

record ChatRequest(String message) {}
record TextRequest(String text) {}
```

### CLI Tool with Streaming

```java
public class AICli {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: ai-cli <prompt>");
            return;
        }
        
        String prompt = String.join(" ", args);
        
        Responder responder = Responder.builder()
            .openRouter()
            .apiKey(System.getenv("OPENROUTER_API_KEY"))
            .build();
        
        var payload = CreateResponsePayload.builder()
            .model("openai/gpt-4o-mini")
            .addUserMessage(prompt)
            .streaming()
            .build();
        
        responder.respond(payload)
            .onTextDelta(delta -> {
                System.out.print(delta);
                System.out.flush();
            })
            .onComplete(r -> System.out.println())
            .onError(e -> {
                System.err.println("\nError: " + e.getMessage());
                System.exit(1);
            })
            .start();
    }
}
```

### Document Summarizer

```java
public class DocumentSummarizer {
    private final Responder responder;
    
    public DocumentSummarizer(Responder responder) {
        this.responder = responder;
    }
    
    public record Summary(
        String title,
        String oneSentence,
        List<String> keyPoints,
        List<String> actionItems,
        String targetAudience
    ) {}
    
    public Summary summarize(String document) {
        var payload = CreateResponsePayload.builder()
            .model("openai/gpt-4o")
            .addDeveloperMessage("""
                You are a document analysis expert. Summarize documents
                by extracting the most important information.
                """)
            .addUserMessage("Summarize this document:\n\n" + document)
            .withStructuredOutput(Summary.class)
            .build();
        
        return responder.respond(payload).outputParsed();
    }
    
    public static void main(String[] args) throws Exception {
        Responder responder = Responder.builder()
            .openRouter()
            .apiKey(System.getenv("OPENROUTER_API_KEY"))
            .build();
        
        DocumentSummarizer summarizer = new DocumentSummarizer(responder);
        
        String document = Files.readString(Path.of("document.txt"));
        Summary summary = summarizer.summarize(document);
        
        System.out.println("# " + summary.title());
        System.out.println("\n" + summary.oneSentence());
        System.out.println("\n## Key Points");
        summary.keyPoints().forEach(p -> System.out.println("- " + p));
        System.out.println("\n## Action Items");
        summary.actionItems().forEach(a -> System.out.println("- [ ] " + a));
    }
}
```
