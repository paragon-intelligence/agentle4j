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

### Agent Instantiation Benchmark

Measures **time to create an agent instance** (not API calls). Lower is better.

```
╔═══════════════════════════════════════════════════════════════════╗
║           AGENT INSTANTIATION BENCHMARK (lower = better)          ║
╚═══════════════════════════════════════════════════════════════════╝

Framework        │  Time (μs) │  Memory (KiB) │ Language
─────────────────┼────────────┼───────────────┼─────────────────────
🚀 Agentle4j     │       0.8.3 │         0.39  │ Java 25+ (this lib)
AGNO             │       3.00 │         6.60  │ Python
PydanticAI       │     170.00 │        28.71  │ Python
CrewAI           │     210.00 │        65.65  │ Python
LangGraph        │   1,587.00 │       161.43  │ Python
──────────────────────────────────────────────────────────────────────
```

!!! warning "Important Caveats"
    - **Cross-language comparisons are inherently unfair.** Java's JVM provides different performance characteristics than Python's interpreter.
    - This benchmark only measures **agent instantiation time**, not actual LLM inference or end-to-end latency.
    - We haven't benchmarked against **LangChain4J** or **Spring AI** yet—contributions welcome!
    - Real-world performance depends heavily on network latency, model choice, and payload size.

### Java Alternatives

| Library | Focus | Notes |
|---------|-------|-------|
| **Agentle4j** | Agents-first, OpenAI Responses API | This library |
| **LangChain4J** | General-purpose, many integrations | Mature ecosystem |
| **Spring AI** | Spring ecosystem integration | Production-ready |

---

## 🚀 Quick Example

```java
import com.paragon.responses.Responder;
import com.paragon.responses.spec.CreateResponsePayload;

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
        <version>0.8.3</version>
    </dependency>
    ```

=== "Gradle"

    ```groovy
    implementation 'io.github.paragon-intelligence:agentle4j:0.8.3'
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
