# Package `com.paragon.harness`

> This docs was updated at: 2026-03-09









Harness engineering primitives for building reliable, long-horizon agents.

Harness engineering is the discipline of building constraints, state management, verification loops, and feedback systems *around* LLMs. The three pillars are:

1. **Constrain** — limit what an agent can do, mechanically enforcing architectural rules
2. **Verify** — check the agent's work via tests, linters, structured output validation
3. **Correct** — close the feedback loop so agents learn from failures within a session

---

## :material-code-braces: Classes

| Name | Description |
|------|-------------|
| [`AgentRunReport`](agentrunreport.md) | Enriched summary of an `AgentResult` annotated with timing, retry counts, and tool error statistics |
| [`FilesystemArtifactStore`](filesystemartifactstore.md) | Filesystem-backed implementation of `ArtifactStore` with versioning |
| [`HookRegistry`](hookregistry.md) | Ordered registry of `AgentHook` instances dispatched around agent lifecycle events |
| [`Harness`](harness.md) | Single fluent builder composing all harness policies around any `Interactable` |
| [`ProgressLog`](progresslog.md) | Append-only log of work items with DONE/FAILED/IN_PROGRESS status |
| [`RunReportExporter`](runreportexporter.md) | Writes `AgentRunReport` instances to a filesystem directory for later analysis |
| [`SelfCorrectingInteractable`](selfcorrectinginteractable.md) | Decorator that wraps any `Interactable` with a self-correction retry loop |
| [`SelfCorrectionConfig`](selfcorrectionconfig.md) | Configuration for the self-correction loop |
| [`VerificationResult`](verificationresult.md) | Result of running a verification command (test suite, linter, etc.) |

## :material-approximately-equal: Interfaces

| Name | Description |
|------|-------------|
| [`AgentHook`](agenthook.md) | Lifecycle hook that intercepts agent and tool execution events |
| [`ArtifactStore`](artifactstore.md) | Interface for reading and writing named artifacts with versioning |

## Sub-packages

| Package | Description |
|---------|-------------|
| [`com.paragon.harness.tools`](tools/index.md) | `FunctionTool` wrappers exposing harness primitives to agents |

## Quick Start

```java
// Wire everything together with the Harness builder
ArtifactStore store = FilesystemArtifactStore.create(Path.of("./artifacts"));
ProgressLog log = ProgressLog.create();

Interactable agent = Agent.builder()
    .name("CodeWriter")
    .model("openai/gpt-4o")
    .instructions("You write and verify Java code.")
    .responder(responder)
    .addTool(ShellVerificationTool.builder()
        .name("run_tests")
        .command("mvn", "test", "-q")
        .build())
    .build();

Interactable harnessedAgent = Harness.builder()
    .selfCorrection(SelfCorrectionConfig.builder().maxRetries(3).build())
    .addHook(new LoggingHook())
    .artifactStore(store)
    .progressLog(log)
    .reportExporter(RunReportExporter.create(Path.of("./reports")))
    .wrap(agent);

AgentResult result = harnessedAgent.interact("Write a binary search implementation");
```
