---
hide:
  - navigation
  - toc
---

<style>
.md-typeset h1 {
  display: none;
}
</style>

<div align="center" markdown>

![Agentle4j Logo](assets/logo.png){ width="200" }

# Agentle4j

> This docs was updated at: 2026-03-20

















**Java 25+, virtual-thread-friendly, Responses API-native**

[![Java](https://img.shields.io/badge/Java-25+-orange?logo=openjdk)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://github.com/paragon-intelligence/agentle4j/blob/main/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.paragon-intelligence/agentle4j)](https://central.sonatype.com/artifact/io.github.paragon-intelligence/agentle4j)

*Type-safe agents, streaming, and structured output for Java*

[Get Started](getting-started.md){ .md-button .md-button--primary }
[API Reference](api.md){ .md-button }

</div>

---

## ✨ Key Features

<div class="grid cards" markdown>

-   :robot:{ .lg .middle } **Agent Framework**

    ---

    Complete agent system with tools, guardrails, memory, and handoffs

-   :dart:{ .lg .middle } **Type-safe API**

    ---

    Full OpenAI Responses API with Java records

-   :ocean:{ .lg .middle } **Real-time Streaming**

    ---

    Virtual thread-based streaming with callbacks

-   :package:{ .lg .middle } **Structured Outputs**

    ---

    Strongly-typed JSON responses

-   :wrench:{ .lg .middle } **Function Calling**

    ---

    Auto-generated JSON schemas from Java classes

-   :telescope:{ .lg .middle } **OpenTelemetry**

    ---

    Built-in observability with spans/metrics

</div>

---

## ⚙️ Runtime Model

- Agentle4j requires **Java 25+ with preview features enabled**.
- Core APIs are **synchronous-first** and designed to be used directly or dispatched onto virtual threads.
- Streaming is exposed through **`ResponseStream`** and **`AgentStream`** callbacks.
- The published artifact version documented here is **`0.10.0`**.

---

## 🚀 Quick Example

```java
import com.paragon.responses.Responder;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.spec.Response;

// Create a responder
Responder responder = Responder.builder()
    .openRouter()
    .apiKey("your-api-key")
    .build();

// Build your request
var payload = CreateResponsePayload.builder()
    .model("openai/gpt-4o-mini")
    .addDeveloperMessage("You are a helpful assistant.")
    .addUserMessage("Hello!")
    .build();

// Get the response
Response response = responder.respond(payload);
System.out.println(response.outputText());
```

---

## 📦 Installation

=== "Maven"

    ```xml
    <dependency>
        <groupId>io.github.paragon-intelligence</groupId>
        <artifactId>agentle4j</artifactId>
        <version>0.10.0</version>
    </dependency>
    ```

=== "Gradle"

    ```groovy
    implementation 'io.github.paragon-intelligence:agentle4j:0.10.0'
    ```

Requires Java 25+ with preview features enabled.

---

## 📚 Documentation

| Section | Description |
|---------|-------------|
| [Installation](installation.md) | Detailed setup instructions |
| [Getting Started](getting-started.md) | Quick start guide |
| [Agentic Patterns](guides/agentic-patterns.md) | Visual control-flow guide for agents, delegation, and orchestration |
| [Guides](guides/responder.md) | In-depth feature guides |
| [API Reference](api.md) | Auto-generated Javadoc |
| [Examples](examples/code-samples.md) | Code samples |

---

## 🤝 Contributing

We welcome contributions! Please see our [GitHub repository](https://github.com/paragon-intelligence/agentle4j) for more information.

---

<div align="center" markdown>

Made with ❤️ by [Paragon Intelligence](https://github.com/paragon-intelligence)

</div>
