# :material-code-braces: Agent

> This docs was updated at: 2026-02-23

`com.paragon.agents.Agent` &nbsp;·&nbsp; **Class**

Implements `Serializable`, `Interactable`

---

A stateful AI agent that can perceive, plan, and act using tools.

Unlike `Responder` which is stateless, Agent maintains:

  
- Conversation history (via `AgenticContext`)
- Tool store for function calling
- Handoffs for multi-agent delegation
- Input/output guardrails

### Core Concepts

Agent implements an **agentic loop**: it calls the LLM, checks for tool calls, executes
tools, and repeats until the model produces a final answer or a handoff is triggered.

### Usage Examples

#### Basic Agent

```java
Agent agent = Agent.builder()
    .name("CustomerSupport")
    .instructions("You are a helpful customer support agent.")
    .model("openai/gpt-4o")
    .responder(responder)
    .addTool(new GetOrderStatusTool())
    .addTool(new RefundOrderTool())
    .maxTurns(10)
    .build();
// Blocking call - cheap with virtual threads
AgentResult result = agent.interact("What's the status of order #12345?");
System.out.println(result.output());
```

#### With Context Persistence

```java
AgentContext context = AgentContext.create();
// First turn
agent.interact("Hi, I need help with my order", context);
// Second turn (remembers first)
agent.interact("It's order #12345", context);
// Third turn
AgentResult result = agent.interact("Can you refund it?", context);
```

#### With Structured Output

```java
Agent agent = Agent.builder()
    .name("PersonExtractor")
    .instructions("Extract person information from text.")
    .model("openai/gpt-4o")
    .responder(responder)
    .outputType(Person.class)  // Structured output type
    .build();
AgentResult result = agent.interact("John is a 30-year-old engineer.");
Person person = result.parsed();  // Type-safe parsed output
```

#### Multi-Agent Handoff

```java
Agent salesAgent = Agent.builder()
    .name("Sales")
    .instructions("Handle sales inquiries.")
    .addHandoff(Handoff.to(supportAgent)
        .withDescription("Transfer to support for technical issues"))
    .build();
// When a handoff is detected, the target agent is automatically invoked
AgentResult result = salesAgent.interact("I have a billing question");
System.out.println(result.output());  // Final output from support agent
```

**See Also**

- `AgenticContext`
- `AgentResult`
- `Responder`

*Since: 1.0*

## Methods

### `builder`

```java
public static @NonNull Builder builder()
```

Creates a new Agent builder.

**Returns**

a new builder instance

---

### `name`

```java
public @NonNull String name()
```

Returns the agent's name.

**Returns**

the name

---

### `instructions`

```java
public @NonNull Prompt instructions()
```

Returns the agent's instructions (system prompt).

**Returns**

the instructions

---

### `model`

```java
public @NonNull String model()
```

Returns the model identifier.

**Returns**

the model

---

### `responder`

```java
public @NonNull Responder responder()
```

Returns the responder used by this agent.

**Returns**

the responder

---

### `maxTurns`

```java
public int maxTurns()
```

Returns the maximum number of LLM turns allowed.

**Returns**

the max turns limit

---

### `inputGuardrails`

```java
public @NonNull List<InputGuardrail> inputGuardrails()
```

Returns the input guardrails.

**Returns**

the input guardrails (unmodifiable)

---

### `outputGuardrails`

```java
public @NonNull List<OutputGuardrail> outputGuardrails()
```

Returns the output guardrails.

**Returns**

the output guardrails (unmodifiable)

---

### `handoffs`

```java
public @NonNull List<Handoff> handoffs()
```

Returns the handoffs.

**Returns**

the handoffs (unmodifiable)

---

### `toolStore`

```java
FunctionToolStore toolStore()
```

Returns the tool store. Package-private for AgentStream.

---

### `buildPayloadInternal`

```java
CreateResponsePayload buildPayloadInternal(@NonNull AgenticContext context)
```

