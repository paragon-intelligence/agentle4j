# Getting Started

This guide will help you make your first API call with Agentle4j in under 5 minutes.

## Prerequisites

- [Agentle4j installed](installation.md)
- An API key from OpenRouter, OpenAI, or another supported provider

## Your First Request

Let's create a simple chatbot that responds to a message:

```java
import com.paragon.responses.Responder;
import com.paragon.responses.payload.CreateResponsePayload;
import com.paragon.responses.Response;

public class HelloAgentle {
    public static void main(String[] args) {
        // 1. Create a Responder
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

        // 3. Get the response (blocking)
        Response response = responder.respond(payload).join();
        
        // 4. Print the output
        System.out.println(response.outputText());
        // Output: The capital of France is Paris.
    }
}
```

## Streaming Responses

For real-time streaming of responses:

```java
var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Write a poem about Java")
    .streaming()  // Enable streaming
    .build();

responder.respond(payload)
    .onTextDelta(delta -> {
        System.out.print(delta);  // Print as it arrives
        System.out.flush();
    })
    .onComplete(response -> {
        System.out.println("\n✅ Done!");
    })
    .onError(Throwable::printStackTrace)
    .start();
```

## Structured Outputs

Get type-safe JSON responses using Java records:

```java
// Define your output schema
public record Person(String name, int age, String occupation) {}

// Request structured output
var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o")
    .addUserMessage("Create a fictional software engineer")
    .withStructuredOutput(Person.class)
    .build();

ParsedResponse<Person> response = responder.respond(payload).join();
Person person = response.parsed();

System.out.println("Name: " + person.name());
System.out.println("Age: " + person.age());
System.out.println("Occupation: " + person.occupation());
```

## Using Different Providers

Agentle4j supports multiple AI providers:

=== "OpenRouter"

    ```java
    Responder responder = Responder.builder()
        .openRouter()
        .apiKey("your-openrouter-key")
        .build();
    ```

=== "OpenAI"

    ```java
    Responder responder = Responder.builder()
        .openai()
        .apiKey("your-openai-key")
        .build();
    ```

=== "Custom Endpoint"

    ```java
    Responder responder = Responder.builder()
        .custom("https://your-api.com/v1")
        .apiKey("your-key")
        .build();
    ```

## Next Steps

Now that you've made your first request, explore these guides:

- [Responder Guide](guides/responder.md) - Deep dive into the Responder API
- [Agents Guide](guides/agents.md) - Build AI agents with tools and memory
- [Streaming Guide](guides/streaming.md) - Real-time response streaming
- [Function Tools Guide](guides/tools.md) - Let the AI call your functions
- [Observability Guide](guides/observability.md) - Monitor and trace your AI calls
