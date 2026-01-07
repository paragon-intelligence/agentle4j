# Changelog

All notable changes to Agentle4j will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.5.0] - 2026-01-04

### Refactored

Completely removed **all** `CompletableFuture` references in all the classes. Now, I decided to take advantage of
Virtual Threads and make the interface simpler.

## [0.4.0] - 2026-01-04

### Added

- **SupervisorAgent** - Central coordinator pattern for complex task orchestration
    - Manages multiple worker agents with task decomposition
    - `orchestrate()` and `orchestrateStream()` methods
    - Worker agents accessed as sub-agent tools automatically
    - Configurable max turns for supervision loop

- **AgentNetwork** - Decentralized peer-to-peer agent communication
    - Sequential discussion rounds where agents build on each other's ideas
    - `discuss()` for multi-round conversations
    - `broadcast()` for parallel message dissemination
    - Optional synthesizer agent to combine all contributions
    - `NetworkResult` with contribution filtering by agent/round

- **HierarchicalAgents** - Multi-layered organizational structure
    - Executive → Manager → Worker hierarchy
    - `addDepartment()` to create organizational units
    - `execute()` flows tasks through the hierarchy
    - `sendToDepartment()` for direct department routing
    - Streaming support with `executeStream()`

### Improved

- **Test Coverage** - New patterns achieve 100% line and branch coverage
    - `SupervisorAgentTest` - 26 tests
    - `AgentNetworkTest` - 28 tests
    - `HierarchicalAgentsTest` - 23 tests

### Documentation

- Added "Multiagent Interaction Patterns - A wide overview" section to README
- Added pattern comparison table with 6 patterns
- Added SupervisorAgent, AgentNetwork, HierarchicalAgents guides to agents.md
- Included multiagent.png diagram in README

---

## [0.3.0] - 2026-01-04

### Added

- **Sub-Agents (Agent-as-Tool)** - Invoke agents as tools within a parent agent's execution loop
    - `SubAgentTool` class for wrapping an Agent as a FunctionTool
    - `Agent.Builder.addSubAgent()` methods with description or config
    - `SubAgentTool.Config` for context sharing options:
        - `shareState(boolean)` - Share custom state like userId (default: true)
        - `shareHistory(boolean)` - Include full conversation context (default: false)
    - Tool naming follows `invoke_[agent_snake_case_name]` pattern

- **Prompt Management** - External prompt providers for managing agent instructions
    - `PromptProvider` interface for abstracting prompt sources
    - `FilesystemPromptProvider` - Load prompts from local files
    - `LangfusePromptProvider` - Fetch prompts from Langfuse API with:
        - Basic authentication support
        - Configurable retry with exponential backoff
        - Polymorphic response parsing (text and chat prompts)
    - `PromptProviderException` for prompt retrieval failures

- **Enhanced Prompt Templating** - Advanced template syntax in `Prompt.java`
    - Variable interpolation: `{{variable}}`
    - Nested properties: `{{user.name}}`
    - Conditional blocks: `{{#if condition}}...{{/if}}`
    - Iteration blocks: `{{#each items}}...{{/each}}`

### Improved

- **Test Coverage** - Increased overall coverage to 88%
    - Comprehensive tests for `SubAgentTool` (25 tests)
    - Enhanced `Agent.java` coverage (95%+)
    - `PromptProvider` implementations fully tested

### Documentation

- Added Sub-agents section to README and agents guide
- Added Prompt Management guide
- Enhanced Spring Boot integration guide with agent management patterns
- Improved multi-agent documentation with comparison tables

---

[0.4.0]: https://github.com/paragon-intelligence/agentle4j/compare/v0.3.0...v0.4.0

[0.3.0]: https://github.com/paragon-intelligence/agentle4j/releases/tag/v0.3.0
