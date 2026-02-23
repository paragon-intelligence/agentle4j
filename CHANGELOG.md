# Changelog

All notable changes to Agentle4j will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.7.0] - 2026-02-23

### Added

- **Tool Search & Planning Capabilities** 🔍
    - **Tool Search Integration**: Dynamically select and load relevant tools on the fly, dramatically reducing context window overhead for agents with huge tool sets.
    - **Parallel & Batched Tools Execution**: Agents can now plan and execute multiple tool calls simultaneously in batched and parallel fashion.

- **WhatsApp & Messaging Engine** 💬
    - Robust, fully-tested out-of-the-box WhatsApp integration natively connected to agent workflows.
    - **Smart Messaging Flow**: Introduced adaptive batching, robust rate-limiting, and deep conversation history management.
    - **Rich Interactive Content**: Send interactive payloads and even audio files directly natively via standard payload models.
    - Fail-fast configuration mechanism integrated via Jakarta Validations and comprehensive error tracking capabilities.

- **Agent Blueprints & Structured Outputs** 🏗️
    - `InteractableBlueprint` introduced as a standard to elegantly serialize implementations across agents.
    - Completely redesigned `AgentDefinition` to be strictly behavioral—stripping away heavy infrastructure variables like `Responder`s, establishing a flawless standard for receiving meta-agent definitions via LLM Structured Outputs.
    - Structured outputs natively deployed across **ALL** organizational and architectural agent patterns (Supervisor, Parallel, etc.).

- **Tracing & Framework Core Enhancements** 📡
    - Added deep trace support through `TraceMetadata` bridging OpenRouter specs to underlying responders for optimal ecosystem observability.
    - Consolidating inputs into the `AgenticContext`, avoiding huge boilerplate and standardizing context consumption on defaults.

### Improved & Fixed

- Assorted code format optimizations and removal of redundant code chunks across release processes.

### Documentation

- **Brand New MkDocs API Reference** 📖: Fully rewritten and structured API documentation setup for high readability.
- Authored sweeping new architecture guides explaining `Tool Planning`, `Tool Search`, `Blueprints`, and WhatsApp `Messaging` layers.

---

## [0.6.0] - 2026-01-20

### Added

- **Prompt Builder Pattern** - Comprehensive builder API for creating high-quality prompts
    - 60+ fluent builder methods following order-independent design
    - Multi-language support (English US, Portuguese BR)
    - Pre-built templates for common tasks:
        - Classification, sentiment analysis, summarization
        - Code generation, review, debugging, translation
        - Data extraction, RAG, question answering
        - Creative writing, storytelling, brainstorming
    - Reasoning strategies:
        - Chain-of-thought, step-back, self-verification
        - Decomposition, tree-of-thoughts, self-consistency
    - Output format controls (JSON, Markdown, CSV, YAML, plain text, etc.)
    - Specialized modes:
        - Code explanation, debugging, review, refactoring
        - SWOT analysis, pros/cons, decision-making
        - Email drafting, technical writing, academic writing
        - Interview, debate, role-play modes
        - Quiz generation, tutorials, ELI5 explanations
    - Static factory methods: `Prompt.builder()`, `Prompt.forTask()`, `Prompt.forExtraction()`, etc.
    - Delimiter styles: XML tags, triple backticks, Markdown headers, etc.

- **MCP (Model Context Protocol) Integration**
    - `McpClient` interface for connecting to MCP servers
    - `StdioMcpClient` for stdio-based MCP servers
    - `StreamableHttpMcpClient` for HTTP/SSE MCP servers
    - `McpRemoteTool` wraps MCP tools as FunctionTools
    - `Agent.Builder.addMcpServer()` for easy integration
    - Automatic tool discovery and schema mapping
    - Full MCP protocol support (resources, prompts, tools)

- **Interactable Interface** - Unified API for all agent patterns
    - Common interface implemented by `Agent`, `RouterAgent`, `ParallelAgents`, `SupervisorAgent`, etc.
    - `interact()` and `interactStream()` methods with consistent signatures
    - Enables seamless composition of different agent patterns
    - `ParallelAgents.relatedResults()` for accessing individual agent results

### Changed

- **Skills Architecture** - Prompt augmentation design for better performance
    - Skills merge instructions directly into the agent's system prompt
    - Skills share the main agent's context window (better context awareness)
    - LLM automatically applies skill expertise when relevant
    - Simplified API: `agent.addSkill(skill)` (no configuration needed)
    - `Skill.toPromptSection()` generates formatted prompt text
    - Benefits: Fewer model calls, better context awareness vs isolated sub-agents
    - Skills support file-based loading via `SKILL.md` and programmatic creation
    - For isolated execution with separate context, use Sub-agents instead

- **Java 25 Upgrade**
    - Updated to Java 25 APIs
    - Migrated to new `StructuredTaskScope.Joiner` API
    - Adopted `ScopedValue.CallableOp` for scoped values
    - Enhanced virtual thread support

### Improved

- **Test Coverage** - Maintained high test coverage with new features
    - Prompt Builder: Comprehensive builder method tests
    - Skills: 77 tests passing (SkillTest, SkillStoreTest, SkillMarkdownParserTest, etc.)
    - MCP integration tests with mock servers

### Documentation

- Added Prompt Builder Pattern section to README
- Added Skills section in README with architecture explanation
- Added MCP integration documentation
- Updated code examples to Java 25
- Clarified Skills vs Sub-agents distinction in README

---

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
