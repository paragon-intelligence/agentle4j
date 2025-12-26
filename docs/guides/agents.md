# Agents Guide

Agents are high-level abstractions that wrap a Responder with tools, guardrails, memory, and multi-agent orchestration. They handle the complete agentic loop automatically.

---

## What is an Agent?

While `Responder` is a low-level HTTP client, `Agent` provides:

| Feature | Description |
|---------|-------------|
| **Instructions** | System prompt defining behavior |
| **Tools** | Functions the AI can call |
| **Guardrails** | Input/output validation |
| **Memory** | Cross-conversation persistence |
| **Handoffs** | Routing to other agents |
| **Agentic Loop** | Automatic tool execution until final answer |

```mermaid
flowchart TB
    subgraph Agent["Agent"]
        I[Instructions]
        T[Tools]
        G[Guardrails]
        M[Memory]
        H[Handoffs]
    end
    
    R[Responder] --> Agent
    Agent --> LLM[LLM API]
    LLM --> Agent
```

---

## Creating an Agent

### Basic Agent

```java
// First, create a Responder
Responder responder = Responder.builder()
    .openRouter()
    .apiKey(System.getenv("OPENROUTER_API_KEY"))
    .build();

// Then, create an Agent
Agent agent = Agent.builder()
    .name("Assistant")
    .model("openai/gpt-4o")
    .instructions("You are a helpful assistant. Be concise and friendly.")
    .responder(responder)
    .build();

// Interact with the agent
AgentResult result = agent.interact("Hello! What can you help me with?").join();
System.out.println(result.output());
```

### Agent with Tools

```java
Agent agent = Agent.builder()
    .name("WeatherBot")
    .model("openai/gpt-4o")
    .instructions("""
        You are a weather assistant. 
        Use the get_weather tool when users ask about weather.
        Always specify the location and unit.
        """)
    .responder(responder)
    .addTool(weatherTool)
    .addTool(forecastTool)
    .build();

// The agent will automatically call tools when needed
AgentResult result = agent.interact("What's the weather in Tokyo?").join();
System.out.println(result.output());
// Output: "The weather in Tokyo is 25°C and sunny."
```

---

## The Agentic Loop

When you call `agent.interact()`, Agentle runs the **agentic loop**:

```
┌─────────────────────────────────────────────────────────────────┐
│                       AGENTIC LOOP                              │
├─────────────────────────────────────────────────────────────────┤
│  1. ✅ Validate input (input guardrails)                        │
│  2. 📦 Build payload from context and history                   │
│  3. 🤖 Call LLM                                                 │
│  4. 🔧 If tool calls detected:                                  │
│     • Check for handoffs → route to other agent                 │
│     • Execute tools → add results to context                    │
│     • Go to step 3 (multi-turn)                                 │
│  5. ✅ Validate output (output guardrails)                      │
│  6. 📤 Return AgentResult                                       │
└─────────────────────────────────────────────────────────────────┘
```

The loop continues until the LLM responds without tool calls (final answer) or max turns is reached.

---

## AgentContext

`AgentContext` holds the per-conversation state:

```java
// Create fresh context for each conversation
AgentContext context = AgentContext.create();

// Store custom state
context.setState("userId", "user-123");
context.setState("orderId", 42);
context.setState("isPremium", true);

// Use context in interactions
AgentResult result = agent.interact("What's my order status?", context).join();

// Retrieve state later
String userId = context.getState("userId", String.class);
int orderId = context.getState("orderId", Integer.class);
```

### Resuming Conversations

```java
// Create context with existing history
List<ResponseInputItem> previousMessages = loadFromDatabase();
AgentContext resumed = AgentContext.withHistory(previousMessages);

// Continue the conversation
agent.interact("Thanks for the help earlier!", resumed).join();
```

---

## 🛡️ Guardrails

Guardrails validate inputs and outputs to ensure safe, appropriate responses.

### Input Guardrails

Validate user input before sending to LLM:

