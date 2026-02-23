# Tool Search Guide

When building agents with many tools, sending all of them in every API call wastes context window tokens and can degrade tool selection accuracy. **Tool Search** solves this by searching the available tools based on the user's input and only sending the most relevant subset to the LLM.

This is the framework-level equivalent of Anthropic's server-side tool search feature, but works client-side with **any LLM provider** (OpenAI, OpenRouter, local models).

---

## Eager vs Deferred Tools

Tool Search introduces two categories of tools:

1. **Eager Tools**: Critical tools that are *always* included in every API call.
2. **Deferred Tools**: Tools that are only included when the search strategy determines they are relevant to the user's input.

---

## Basic Usage

You configure tool search using a `ToolRegistry` and attach it to your `Agent`:

```java
// 1. Choose a search strategy
ToolSearchStrategy strategy = new BM25ToolSearchStrategy(5); // Return top 5 matches

// 2. Create the registry
ToolRegistry registry = ToolRegistry.builder()
    .strategy(strategy)
    .eagerTool(new GetTimeTool())                  // Always available
    .eagerTool(new HelpTool())                     // Always available
    .deferredTools(thousandsOfDatabaseTools)       // Only included when relevant
    .build();

// 3. Attach to agent
Agent agent = Agent.builder()
    .name("DatabaseAssistant")
    .model("openai/gpt-4o")
    .instructions("You are a helpful assistant that queries databases.")
    .responder(responder)
    .toolRegistry(registry)
    .build();
```

When `agent.interact("Get user 123")` is called, the framework parses the user's message, searches the `deferredTools`, and sends a curated list of tools (Eager + relevant Deferred tools) to the LLM.

---

## Built-in Search Strategies

The framework provides three built-in search strategies, each optimized for different use cases.

### 1. RegexToolSearchStrategy

A lightweight strategy that uses regex patterns to match the query words against tool names and descriptions.

```java
// Returns up to 5 tools where ANY word in the query matches the tool name or description
ToolSearchStrategy strategy = new RegexToolSearchStrategy(5);
```

**Best for**: Simple agents, exact keyword matching, and extremely low latency.

### 2. BM25ToolSearchStrategy

A robust information retrieval algorithm (Best Matching 25) that computes relevance scores based on term frequency and inverse document frequency. It's much smarter than regex because it handles term saturation and document length normalization.

```java
// Returns top 5 tools ranked by BM25 score
ToolSearchStrategy strategy = new BM25ToolSearchStrategy(5);

// Or with custom tuning parameters (maxResults, k1, b)
ToolSearchStrategy advancedStrategy = new BM25ToolSearchStrategy(10, 1.5, 0.75);
```

**Best for**: Most standard use cases. It runs entirely locally, requires no external API calls, and provides excellent keyword-based relevance ranking.

### 3. EmbeddingToolSearchStrategy

Uses embedding vectors for semantic similarity matching. This allows for matching concepts rather than exact words (e.g., matching "temperature outside" to a tool named "get_weather").

```java
EmbeddingProvider provider = new OpenRouterEmbeddingProvider(apiKey);

// Returns top 5 tools based on cosine similarity
ToolSearchStrategy strategy = new EmbeddingToolSearchStrategy(
    provider, 
    "text-embedding-3-small", 
    5
);
```

**Best for**: Massive toolsets where users use unpredictable terminology that keywords might miss.
*Note: This strategy makes an API call to the embedding provider on each search, so it has higher latency than the others. Tool embeddings are computed once and cached.*

---

## Combining with Tool Planning

**Yes, Tool Search and Tool Planning work seamlessly together!**

While they solve different problems, combining them creates highly efficient agents:

- **Tool Search** solves the *prompt size* problem by filtering which tools are available to the LLM.
- **Tool Planning** solves the *round-trip latency* problem by allowing the LLM to batch calls to those available tools.

When both are enabled, the `execute_tool_plan` meta-tool is automatically injected as an **eager** tool. 

If a user asks a complex question, the search strategy first finds the relevant deferred tools. The LLM then receives the `execute_tool_plan` tool alongside the relevant domain tools, and can choose to output a batched plan utilizing them.

```java
Agent agent = Agent.builder()
    .name("SuperAgent")
    .model("openai/gpt-4o")
    .instructions("You can search everything and plan your actions.")
    .responder(responder)
    .toolRegistry(registry)         // Filters the tools sent to the LLM
    .enableToolPlanning()           // Adds execute_tool_plan as an eager tool
    .build();
```

With this setup, you get the best of both worlds: minimal context window usage *and* minimal API round-trips.