Builds a payload from context. Package-private for AgentStream.

---

### `interactStream`

```java
public @NonNull AgentStream interactStream(@NonNull String input)
```

Interacts with the agent using streaming with full agentic loop. Creates a fresh context.

Returns an `AgentStream` that runs the complete agentic loop including guardrails,
tool execution, and multi-turn conversations, emitting events at each phase.

Example:

```java
agent.interactStream("Help me debug this code")
    .onTurnStart(turn -> System.out.println("--- Turn " + turn + " ---"))
    .onTextDelta(chunk -> System.out.print(chunk))
    .onToolCallPending((call, approve) -> approve.accept(true))  // Human-in-the-loop
    .onToolExecuted(exec -> System.out.println("Tool: " + exec.toolName()))
    .onComplete(result -> System.out.println("\nDone!"))
    .start();
```

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the user's text input |

**Returns**

an AgentStream for processing streaming events

---

### `interactStream`

```java
public @NonNull AgentStream interactStream(
      @NonNull AgenticContext context, @Nullable TraceMetadata trace)
```

Interacts with the agent with streaming using an existing context and trace metadata.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | the conversation context containing history |
| `trace` | optional trace metadata (overrides agent-level configuration) |

**Returns**

an AgentStream for processing streaming events

---

### `interactStream`

```java
public @NonNull AgentStream interactStream(@NonNull AgenticContext context)
```

Interacts with the agent using streaming with an existing context.

This is the core streaming method. All other streaming overloads ultimately delegate here
after adding their input to the context.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | the conversation context containing all history |

**Returns**

an AgentStream for processing streaming events

---

### `resume`

```java
public @NonNull AgentResult resume(@NonNull AgentRunState state)
```

Resumes a paused agent run.

Use this after calling `state.approveToolCall()` or `state.rejectToolCall()` on
a previously paused run.

Example:

```java
// Load saved state from database
AgentRunState state = loadFromDatabase(runId);
// User approved the tool
state.approveToolCall(toolOutput);
// Resume the run
AgentResult result = agent.resume(state);
```

**Parameters**

| Name | Description |
|------|-------------|
| `state` | the paused run state with approval decision set |

**Returns**

the agent result after resumption

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalStateException` | if state is not pending approval or no decision was made |

---

### `resumeStream`

```java
public @NonNull AgentStream resumeStream(@NonNull AgentRunState state)
```

Resumes a paused agent run with streaming.

**Parameters**

| Name | Description |
|------|-------------|
| `state` | the paused run state with approval decision set |

**Returns**

an AgentStream for processing remaining events

---

### `interact`

```java
public @NonNull AgentResult interact(
      @NonNull AgenticContext context, @Nullable TraceMetadata trace)
```

Core interact method. Executes the agentic loop.

This is the primary execution method. All other interact overloads ultimately delegate here
after adding their input to the context.

This method blocks until the interaction completes. When running on virtual threads,
blocking is cheap and does not consume platform threads.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | the conversation context containing all history |
| `trace` | optional trace metadata (overrides agent-level configuration) |

**Returns**

the agent's result

---

### `interactBlocking`

```java
AgentResult interactBlocking(
      @NonNull AgenticContext context,
      @Nullable LoopCallbacks callbacks,
      @Nullable TraceMetadata trace)
```

Core blocking interact method with callbacks. Package-private for AgentStream.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | the conversation context containing all history |
| `callbacks` | optional loop callbacks for streaming/events |
| `trace` | optional trace metadata to include in API requests |

**Returns**

the agent result

---

### `executeAgenticLoop`

```java
private AgentResult executeAgenticLoop(
      AgenticContext context,
      List<ToolExecution> initialExecutions,
      @Nullable LoopCallbacks callbacks,
      String fallbackHandoffText,
      @Nullable TraceMetadata trace)