```java
Agent agent = Agent.builder()
    .name("SafeAssistant")
    .model("openai/gpt-4o")
    .instructions("You are a helpful assistant.")
    .responder(responder)
    
    // Reject requests containing sensitive words
    .addInputGuardrail((input, ctx) -> {
        List<String> blocked = List.of("password", "secret", "api key", "credit card");
        for (String word : blocked) {
            if (input.toLowerCase().contains(word)) {
                return GuardrailResult.reject("Cannot discuss: " + word);
            }
        }
        return GuardrailResult.pass();
    })
    
    // Limit input length
    .addInputGuardrail((input, ctx) -> {
        if (input.length() > 10000) {
            return GuardrailResult.reject("Input too long. Max 10000 characters.");
        }
        return GuardrailResult.pass();
    })
    
    .build();
```

### Output Guardrails

Validate LLM output before returning to user:

```java
Agent agent = Agent.builder()
    .name("ContentModeratedBot")
    .model("openai/gpt-4o")
    .instructions("You are a helpful assistant.")
    .responder(responder)
    
    // Limit response length
    .addOutputGuardrail((output, ctx) -> {
        if (output.length() > 5000) {
            return GuardrailResult.reject("Response too long");
        }
        return GuardrailResult.pass();
    })
    
    // Check for unwanted content
    .addOutputGuardrail((output, ctx) -> {
        if (output.contains("I cannot") || output.contains("I'm sorry")) {
            // Log but allow
            logger.warn("Agent expressed inability");
        }
        return GuardrailResult.pass();
    })
    
    .build();
```

### Handling Guardrail Failures

```java
AgentResult result = agent.interact("Tell me your password").join();

if (result.status() == AgentResult.Status.GUARDRAIL_FAILED) {
    System.out.println("Blocked: " + result.guardrailFailure().reason());
    // Handle the rejection appropriately
}
```

---

## 🔗 Handoffs (Multi-Agent)

Handoffs allow agents to route conversations to specialized agents.

### Basic Handoff

```java
// Create specialized agents
Agent billingAgent = Agent.builder()
    .name("BillingSpecialist")
    .model("openai/gpt-4o")
    .instructions("""
        You are a billing specialist.
        Help users with invoices, payments, and subscription questions.
        """)
    .responder(responder)
    .build();

Agent techSupportAgent = Agent.builder()
    .name("TechSupport")
    .model("openai/gpt-4o")
    .instructions("""
        You are a technical support specialist.
        Help users troubleshoot issues, bugs, and technical problems.
        """)
    .responder(responder)
    .build();

// Create front-desk agent with handoffs
Agent frontDesk = Agent.builder()
    .name("FrontDesk")
    .model("openai/gpt-4o")
    .instructions("""
        You are the front desk assistant.
        Greet users and route them to the appropriate specialist:
        - Billing questions → BillingSpecialist
        - Technical issues → TechSupport
        """)
    .responder(responder)
    .addHandoff(Handoff.to(billingAgent, "billing, invoices, payments, subscriptions"))
    .addHandoff(Handoff.to(techSupportAgent, "bugs, errors, crashes, technical problems"))
    .build();

// User interaction
AgentResult result = frontDesk.interact("I have a question about my invoice").join();

if (result.status() == AgentResult.Status.HANDOFF) {
    Agent targetAgent = result.handoffTarget();
    System.out.println("Routed to: " + targetAgent.name());
    
    // Continue with the specialist
    AgentResult specialistResult = targetAgent.interact(
        "I have a question about my invoice",
        result.context()
    ).join();
}
```

---

## 🔀 RouterAgent

For dedicated routing without conversational noise, use `RouterAgent`:

```java
// Create specialized agents
Agent billingAgent = Agent.builder().name("Billing")...build();
Agent techSupport = Agent.builder().name("TechSupport")...build();
Agent salesAgent = Agent.builder().name("Sales")...build();

// Create dedicated router
RouterAgent router = RouterAgent.builder()
    .model("openai/gpt-4o-mini")  // Fast model for routing
    .responder(responder)
    .addRoute(billingAgent, "billing, invoices, payments, charges, subscription")
    .addRoute(techSupport, "bugs, errors, crashes, technical problems, not working")
    .addRoute(salesAgent, "pricing, demos, upgrades, features, enterprise")
    .fallback(techSupport)  // Default if no match
    .build();

// Option 1: Route and execute
AgentResult result = router.route("I have a question about my invoice").join();
System.out.println("Handled by: " + result.handoffTarget().name());

// Option 2: Just classify (don't execute)
Agent selected = router.classify("My app keeps crashing").join();
System.out.println("Would route to: " + selected.name());
// selected == techSupport
```

