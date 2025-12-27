# Getting Started

This comprehensive guide will help you master Agentle4j step by step.

## Prerequisites

- **Java 21+** installed
- **Maven** or **Gradle** build tool
- An API key from [OpenRouter](https://openrouter.ai/keys), [OpenAI](https://platform.openai.com/api-keys), or another supported provider

---

## Your First Request

Let's create a simple chatbot that responds to a message:

```java
import com.paragon.responses.Responder;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.Response;

public class HelloAgentle {
    public static void main(String[] args) {
        // 1. Create a Responder (the core HTTP client)
        Responder responder = Responder.builder()
            .openRouter()
            .apiKey(System.getenv("OPENROUTER_API_KEY"))
            .build();

        // 2. Build the request payload
        var payload = CreateResponsePayload.builder()
            .model("openai/gpt-4o-mini")
            .addDeveloperMessage("You are a helpful assistant.")
            .addUserMessage("What is the capital of France?")
            .build();

        // 3. Get the response (blocking with .join())
        Response response = responder.respond(payload).join();
        
        // 4. Print the output
        System.out.println(response.outputText());
        // Output: The capital of France is Paris.
    }
}
```

### Understanding the Code

| Component | Description |
|-----------|-------------|
| `Responder` | The HTTP client that communicates with the AI API |
| `CreateResponsePayload` | A builder to construct your request |
| `.addDeveloperMessage()` | Sets the system prompt (AI's behavior) |
| `.addUserMessage()` | The user's input message |
| `.respond()` | Returns `CompletableFuture<Response>` |
| `.join()` | Blocks until the response is ready |

---

## Async vs Blocking

Agentle4j is **async by default**. Choose your style:

### Blocking (Simple)

```java
// Use .join() to block and wait for the result
Response response = responder.respond(payload).join();
System.out.println(response.outputText());
```

### Async with Callbacks

```java
// Non-blocking with callbacks
responder.respond(payload)
    .thenAccept(response -> {
        System.out.println("Got response: " + response.outputText());
    })
    .exceptionally(error -> {
        System.err.println("Error: " + error.getMessage());
        return null;
    });

// Continue doing other work while waiting...
System.out.println("Request sent, doing other work...");
```

### Async with CompletableFuture

```java
// Chain multiple operations
CompletableFuture<String> result = responder.respond(payload)
    .thenApply(Response::outputText)
    .thenApply(text -> text.toUpperCase());

// Use the result later
String upperCaseResponse = result.join();
```

---

## Streaming Responses

Stream responses in real-time for a better user experience:

```java
var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Write a short poem about Java programming")
    .streaming()  // Enable streaming mode
    .build();

responder.respond(payload)
    .onTextDelta(delta -> {
        // Called for each chunk of text as it arrives
        System.out.print(delta);
        System.out.flush();  // Important: flush for immediate display
    })
    .onComplete(response -> {
        System.out.println("\n\n✅ Generation complete!");
        System.out.println("Total tokens: " + response.usage().totalTokens());
    })
    .onError(error -> {
        System.err.println("Error: " + error.getMessage());
    })
    .start();
```

### Streaming Callbacks Reference

| Callback | When Called | Use Case |
|----------|-------------|----------|
| `.onTextDelta(String)` | Each text chunk | Display real-time typing |
| `.onComplete(Response)` | Stream finished | Show final stats, save |
| `.onError(Throwable)` | Error occurred | Handle failures |

---

## Structured Outputs

Get **type-safe JSON responses** using Java records. No manual parsing needed!

### Basic Example

```java
// 1. Define your output schema as a Java record
public record Person(
    String name,
    int age,
    String occupation,
    List<String> skills
) {}

// 2. Request structured output
var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Create a fictional senior software engineer")
    .withStructuredOutput(Person.class)
    .build();

// 3. Get the parsed response
ParsedResponse<Person> response = responder.respond(payload).join();
Person person = response.outputParsed();

// 4. Use the strongly-typed object
System.out.println("Name: " + person.name());
System.out.println("Age: " + person.age());
System.out.println("Occupation: " + person.occupation());
System.out.println("Skills: " + String.join(", ", person.skills()));
```

### Real-World Example: Data Extraction

```java
// Extract structured data from unstructured text
public record ContactInfo(
    String name,
    String email,
    String phone,
    String company
) {}

String rawText = """
    Hi, I'm John Smith from Acme Corporation.
    You can reach me at john.smith@acme.com
    or call me at (555) 123-4567.
    Looking forward to hearing from you!
    """;

var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Extract contact information from this text:\n\n" + rawText)
    .withStructuredOutput(ContactInfo.class)
    .build();

ContactInfo contact = responder.respond(payload).join().outputParsed();

System.out.println("Name: " + contact.name());      // John Smith
System.out.println("Email: " + contact.email());    // john.smith@acme.com
System.out.println("Phone: " + contact.phone());    // (555) 123-4567
System.out.println("Company: " + contact.company()); // Acme Corporation
```

### Complex Nested Structures

```java
// Nested records work perfectly
public record Address(String street, String city, String country) {}

public record Employee(
    String name,
    String department,
    Address address,
    List<String> projects
) {}

var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Create a fictional employee at a tech company in San Francisco")
    .withStructuredOutput(Employee.class)
    .build();

Employee emp = responder.respond(payload).join().outputParsed();
System.out.println(emp.name() + " works in " + emp.department());
System.out.println("Located at: " + emp.address().city() + ", " + emp.address().country());
```

---

## Multi-Turn Conversations

Build conversational experiences by maintaining message history:

```java
import java.util.ArrayList;
import java.util.List;

// Keep track of the conversation
List<ResponseInputItem> conversation = new ArrayList<>();

// Helper to add messages
void addUserMessage(String text) {
    conversation.add(new UserMessage(text));
}

void addAssistantMessage(String text) {
    conversation.add(new AssistantMessage(text));
}

// First turn
addUserMessage("My name is Alice and I'm a Java developer.");
Response response1 = responder.respond(
    CreateResponsePayload.builder()
        .model("openai/gpt-4o-mini")
        .inputItems(conversation)
        .build()
).join();
addAssistantMessage(response1.outputText());
System.out.println("Assistant: " + response1.outputText());

// Second turn - the AI remembers!
addUserMessage("What's my name and what do I do?");
Response response2 = responder.respond(
    CreateResponsePayload.builder()
        .model("openai/gpt-4o-mini")
        .inputItems(conversation)
        .build()
).join();
System.out.println("Assistant: " + response2.outputText());
// Output: Your name is Alice and you're a Java developer!
```

---

## Using Different Providers

Agentle4j supports multiple AI providers through a unified API:

=== "OpenRouter (Recommended)"

    Access 300+ models through a single API:
    
    ```java
    Responder responder = Responder.builder()
        .openRouter()
        .apiKey(System.getenv("OPENROUTER_API_KEY"))
        .build();
    
    // Use any model available on OpenRouter
    var payload = CreateResponsePayload.builder()
        .model("anthropic/claude-3.5-sonnet")  // Claude
        // .model("google/gemini-pro")         // Gemini
        // .model("meta-llama/llama-3-70b")    // Llama
        .addUserMessage("Hello!")
        .build();
    ```

=== "OpenAI Direct"

    Connect directly to OpenAI:
    
    ```java
    Responder responder = Responder.builder()
        .openAi()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .build();
    
    var payload = CreateResponsePayload.builder()
        .model("gpt-4o")
        .addUserMessage("Hello!")
        .build();
    ```

=== "Groq (Fast Inference)"

    Ultra-fast inference with Groq:
    
    ```java
    Responder responder = Responder.builder()
        .baseUrl(HttpUrl.parse("https://api.groq.com/openai/v1"))
        .apiKey(System.getenv("GROQ_API_KEY"))
        .build();
    
    var payload = CreateResponsePayload.builder()
        .model("llama-3.1-70b-versatile")
        .addUserMessage("Hello!")
        .build();
    ```

=== "Custom Endpoint"

    Any OpenAI-compatible API:
    
    ```java
    Responder responder = Responder.builder()
        .baseUrl(HttpUrl.parse("https://your-api.example.com/v1"))
        .apiKey("your-api-key")
        .build();
    ```

---

## Complete Example: AI Assistant

Here's a complete, runnable example of an AI assistant:

```java
import com.paragon.responses.Responder;
import com.paragon.responses.spec.CreateResponsePayload;
import java.util.Scanner;

public class ChatBot {
    public static void main(String[] args) {
        // Initialize the responder
        Responder responder = Responder.builder()
            .openRouter()
            .apiKey(System.getenv("OPENROUTER_API_KEY"))
            .build();
        
        Scanner scanner = new Scanner(System.in);
        System.out.println("🤖 AI Assistant Ready! (type 'quit' to exit)\n");
        
        while (true) {
            System.out.print("You: ");
            String input = scanner.nextLine();
            
            if (input.equalsIgnoreCase("quit")) {
                System.out.println("Goodbye!");
                break;
            }
            
            System.out.print("Assistant: ");
            
            // Stream the response
            var payload = CreateResponsePayload.builder()
                .model("openai/gpt-4o-mini")
                .addDeveloperMessage("You are a helpful, friendly assistant. Keep responses concise.")
                .addUserMessage(input)
                .streaming()
                .build();
            
            responder.respond(payload)
                .onTextDelta(delta -> {
                    System.out.print(delta);
                    System.out.flush();
                })
                .onComplete(r -> System.out.println("\n"))
                .onError(e -> System.err.println("\nError: " + e.getMessage()))
                .start();
            
            // Small delay to ensure streaming completes before next prompt
            try { Thread.sleep(100); } catch (InterruptedException e) {}
        }
        
        scanner.close();
    }
}
```

---

## Next Steps

Now that you understand the basics, dive deeper:

| Guide | What You'll Learn |
|-------|-------------------|
| [Responder Guide](guides/responder.md) | Advanced Responder configuration |
| [Agents Guide](guides/agents.md) | Build AI agents with tools and memory |
| [Streaming Guide](guides/streaming.md) | Real-time streaming patterns |
| [Function Tools Guide](guides/tools.md) | Let the AI call your Java functions |
| [Observability Guide](guides/observability.md) | Monitor and trace AI calls |

---

## Troubleshooting

### Common Issues

**"API key not found"**
```bash
# Make sure to export your API key
export OPENROUTER_API_KEY="your-key-here"
```

**"Connection timeout"**
```java
// Increase timeout in the builder
Responder responder = Responder.builder()
    .openRouter()
    .apiKey(apiKey)
    .timeout(Duration.ofSeconds(60))
    .build();
```

**"Rate limit exceeded"**
```java
// Implement retry with exponential backoff
// Or upgrade your API plan
```

### Getting Help

- 📖 [Full API Reference](api.md)
- 🐛 [Report Issues](https://github.com/paragon-intelligence/agentle4j/issues)
- 💬 [Discussions](https://github.com/paragon-intelligence/agentle4j/discussions)