```

Unified agentic loop. Shared by interact, resume, and AgentStream.

---

### `broadcastFailedEvent`

```java
private void broadcastFailedEvent(Exception exception, AgenticContext context)
```

Broadcasts a failed event for telemetry.

---

### `continueAgenticLoop`

```java
private AgentResult continueAgenticLoop(
      AgenticContext context,
      Response lastResponse,
      List<ToolExecution> previousExecutions,
      int startTurn)
```

Continues the agentic loop from a saved state (used by resume).

---

### `buildTelemetryContext`

```java
private TelemetryContext buildTelemetryContext(AgenticContext context)
```

Builds a TelemetryContext from AgentContext for trace correlation.

---

### `executeSingleToolWithErrorHandling`

```java
private ToolExecution executeSingleToolWithErrorHandling(
      FunctionToolCall call, AgenticContext context)
```

Executes a single tool with proper error handling and telemetry. Wraps errors in
ToolExecutionException for better diagnostics.

---

### `onTurnStart`

```java
default void onTurnStart(int turn)
```

Called at start of each turn.

---

### `onTurnComplete`

```java
default void onTurnComplete(Response response)
```

Called after LLM response received.

---

### `onToolCall`

```java
default boolean onToolCall(FunctionToolCall call)
```

Called when tool call is detected. Returns true to execute, false to skip.

---

### `onToolExecuted`

```java
default void onToolExecuted(ToolExecution execution)
```

Called after tool is executed.

---

### `onHandoff`

```java
default void onHandoff(Handoff handoff)
```

Called when handoff is detected.

---

### `onGuardrailFailed`

```java
default void onGuardrailFailed(GuardrailResult.Failed failed)
```

Called when guardrail fails.

---

### `onPauseRequested`

```java
default AgentRunState onPauseRequested(
        FunctionToolCall call,
        Response lastResponse,
        List<ToolExecution> executions,
        AgenticContext context)