### RouterAgent vs Handoffs

| Feature | RouterAgent | Handoffs |
|---------|-------------|----------|
| **Purpose** | Dedicated classifier | Part of conversation |
| **Model Usage** | Uses fast, cheap model | Uses agent's model |
| **Conversation** | Pure routing, no small talk | Can converse before routing |
| **Best For** | High-volume routing | Conversational routing |

---

## 🧠 Memory

Add persistent memory across conversations:

```java
// Create memory store
Memory memory = InMemoryMemory.create();

// Create agent with memory tools
Agent agent = Agent.builder()
    .name("RememberingAssistant")
    .model("openai/gpt-4o")
    .instructions("""
        You remember user preferences and past conversations.
        Use the store_memory and retrieve_memory tools to remember things.
        Always check memory before answering personal questions.
        """)
    .responder(responder)
    .addMemoryTools(memory)  // Adds store/retrieve tools automatically
    .build();

// Create context with user ID
AgentContext context = AgentContext.create();
context.setState("userId", "user-123");

// First conversation - store preference
agent.interact("My favorite color is blue", context).join();

// Later conversation - retrieve preference
AgentResult result = agent.interact("What's my favorite color?", context).join();
System.out.println(result.output());
// Output: "Your favorite color is blue!"
```

### Custom Memory Implementation

```java
// Implement your own memory store (e.g., Redis, PostgreSQL)
public class RedisMemory implements Memory {
    private final RedisClient redis;
    
    @Override
    public void store(String userId, String key, String value) {
        redis.hset("memory:" + userId, key, value);
    }
    
    @Override
    public String retrieve(String userId, String key) {
        return redis.hget("memory:" + userId, key);
    }
    
    @Override
    public List<String> search(String userId, String query) {
        // Implement semantic search
        return redis.search("memory:" + userId, query);
    }
}
```

---

## 🧑‍💻 Human-in-the-Loop

Control tool execution with **per-tool** approval workflows for sensitive operations.

### Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        HUMAN-IN-THE-LOOP FLOW                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Agent receives: "Delete records and check weather"                         │
│                          │                                                  │
│                          ▼                                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    LLM Response                                      │   │
│  │  Tool Calls: [delete_records, get_weather]                          │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                          │                                                  │
│          ┌───────────────┴───────────────┐                                  │
│          ▼                               ▼                                  │
│  ┌───────────────────┐          ┌───────────────────┐                      │
│  │ delete_records    │          │ get_weather       │                      │
│  │ ⚠️ requiresConf=  │          │ ✅ requiresConf=  │                      │
│  │      TRUE         │          │      FALSE        │                      │
│  └─────────┬─────────┘          └─────────┬─────────┘                      │
│            │                              │                                 │
│            ▼                              ▼                                 │
│    ┌──────────────┐              ┌──────────────┐                          │
│    │ WAIT FOR     │              │ AUTO-EXECUTE │                          │
│    │ APPROVAL     │              │ immediately  │                          │
│    └──────────────┘              └──────────────┘                          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Tool Confirmation Matrix

| Tool | `requiresConfirmation` | With `onToolCallPending` | With `onPause` | Without handlers |
|------|------------------------|-------------------------|----------------|------------------|
| delete_records | `true` | ⏸️ Waits for callback | ⏸️ Pauses agent | ⚡ Auto-executes |
| send_email | `true` | ⏸️ Waits for callback | ⏸️ Pauses agent | ⚡ Auto-executes |
| get_weather | `false` (default) | ⚡ Auto-executes | ⚡ Auto-executes | ⚡ Auto-executes |
| calculate | `false` (default) | ⚡ Auto-executes | ⚡ Auto-executes | ⚡ Auto-executes |

### Synchronous vs Async Flow

