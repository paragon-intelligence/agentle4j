# Installation

> This docs was updated at: 2026-02-23

This guide covers how to add Agentle4j to your Java project.

## Requirements

- **Java 25+** (preview features enabled)
- **Maven 3.8+** or **Gradle 8+**

## Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.paragon-intelligence</groupId>
    <artifactId>agentle4j</artifactId>
    <version>0.7.0</version>
</dependency>
```

## Gradle

=== "Groovy DSL"

    ```groovy
    implementation 'io.github.paragon-intelligence:agentle4j:0.7.0'
    ```

=== "Kotlin DSL"

    ```kotlin
    implementation("io.github.paragon-intelligence:agentle4j:0.7.0")
    ```

## Verifying Installation

After adding the dependency, verify the installation by creating a simple test:

```java
import com.paragon.responses.Responder;

public class VerifyInstall {
    public static void main(String[] args) {
        Responder responder = Responder.builder()
            .openRouter()
            .apiKey("test-key")
            .build();
        
        System.out.println("✅ Agentle4j installed successfully!");
    }
}
```

## API Keys

Agentle4j supports multiple AI providers. You'll need an API key from one of:

| Provider | Get API Key |
|----------|-------------|
| OpenRouter | [openrouter.ai/keys](https://openrouter.ai/keys) |
| OpenAI | [platform.openai.com/api-keys](https://platform.openai.com/api-keys) |
| Groq | [console.groq.com/keys](https://console.groq.com/keys) |

!!! tip "Recommendation"
    We recommend **OpenRouter** for development as it provides access to multiple models through a single API key, including OpenAI, Anthropic, Google, and open-source models.

## Environment Variables

For security, store your API keys in environment variables:

```bash
export OPENROUTER_API_KEY="your-key-here"
```

Then load them in your code:

```java
String apiKey = System.getenv("OPENROUTER_API_KEY");

Responder responder = Responder.builder()
    .openRouter()
    .apiKey(apiKey)
    .build();
```

## Next Steps

Now that you have Agentle4j installed, head to the [Getting Started](getting-started.md) guide to learn the basics!