```

Called to pause for approval. Return non-null to pause.

---

### `name`

```java
public @NonNull Builder name(@NonNull String name)
```

Sets the agent's name (required).

**Parameters**

| Name | Description |
|------|-------------|
| `name` | the agent name |

**Returns**

this builder

---

### `instructions`

```java
public @NonNull Builder instructions(@NonNull Prompt instructions)
```

Sets the agent's instructions/system prompt (required).

**Parameters**

| Name | Description |
|------|-------------|
| `instructions` | the system prompt |

**Returns**

this builder

---

### `instructions`

```java
public @NonNull Builder instructions(@NonNull String instructions)
```

Sets the agent's instructions/system prompt (required).

**Parameters**

| Name | Description |
|------|-------------|
| `instructions` | the system prompt |

**Returns**

this builder

---

### `model`

```java
public @NonNull Builder model(@NonNull String model)
```

Sets the model to use (required).

**Parameters**

| Name | Description |
|------|-------------|
| `model` | the model identifier |

**Returns**

this builder

---

### `responder`

```java
public @NonNull Builder responder(@NonNull Responder responder)
```

Sets the Responder to use for API calls (required).

**Parameters**

| Name | Description |
|------|-------------|
| `responder` | the responder instance |

**Returns**

this builder

---

### `objectMapper`

```java
public @NonNull Builder objectMapper(@NonNull ObjectMapper objectMapper)
```

Sets a custom ObjectMapper for JSON serialization.

**Parameters**

| Name | Description |
|------|-------------|
| `objectMapper` | the object mapper |

**Returns**

this builder

---

### `outputType`

```java
public @NonNull Builder outputType(@NonNull Class<?> outputType)
```

Sets the structured output type for parsing responses.

When set, the agent will parse the final response as this type.

**Parameters**

| Name | Description |
|------|-------------|
| `outputType` | the class to parse responses as |

**Returns**

this builder

---

### `maxTurns`

```java
public @NonNull Builder maxTurns(int maxTurns)
```

Sets the maximum number of LLM turns (default: 10).

**Parameters**

| Name | Description |
|------|-------------|
| `maxTurns` | the maximum turns |

**Returns**

this builder

---

### `temperature`

```java
public @NonNull Builder temperature(double temperature)
```

Sets the temperature for LLM calls (0.0 to 2.0).

Lower values make output more deterministic, higher values more creative.

**Parameters**

| Name | Description |
|------|-------------|
| `temperature` | the temperature value |

**Returns**

this builder

---

### `maxOutputTokens`

```java
public @NonNull Builder maxOutputTokens(int maxOutputTokens)
```

Sets the maximum number of output tokens for LLM responses.

**Parameters**

| Name | Description |
|------|-------------|
| `maxOutputTokens` | the maximum tokens |

**Returns**

this builder

---

### `metadata`

```java
public @NonNull Builder metadata(@NonNull Map<String, String> metadata)
```

Sets metadata to attach to the agent's requests.

**Parameters**

| Name | Description |
|------|-------------|
| `metadata` | key-value pairs |

**Returns**

this builder

---

### `addTool`

```java
public @NonNull Builder addTool(@NonNull FunctionTool<?> tool)
```

Adds a function tool.

**Parameters**

| Name | Description |
|------|-------------|
| `tool` | the tool to add |

**Returns**

this builder

---

### `addTools`

```java
public @NonNull Builder addTools(@NonNull FunctionTool<?>... tools)
```

Adds multiple function tools.

**Parameters**

| Name | Description |
|------|-------------|
| `tools` | the tools to add |

**Returns**

this builder

---

### `addHandoff`

```java
public @NonNull Builder addHandoff(@NonNull Handoff handoff)
```

Adds a handoff to another agent.

**Parameters**

| Name | Description |
|------|-------------|
| `handoff` | the handoff to add |

**Returns**

this builder

---

### `addSubAgent`

```java
public @NonNull Builder addSubAgent(@NonNull Agent subAgent, @NonNull String description)
```

Adds a sub-agent that can be invoked as a tool during execution.

Unlike handoffs which transfer control permanently, sub-agents are invoked like tools: the
parent agent calls the sub-agent, receives its output, and continues processing.

By default, the sub-agent inherits custom state (userId, sessionId, etc.) but starts with
a fresh conversation history.

Example:

```java
Agent orchestrator = Agent.builder()
    .name("Orchestrator")
    .addSubAgent(dataAnalyst, "For data analysis and statistical insights")
    .build();
```

**Parameters**

| Name | Description |
|------|-------------|
| `subAgent` | the agent to add as a callable tool |
| `description` | describes when to use this sub-agent |

**Returns**

this builder

**See Also**

- `SubAgentTool`

---

### `addSubAgent`

```java
public @NonNull Builder addSubAgent(@NonNull Agent subAgent, SubAgentTool.Config config)
```

Adds a sub-agent with custom configuration.

Example:

```java
Agent orchestrator = Agent.builder()
    .name("Orchestrator")
    .addSubAgent(dataAnalyst, SubAgentTool.Config.builder()
        .description("For data analysis")
        .shareHistory(true)  // Include full conversation context
        .build())
    .build();