```mermaid
sequenceDiagram
    participant U as User
    participant A as Agent
    participant T as Tool
    participant H as Human Approver
    
    Note over U,H: SYNC FLOW (onToolCallPending)
    U->>A: "Delete records"
    A->>A: LLM calls delete_records
    A->>H: 🔧 Approval needed!
    H->>A: approve.accept(true)
    A->>T: Execute tool
    T->>A: "Deleted 50 records"
    A->>U: "Done! Deleted 50 records"
    
    Note over U,H: ASYNC FLOW (onPause)
    U->>A: "Delete records"
    A->>A: LLM calls delete_records
    A->>A: PAUSE (save state to DB)
    A->>H: 📧 Notify manager
    Note over A: Days later...
    H->>A: POST /approve (approved=true)
    A->>T: Execute tool manually
    T->>A: "Deleted 50 records"
    A->>A: state.approveToolCall("Deleted 50 records")
    A->>A: agent.resume(state)
    A->>U: "Done! Deleted 50 records"
```

### Multiple Tools Scenario

```
User: "Delete old users, send report email, and check weather"

LLM Response → 3 tool calls:
  1. delete_old_users  (requiresConfirmation = true)  ← PAUSES HERE
  2. send_report_email (requiresConfirmation = true)  ← Waits until (1) resumes
  3. get_weather       (requiresConfirmation = false) ← Waits until (1) resumes

Flow with onPause:
┌──────────────────────────────────────────────────────────────────────────────┐
│ Step 1: Agent pauses on FIRST dangerous tool                                 │
│         state.pendingToolCall() = "delete_old_users"                         │
│         Other tools NOT processed yet                                        │
├──────────────────────────────────────────────────────────────────────────────┤
│ Step 2: Manager approves delete_old_users                                    │
│         agent.resume(state) → continues loop                                 │
├──────────────────────────────────────────────────────────────────────────────┤
│ Step 3: Agent pauses on SECOND dangerous tool                                │
│         state.pendingToolCall() = "send_report_email"                        │
├──────────────────────────────────────────────────────────────────────────────┤
│ Step 4: Manager approves send_report_email                                   │
│         agent.resume(state) → continues loop                                 │
├──────────────────────────────────────────────────────────────────────────────┤
│ Step 5: get_weather auto-executes (no confirmation needed)                   │
│         Agent completes and returns final result                             │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Per-Tool Confirmation

Mark sensitive tools with `requiresConfirmation = true`:

```java
// ❌ Dangerous tool - requires human approval
@FunctionMetadata(
    name = "delete_records",
    description = "Permanently deletes database records",
    requiresConfirmation = true  // ⬅️ HITL enabled
)
public class DeleteRecordsTool extends FunctionTool<DeleteParams> {
    @Override
    public FunctionToolCallOutput call(DeleteParams params) {
        database.delete(params.table(), params.filter());
        return FunctionToolCallOutput.success(callId, "Deleted");
    }
}

// ✅ Safe tool - auto-executes
@FunctionMetadata(name = "get_weather", description = "Gets weather data")
public class GetWeatherTool extends FunctionTool<WeatherParams> {
    // Default: requiresConfirmation = false
    @Override
    public FunctionToolCallOutput call(WeatherParams params) {
        return FunctionToolCallOutput.success(callId, weatherApi.get(params.city()));
    }
}
```

### Synchronous Approval (Immediate)

For CLI apps, chatbots, or UI dialogs where user can respond immediately:

```java
agent.interactStream("Delete all test records and check weather in Tokyo")
    .onToolCallPending((toolCall, approve) -> {
        // ⚠️ Only called for tools with requiresConfirmation=true
        System.out.println("🔧 Approval required: " + toolCall.name());
        System.out.println("   Arguments: " + toolCall.arguments());
        
        System.out.print("Execute? (y/n): ");
        boolean approved = new Scanner(System.in).nextLine().equalsIgnoreCase("y");
        
        approve.accept(approved);  // true = execute, false = reject
    })
    .onToolExecuted(exec -> {
        System.out.println("✅ " + exec.toolName() + " completed");
    })
    .start();

