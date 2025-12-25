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

# 🤖 Agentle4j

**The Modern Java Library for OpenAI Responses API**

[![Java](https://img.shields.io/badge/Java-21+-orange?logo=openjdk)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://github.com/paragon-intelligence/agentle4j/blob/main/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.paragon-intelligence/agentle4j)](https://central.sonatype.com/artifact/io.github.paragon-intelligence/agentle4j)

*Type-safe, streaming-enabled, production-ready AI integration for Java*

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

## ⚡ Performance

Agentle4j is designed for extreme efficiency — **6x faster than AGNO**, the fastest Python alternative:

```
╔═══════════════════════════════════════════════════════════════════╗
║                 AGENTLE PERFORMANCE BENCHMARK                     ║
╚═══════════════════════════════════════════════════════════════════╝

Framework        │       Time (μs) │    Memory (KiB) │   Speed vs AGNO
──────────────────────────────────────────────────────────────────────
🚀 AGENTLE       │            0.50 │          0.3906 │            6.0×
AGNO             │            3.00 │          6.6000 │   1× (baseline)
PydanticAI       │          170.00 │         28.7120 │      57× slower
CrewAI           │          210.00 │         65.6520 │      70× slower
LangGraph        │         1587.00 │        161.4350 │     529× slower
──────────────────────────────────────────────────────────────────────
```

---

## 🚀 Quick Example

```java
import com.paragon.responses.Responder;
import com.paragon.responses.payload.CreateResponsePayload;

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
Response response = responder.respond(payload).join();
System.out.println(response.outputText());
```

---

## 📦 Installation

=== "Maven"

    ```xml
    <dependency>
        <groupId>io.github.paragon-intelligence</groupId>
        <artifactId>agentle4j</artifactId>
        <version>0.1.0</version>
    </dependency>
    ```

=== "Gradle"

    ```groovy
    implementation 'io.github.paragon-intelligence:agentle4j:0.1.0'
    ```

---

## 📚 Documentation

| Section | Description |
|---------|-------------|
| [Installation](installation.md) | Detailed setup instructions |
| [Getting Started](getting-started.md) | Quick start guide |
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