```

**Parameters**

| Name | Description |
|------|-------------|
| `subAgent` | the agent to add as a callable tool |
| `config` | configuration for context sharing and description |

**Returns**

this builder

**See Also**

- `SubAgentTool.Config`

---

### `addInputGuardrail`

```java
public @NonNull Builder addInputGuardrail(@NonNull InputGuardrail guardrail)
```

Adds an input guardrail.

**Parameters**

| Name | Description |
|------|-------------|
| `guardrail` | the guardrail to add |

**Returns**

this builder

---

### `addOutputGuardrail`

```java
public @NonNull Builder addOutputGuardrail(@NonNull OutputGuardrail guardrail)
```

Adds an output guardrail.

**Parameters**

| Name | Description |
|------|-------------|
| `guardrail` | the guardrail to add |

**Returns**

this builder

---

### `addMemoryTools`

```java
public @NonNull Builder addMemoryTools(@NonNull Memory memory)
```

Adds all memory tools from a Memory storage.

This adds 4 tools: add_memory, retrieve_memories, update_memory, delete_memory. The userId
is passed securely via `interact(input, context, userId)`, NOT by the LLM, to prevent
prompt injection attacks.

**Parameters**

| Name | Description |
|------|-------------|
| `memory` | the memory storage |

**Returns**

this builder

---

### `addSkill`

```java
public @NonNull Builder addSkill(@NonNull Skill skill)
```

Adds a skill that augments this agent's capabilities.

Skills extend the agent's knowledge by adding their instructions to the agent's system
prompt. When the skill's expertise is relevant, the LLM can automatically apply it.

Example:

```java
Agent agent = Agent.builder()
    .name("Assistant")
    .addSkill(pdfProcessorSkill)
    .addSkill(dataAnalyzerSkill)
    .build();
```

**Parameters**

| Name | Description |
|------|-------------|
| `skill` | the skill to add |

**Returns**

this builder

**See Also**

- `Skill`

---

### `addSkillFrom`

```java
public @NonNull Builder addSkillFrom(@NonNull SkillProvider provider, @NonNull String skillId)
```

Loads and adds a skill from a provider.

Example:

```java
SkillProvider provider = FilesystemSkillProvider.create(Path.of("./skills"));
Agent agent = Agent.builder()
    .name("Assistant")
    .addSkillFrom(provider, "pdf-processor")
    .addSkillFrom(provider, "data-analyzer")
    .build();
```

**Parameters**

| Name | Description |
|------|-------------|
| `provider` | the skill provider |
| `skillId` | the skill identifier |

**Returns**

this builder

**See Also**

- `SkillProvider`

---

### `skillStore`

```java
public @NonNull Builder skillStore(@NonNull SkillStore store)
```

Registers all skills from a SkillStore.

Each skill's instructions are merged into the agent's system prompt.

Example:

```java
SkillStore store = new SkillStore();
store.register(pdfSkill);
store.register(dataSkill);
Agent agent = Agent.builder()
    .name("MultiSkillAgent")
    .skillStore(store)
    .build();
```

**Parameters**

| Name | Description |
|------|-------------|
| `store` | the skill store containing skills to add |

**Returns**

this builder

**See Also**

- `SkillStore`

---

### `telemetryContext`

```java
public @NonNull Builder telemetryContext(@NonNull TelemetryContext context)
```

Sets the telemetry context for agent runs.

This context is used as the default for all runs. Can be overridden per-run.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | the telemetry context |

**Returns**

this builder

---

### `traceMetadata`

```java
public @NonNull Builder traceMetadata(@Nullable TraceMetadata trace)
```

Sets the trace metadata for API requests (optional).

**Parameters**

| Name | Description |
|------|-------------|
| `trace` | the trace metadata |

**Returns**

this builder

---

### `addTelemetryProcessor`

```java
public @NonNull Builder addTelemetryProcessor(@NonNull TelemetryProcessor processor)
```

Adds a telemetry processor for OpenTelemetry integration.

Telemetry is enabled by default. Processors receive events for each agent run, tool
execution, and handoff.

**Parameters**

| Name | Description |
|------|-------------|
| `processor` | the processor to add |

**Returns**

this builder

---

### `contextManagement`

```java
public @NonNull Builder contextManagement(@NonNull ContextManagementConfig config)
```

Configures context management with a configuration object.

Context management controls how conversation history is handled when it exceeds the
model's token limit.

Example:

```java
Agent agent = Agent.builder()
    .name("Assistant")
    .model("openai/gpt-4o")
    .responder(responder)
    .contextManagement(ContextManagementConfig.builder()
        .strategy(new SlidingWindowStrategy())
        .maxTokens(4000)
        .build())
    .build();