// Result:
// get_weather → auto-executes (no confirmation needed)
// delete_records → waits for user input
```

### Async Pause/Resume (Long-Running)

For approvals that take hours or days (manager approval, compliance review):

> **Note**: With `onPause`, the agent pauses on the **first** tool requiring confirmation. 
> Use `state.pendingToolCall()` to see which specific tool is waiting for approval.
> After resuming, if there are more tools requiring confirmation, it will pause again for each.

```java
// ═══════════════════════════════════════════════════════
// STEP 1: Start agent and pause when dangerous tool called
// ═══════════════════════════════════════════════════════
agent.interactStream("Delete all customer records from production")
    .onPause(state -> {
        // AgentRunState is Serializable - save to database
        String json = objectMapper.writeValueAsString(state);
        String stateId = state.pendingToolCall().callId();
        database.save("pending:" + stateId, json);
        
        // Notify approver (email, Slack, Teams, etc.)
        slackClient.send("#approvals", String.format(
            "🔧 **Tool Approval Needed**\n" +
            "• Tool: `%s`\n" +
            "• Args: `%s`\n" +
            "• Approve: https://app.example.com/approve/%s",
            state.pendingToolCall().name(),
            state.pendingToolCall().arguments(),
            stateId
        ));
    })
    .start();
// Agent is now paused - returns AgentResult.Status.PAUSED

// ═══════════════════════════════════════════════════════
// STEP 2: Days later, when manager approves via web UI
// ═══════════════════════════════════════════════════════
@PostMapping("/approve/{stateId}")
public ResponseEntity<String> handleApproval(
    @PathVariable String stateId,
    @RequestBody ApprovalRequest request
) {
    // Load saved state
    String json = database.get("pending:" + stateId);
    AgentRunState state = objectMapper.readValue(json, AgentRunState.class);
    
    FunctionToolCall pendingTool = state.pendingToolCall();
    
    if (request.approved()) {
        // ✅ APPROVE: Execute the tool and provide the result to the LLM
        //
        // The parameter is the TOOL RESULT that will be shown to the model.
        // You must execute the tool manually and provide its output.
        //
        // Example: If pending tool is "delete_records" with args {"table": "users"}
        String toolResult = executeToolManually(pendingTool);
        // toolResult = "Deleted 150 records from users table"
        
        state.approveToolCall(toolResult);  // ⬅️ This is the tool's output!
        
    } else {
        // ❌ REJECT: Tell the model the tool was not executed
        //
        // The reason is shown to the model so it can respond appropriately
        state.rejectToolCall("Manager denied: " + request.reason());
        // Model will see: "Tool execution was rejected: Manager denied: Too risky"
    }
    
    // Resume agent execution from where it paused
    AgentResult result = agent.resume(state).join();
    
    // Notify original user
    notifyUser(result.output());
    
    // Cleanup
    database.delete("pending:" + stateId);
    
    return ResponseEntity.ok("Processed");
}

// Helper method to execute the tool manually after approval
private String executeToolManually(FunctionToolCall call) {
    switch (call.name()) {
        case "delete_records" -> {
            var params = objectMapper.readValue(call.arguments(), DeleteParams.class);
            int count = database.delete(params.table(), params.filter());
            return "Deleted " + count + " records from " + params.table();
        }
        case "send_email" -> {
            var params = objectMapper.readValue(call.arguments(), EmailParams.class);
            emailService.send(params.to(), params.subject(), params.body());
            return "Email sent to " + params.to();
        }
        default -> throw new IllegalArgumentException("Unknown tool: " + call.name());
    }
}
```

> **Important**: When using `onPause`, the tool is NOT executed automatically. You must:
> 1. Execute the tool logic manually in your approval handler
> 2. Pass the result to `approveToolCall(result)` 
>
> This gives you full control - useful for compliance logging, audit trails, or modified execution.

---

## ⚡ Parallel Agents

Run multiple agents concurrently for complex tasks:

```java
// Create specialized agents
Agent researcher = Agent.builder()
    .name("Researcher")
    .model("openai/gpt-4o")
    .instructions("You research topics thoroughly and find facts.")
    .responder(responder)
    .build();

Agent analyst = Agent.builder()
    .name("Analyst")
    .model("openai/gpt-4o")
    .instructions("You analyze data and identify patterns.")
    .responder(responder)
    .build();

Agent writer = Agent.builder()
    .name("Writer")
    .model("openai/gpt-4o")
    .instructions("You write clear, engaging summaries.")
    .responder(responder)
    .build();

