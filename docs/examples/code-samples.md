# Code Samples

Practical examples for common use cases with Agentle4j.

## Basic Chat

```java
Responder responder = Responder.builder()
    .openRouter()
    .apiKey(System.getenv("OPENROUTER_API_KEY"))
    .build();

var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o-mini")
    .addDeveloperMessage("You are a helpful assistant.")
    .addUserMessage("Hello! How are you?")
    .build();

Response response = responder.respond(payload).join();
System.out.println(response.outputText());
```

## Multi-Turn Conversation

```java
List<Message> conversation = new ArrayList<>();

// First turn
conversation.add(Message.user("My name is Alice"));
var payload1 = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .messages(conversation)
    .build();

Response response1 = responder.respond(payload1).join();
conversation.add(Message.assistant(response1.outputText()));

// Second turn
conversation.add(Message.user("What's my name?"));
var payload2 = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .messages(conversation)
    .build();

Response response2 = responder.respond(payload2).join();
System.out.println(response2.outputText());  // "Your name is Alice"
```

## Streaming with Progress

```java
var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Explain quantum computing in detail")
    .streaming()
    .build();

AtomicInteger charCount = new AtomicInteger(0);

responder.respond(payload)
    .onTextDelta(delta -> {
        System.out.print(delta);
        charCount.addAndGet(delta.length());
    })
    .onComplete(response -> {
        System.out.println("\n\n📊 Total characters: " + charCount.get());
        System.out.println("📝 Tokens used: " + response.usage().totalTokens());
    })
    .start();
```

## Structured Output: Extract Data

```java
public record ContactInfo(
    String name,
    String email,
    String phone,
    String company
) {}

var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("""
        Extract contact info from this text:
        
        Hi, I'm John Smith from Acme Corp. 
        You can reach me at john.smith@acme.com or call 555-1234.
        """)
    .withStructuredOutput(ContactInfo.class)
    .build();

ParsedResponse<ContactInfo> response = responder.respond(payload).join();
ContactInfo contact = response.parsed();

System.out.println("Name: " + contact.name());
System.out.println("Email: " + contact.email());
System.out.println("Phone: " + contact.phone());
System.out.println("Company: " + contact.company());
```

## Function Tool: Calculator

```java
public record CalcParams(double a, double b, String operation) {}

@FunctionMetadata(
    name = "calculate",
    description = "Performs basic math operations: add, subtract, multiply, divide"
)
public class CalculatorTool extends FunctionTool<CalcParams> {
    @Override
    public FunctionToolCallOutput call(@Nullable CalcParams params) {
        if (params == null) return FunctionToolCallOutput.error("Parameters required");
        
        double result = switch (params.operation()) {
            case "add" -> params.a() + params.b();
            case "subtract" -> params.a() - params.b();
            case "multiply" -> params.a() * params.b();
            case "divide" -> params.a() / params.b();
            default -> throw new IllegalArgumentException("Unknown operation");
        };
        
        return FunctionToolCallOutput.success(String.valueOf(result));
    }
}

// Usage
Agent mathBot = Agent.builder()
    .name("MathBot")
    .model("openai/gpt-4o")
    .instructions("You help with math calculations.")
    .responder(responder)
    .addTool(new CalculatorTool())
    .build();

AgentResult result = mathBot.interact("What is 15 * 7?").join();
System.out.println(result.output());  // "15 * 7 = 105"
```

## Agent with Memory

```java
Memory memory = InMemoryMemory.create();

Agent assistant = Agent.builder()
    .name("PersonalAssistant")
    .model("openai/gpt-4o")
    .instructions("You remember user preferences and provide personalized help.")
    .responder(responder)
    .addMemoryTools(memory)
    .build();

AgentContext context = AgentContext.create();
context.setState("userId", "user-123");

// Store preference
assistant.interact("Remember that my favorite color is blue", context).join();

// Later session
assistant.interact("What's my favorite color?", context).join();
// → "Your favorite color is blue!"
```

## Multi-Agent: Customer Support

```java
Agent billingAgent = Agent.builder()
    .name("BillingSpecialist")
    .model("openai/gpt-4o")
    .instructions("You handle billing and payment questions.")
    .responder(responder)
    .build();

Agent techAgent = Agent.builder()
    .name("TechSupport")
    .model("openai/gpt-4o")
    .instructions("You handle technical issues and troubleshooting.")
    .responder(responder)
    .build();

RouterAgent router = RouterAgent.builder()
    .model("openai/gpt-4o-mini")
    .responder(responder)
    .addRoute(billingAgent, "billing, payments, invoices, subscriptions")
    .addRoute(techAgent, "bugs, errors, crashes, technical problems")
    .fallback(techAgent)
    .build();

// Automatically routes to appropriate agent
AgentResult result = router.route("I can't log into my account").join();
System.out.println("Handled by: " + result.handoffTarget().name());
```

## Image Analysis

```java
Image image = new Image(ImageDetail.AUTO, null, "https://example.com/chart.png");

UserMessage message = Message.builder()
    .addText("Analyze this chart and summarize the key trends")
    .addContent(image)
    .asUser();

var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addMessage(message)
    .build();

Response response = responder.respond(payload).join();
System.out.println(response.outputText());
```

## Error Handling

```java
responder.respond(payload)
    .handle((response, error) -> {
        if (error != null) {
            if (error.getCause() instanceof RateLimitException) {
                System.err.println("Rate limited! Waiting before retry...");
                // Implement retry logic
            } else if (error.getCause() instanceof AuthenticationException) {
                System.err.println("Invalid API key!");
            } else {
                System.err.println("Unexpected error: " + error.getMessage());
            }
            return null;
        }
        return response;
    });
```

## Batch Processing

```java
List<String> prompts = List.of(
    "Summarize: AI in healthcare",
    "Summarize: AI in finance", 
    "Summarize: AI in education"
);

List<CompletableFuture<Response>> futures = prompts.stream()
    .map(prompt -> {
        var payload = CreateResponsePayload.builder()
            .model("openai/gpt-4o-mini")
            .addUserMessage(prompt)
            .build();
        return responder.respond(payload);
    })
    .toList();

// Wait for all to complete
CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

// Process results
for (int i = 0; i < futures.size(); i++) {
    Response response = futures.get(i).join();
    System.out.println("=== " + prompts.get(i) + " ===");
    System.out.println(response.outputText());
}
```
