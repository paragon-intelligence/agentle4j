# Installation

> This docs was updated at: 2026-03-20

















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
    <version>0.10.0</version>
</dependency>
```

## Gradle

=== "Groovy DSL"

    ```groovy
    implementation 'io.github.paragon-intelligence:agentle4j:0.10.0'
    ```

=== "Kotlin DSL"

    ```kotlin
    implementation("io.github.paragon-intelligence:agentle4j:0.10.0")

## Enable Preview Features

Agentle4j uses Java 25 preview features. Your application must compile and run with
`--enable-preview`.

### Maven

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <release>25</release>
        <compilerArgs>
            <arg>--enable-preview</arg>
        </compilerArgs>
    </configuration>
</plugin>

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>--enable-preview</argLine>
    </configuration>
</plugin>
```

### Gradle

```kotlin
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("--enable-preview")
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-preview")
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-preview")
}
```
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