// Create orchestrator
ParallelAgents team = ParallelAgents.of(researcher, analyst);
```

### Run All in Parallel

```java
// All agents process the same input concurrently
List<AgentResult> results = team.run("Analyze market trends in AI").join();

for (AgentResult result : results) {
    System.out.println("== " + result.agentName() + " ==");
    System.out.println(result.output());
}
```

### Race - First Result Wins

```java
// Use when you want the fastest response
AgentResult fastest = team.runFirst("Quick analysis needed").join();
System.out.println("First response from: " + fastest.agentName());
```

### Synthesize Results

```java
// Combine outputs from multiple agents with a synthesizer
AgentResult combined = team.runAndSynthesize(
    "What's the outlook for tech stocks?",
    writer  // Writer agent combines researcher + analyst outputs
).join();

System.out.println(combined.output());
// Writer produces a unified report from both perspectives
```

---

## 🌊 AgentStream

Full agentic loop with streaming and real-time events:

```java
agent.interactStream("Research and summarize AI trends")
    // Turn lifecycle
    .onTurnStart(turn -> {
        System.out.println("=== Turn " + turn + " ===");
    })
    .onTurnComplete(response -> {
        System.out.println("Turn complete, tokens: " + response.usage().totalTokens());
    })
    
    // Text streaming
    .onTextDelta(delta -> {
        System.out.print(delta);
        System.out.flush();
    })
    
    // Tool execution
    .onToolCall((name, args) -> {
        System.out.println("\n🔧 Calling tool: " + name);
    })
    .onToolExecuted(exec -> {
        System.out.println("✅ " + exec.toolName() + " returned: " + exec.result());
    })
    
    // Agent handoffs
    .onHandoff(handoff -> {
        System.out.println("→ Handing off to: " + handoff.targetAgent().name());
    })
    
    // Guardrail failures
    .onGuardrailFailed(failed -> {
        System.err.println("⛔ Blocked: " + failed.reason());
    })
    
    // Completion
    .onComplete(result -> {
        System.out.println("\n\n✅ Done!");
        System.out.println("Final status: " + result.status());
    })
    .onError(error -> {
        System.err.println("Error: " + error.getMessage());
    })
    
    .start();
```

---

## Structured Output Agent

Get type-safe responses from agents:

```java
// Define output schema
record Analysis(
    String summary,
    List<String> keyPoints,
    int sentimentScore,  // -100 to 100
    List<String> recommendations
) {}

// Create structured agent
Agent.Structured<Analysis> analyst = Agent.builder()
    .name("Analyst")
    .model("openai/gpt-4o")
    .instructions("""
        You analyze text and provide structured insights.
        Sentiment score should be from -100 (very negative) to 100 (very positive).
        """)
    .responder(responder)
    .structured(Analysis.class);  // Terminal method

// Get typed result
AgentResult result = analyst.interact("Analyze this quarterly report...").join();
Analysis analysis = result.parsed(Analysis.class);

System.out.println("Summary: " + analysis.summary());
System.out.println("Sentiment: " + analysis.sentimentScore());
for (String point : analysis.keyPoints()) {
    System.out.println("• " + point);
}
```

---

## Best Practices

### ✅ Do

```java
// Reuse agents across requests (they're thread-safe)
private final Agent agent;

public MyService(Responder responder) {
    this.agent = Agent.builder()
        .name("ServiceAgent")...build();
}

// Use specific, detailed instructions
.instructions("""
    You are a customer support agent for Acme Corp.
    Be helpful, professional, and concise.
    If you don't know something, say so honestly.
    Never discuss competitor products.
    """)

// Add appropriate guardrails
.addInputGuardrail(...)
.addOutputGuardrail(...)
```

### ❌ Don't

```java
// Don't create new agents for each request
public String chat(String message) {
    Agent agent = Agent.builder()...build();  // Bad!
    return agent.interact(message).join().output();
}

// Don't use vague instructions
.instructions("Be helpful")  // Too vague!

// Don't forget to handle errors
agent.interact(input).join();  // Uncaught exceptions!
```

---

## Next Steps

- [Function Tools Guide](tools.md) - Create custom tools
- [Streaming Guide](streaming.md) - Advanced streaming patterns
- [Observability Guide](observability.md) - Monitor your agents