```

**Parameters**

| Name | Description |
|------|-------------|
| `config` | the context management configuration |

**Returns**

this builder

**See Also**

- `ContextManagementConfig`

---

### `retryPolicy`

```java
public @NonNull Builder retryPolicy(com.paragon.http.RetryPolicy retryPolicy)
```

Sets the retry policy for handling transient API failures.

This configures automatic retry with exponential backoff for:

  
- 429 - Rate limiting
- 500, 502, 503, 504 - Server errors
- Network failures (connection timeout, etc.)

Note: If you provide a custom Responder via `.responder(Responder)`, configure retry
on the Responder instead. This setting is used when no Responder is provided and one is
created internally.

**Parameters**

| Name | Description |
|------|-------------|
| `retryPolicy` | the retry policy to use |

**Returns**

this builder

**See Also**

- `com.paragon.http.RetryPolicy`

---

### `maxRetries`

```java
public @NonNull Builder maxRetries(int maxRetries)
```

Sets the maximum number of retry attempts with default backoff settings.

Convenience method equivalent to:

```java
.retryPolicy(RetryPolicy.builder().maxRetries(n).build())
```

Note: If you provide a custom Responder via `.responder(Responder)`, configure retry
on the Responder instead.

**Parameters**

| Name | Description |
|------|-------------|
| `maxRetries` | maximum retry attempts (0 = no retries) |

**Returns**

this builder

---

### `structured`

```java
public <T> @NonNull StructuredBuilder<T> structured(@NonNull Class<T> outputType)
```

Configures the agent to produce structured output of the specified type.

Returns a `StructuredBuilder` that will build an `Agent.Structured` instead of
a regular `Agent`.

Example:

```java
var agent = Agent.builder()
    .name("Extractor")
    .structured(Person.class)
    .build();
StructuredAgentResult result = agent.interact("John is 30");
Person person = result.output();  // Type-safe!
```

**Parameters**

| Name | Description |
|------|-------------|
| `<T>` | the output type |
| `outputType` | the class of the structured output |

**Returns**

a structured builder that builds Agent.Structured

---

### `build`

```java
public @NonNull Agent build()
```

Builds the Agent instance.

**Returns**

the configured agent

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if required fields are missing |

---

### `build`

```java
public @NonNull Structured<T> build()
```

Builds the type-safe structured agent.

**Returns**

the configured Structured agent

**Throws**

| Type | Condition |
|------|-----------|
| `NullPointerException` | if required fields are missing |

---

### `name`

```java
public @NonNull String name()
```

Returns the wrapped agent's name.

---

### `outputType`

```java
public @NonNull Class<T> outputType()
```

Returns the structured output type.

---

### `interact`

```java
public @NonNull StructuredAgentResult<T> interact(@NonNull String input)
```

Interacts with the agent and returns type-safe structured output.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the user's text input |

**Returns**

the typed result

---

### `interact`

```java
public @NonNull StructuredAgentResult<T> interact(
        @NonNull String input, @NonNull AgenticContext context)
```

Interacts with the agent with context and returns type-safe structured output.

**Parameters**

| Name | Description |
|------|-------------|
| `input` | the user's text input |
| `context` | the conversation context |

**Returns**

the typed result

---

### `interact`

```java
public @NonNull StructuredAgentResult<T> interact(@NonNull AgenticContext context)
```

Interacts with the agent using an existing context.

This is the core method. All other interact overloads delegate here.

**Parameters**

| Name | Description |
|------|-------------|
| `context` | the conversation context containing all history |

**Returns**

the typed result

---

### `parseResult`

```java
private @NonNull StructuredAgentResult<T> parseResult(AgentResult result)
```

Parses the AgentResult into a type-safe StructuredAgentResult.
