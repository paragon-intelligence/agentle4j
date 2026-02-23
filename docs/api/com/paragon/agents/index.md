# Package `com.paragon.agents`

> This docs was updated at: 2026-02-23

---

## :material-code-braces: Classs

| Name | Description |
|------|-------------|
| [`Agent`](agent.md) | A stateful AI agent that can perceive, plan, and act using tools |
| [`AgentNetwork`](agentnetwork.md) | Implements the Network pattern: decentralized peer-to-peer agent communication |
| [`AgentResult`](agentresult.md) | The result of an agent interaction, containing the final output and execution metadata |
| [`AgentRunState`](agentrunstate.md) | Serializable state of a paused agent run |
| [`AgentService`](agentservice.md) |  |
| [`AgentStream`](agentstream.md) | Streaming agent interaction with full agentic loop |
| [`AgenticContext`](agenticcontext.md) | Holds conversation state for an agent interaction |
| [`GuardrailRegistry`](guardrailregistry.md) | Thread-safe global registry for named guardrails |
| [`Handoff`](handoff.md) | A handoff defines when and to which agent control should be transferred |
| [`HierarchicalAgents`](hierarchicalagents.md) | Implements the Hierarchical pattern: multi-layered supervisor structure |
| [`InMemoryMemory`](inmemorymemory.md) | Thread-safe in-memory implementation of `Memory` with user isolation |
| [`InteractableSubAgentTool`](interactablesubagenttool.md) | Wraps any Interactable as a FunctionTool, enabling composition of multi-agent patterns |
| [`MemoryTool`](memorytool.md) | Memory exposed as FunctionTools for agent use |
| [`NamedInputGuardrail`](namedinputguardrail.md) | An `InputGuardrail` wrapper that carries a string ID for serialization support |
| [`NamedOutputGuardrail`](namedoutputguardrail.md) | An `OutputGuardrail` wrapper that carries a string ID for serialization support |
| [`NetworkStream`](networkstream.md) | Streaming wrapper for AgentNetwork that provides event callbacks during network discussions |
| [`ParallelAgents`](parallelagents.md) | Orchestrates parallel execution of multiple agents |
| [`ParallelStream`](parallelstream.md) | Streaming wrapper for ParallelAgents that provides event callbacks during parallel execution |
| [`RouterAgent`](routeragent.md) | A specialized agent for routing inputs to appropriate target agents |
| [`RouterStream`](routerstream.md) | Streaming wrapper for RouterAgent that provides event callbacks during routing and execution |
| [`SubAgentTool`](subagenttool.md) | Wraps an Agent as a FunctionTool, enabling agent composition |
| [`SupervisorAgent`](supervisoragent.md) | Implements the Supervisor pattern: a central agent that coordinates multiple worker agents |

## :material-approximately-equal: Interfaces

| Name | Description |
|------|-------------|
| [`GuardrailResult`](guardrailresult.md) | Result of a guardrail validation check |
| [`InputGuardrail`](inputguardrail.md) | Validates user input before agent processing |
| [`Memory`](memory.md) | Interface for agent long-term memory storage with user isolation |
| [`OutputGuardrail`](outputguardrail.md) | Validates agent output before returning to the user |

## :material-database: Records

| Name | Description |
|------|-------------|
| [`AgentDefinition`](agentdefinition.md) | A richly annotated agent definition record designed for **LLM structured output** |
| [`MemoryEntry`](memoryentry.md) | Represents a single memory entry for long-term agent memory |
| [`ResponderBlueprint`](responderblueprint.md) | Serializable descriptor for a `Responder` configuration |
| [`StructuredAgentResult`](structuredagentresult.md) | The result of a structured agent interaction, containing the typed output |
| [`ToolExecution`](toolexecution.md) | Records the execution details of a single tool call during an agent run |
