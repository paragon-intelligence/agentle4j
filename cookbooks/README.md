# Agentle Cookbooks

This directory contains standalone Java examples demonstrating the core capabilities of the **Agentle** library.
Each file is self-contained and can be run independently.

## Prerequisites

Create a `.env` file at the project root with:

```
OPENROUTER_API_KEY=your_api_key_here
```

---

## Responder API Examples

| File | Description |
|------|-------------|
| `SimpleTextGeneration.java` | Basic text generation with optional Langfuse telemetry |
| `StructuredOutputGeneration.java` | JSON output mapped to a Java record via `withStructuredOutput` |
| `FunctionCalling.java` | Expose custom tools and execute model-requested calls |
| `TemperatureControl.java` | Tune `temperature` and `topP` for creativity vs. determinism |
| `MultiTurnConversation.java` | Manual multi-turn history by threading assistant replies |
| `Vision.java` | Send image URLs to vision-capable models |
| `ToolChoiceControl.java` | Force (`REQUIRED`) or suppress (`NONE`) tool calls |
| `MaxTokens.java` | Cap response length with `maxOutputTokens` |
| `StreamingResponse.java` | Real-time token streaming with `onTextDelta` / `onComplete` |
| `StructuredStreamingOutput.java` | Stream JSON and parse into a typed record via `onParsedComplete` |

## Agent API Examples

| File | Description |
|------|-------------|
| `BasicAgentInteraction.java` | Create an agent and maintain multi-turn context with `AgenticContext` |
| `AgentWithGuardrails.java` | Input & output guardrails to validate or block requests/responses |
| `AgentWithHandoffs.java` | Agent-to-agent transfer when a topic is detected |
| `ParallelAgents.java` | Fan-out to multiple agents in parallel, then fan-in with a synthesizer |
| `RouterAgent.java` | LLM-based classification routing to the right specialist agent |
| `AgentWithMemory.java` | Store, retrieve, and clear user-scoped memories with `InMemoryMemory` |
| `AgentWithToolPlanning.java` | Batch tool calls into a declarative plan with parallel execution and `$ref` dependencies |
