# :material-code-braces: Main

> This docs was updated at: 2026-02-23

`com.paragon.Main` &nbsp;·&nbsp; **Class**

---

Agentle Usage Examples

This class demonstrates the core capabilities of the Agentle library. Use the CLI menu to
select and run different examples.

Examples included:

  
- 1-10: Responder API Examples (text generation, structured output, streaming, etc.)
- 11-16: Agent API Examples (basic agent, guardrails, handoffs, parallel agents, etc.)

## Methods

### `simpleTextGeneration`

```java
private static void simpleTextGeneration(String apiKey)
```

Example 1: Basic text generation with the Responses API.

---

### `structuredOutputGeneration`

```java
private static void structuredOutputGeneration(String apiKey)
```

Example 2: Generate structured JSON output matching a specific schema.

---

### `functionCallingExample`

```java
private static void functionCallingExample(String apiKey) throws JsonProcessingException
```

Example 3: Function calling allows the model to invoke custom tools.

---

### `temperatureControlExample`

```java
private static void temperatureControlExample(String apiKey)
```

Example 4: Control randomness using temperature and topP.

---

### `multiTurnConversationExample`

```java
private static void multiTurnConversationExample(String apiKey)
```

Example 5: Multi-turn conversation with context.

---

### `visionExample`

```java
private static void visionExample(String apiKey)
```

Example 6: Vision - send images to vision-capable models.

---

### `toolChoiceExample`

```java
private static void toolChoiceExample(String apiKey)
```

Example 7: Tool choice control - AUTO, REQUIRED, or NONE.

---

### `maxTokensExample`

```java
private static void maxTokensExample(String apiKey)
```

Example 8: Control response length with max tokens.

---

### `streamingExample`

```java
private static void streamingExample(String apiKey)
```

Example 9: Stream responses in real-time using virtual threads.

---

### `structuredStreamingExample`

```java
private static void structuredStreamingExample(String apiKey)
```

Example 10: Stream structured output and parse to typed object.

---

### `basicAgentExample`

```java
private static void basicAgentExample(String apiKey)
```

Example 11: Basic Agent Interaction. Demonstrates creating an agent and interacting with it
using the async API. Shows how to maintain conversation history by reusing AgentContext.

---

### `agentWithGuardrailsExample`

```java
private static void agentWithGuardrailsExample(String apiKey)
```

Example 12: Agent with Guardrails. Demonstrates input/output validation using guardrails.

---

### `agentWithHandoffsExample`

```java
private static void agentWithHandoffsExample(String apiKey)
```

Example 13: Agent with Handoffs. Demonstrates agent-to-agent transfer when conditions are met.

---

### `parallelAgentsExample`

```java
private static void parallelAgentsExample(String apiKey)
```

Example 14: Parallel Agents (Fan-out/Fan-in). Demonstrates running multiple agents concurrently
and synthesizing results.

---

### `routerAgentExample`

```java
private static void routerAgentExample(String apiKey)
```

Example 15: Router Agent (Classification). Demonstrates intelligent routing of inputs to
appropriate agents.

---

### `agentWithMemoryExample`

```java
private static void agentWithMemoryExample(String apiKey)
```

Example 16: Agent with Memory. Demonstrates using InMemoryMemory for conversation context.
