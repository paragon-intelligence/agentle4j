# Package `com.paragon.harness`

> This docs was updated at: 2026-03-21

---

## :material-code-braces: Classs

| Name | Description |
|------|-------------|
| [`AgentRunReport`](agentrunreport.md) | Enriched summary of an `AgentResult` annotated with timing, retry counts, tool error statistics, … |
| [`FilesystemArtifactStore`](filesystemartifactstore.md) | Filesystem-backed implementation of `ArtifactStore` |
| [`Harness`](harness.md) | Cohesive builder that composes all harness features around any `Interactable` |
| [`HookRegistry`](hookregistry.md) | Ordered registry of `AgentHook` instances executed around agent lifecycle events |
| [`ProgressLog`](progresslog.md) | Append-only log of work items for tracking agent progress across sessions |
| [`RunReportExporter`](runreportexporter.md) | Writes `AgentRunReport` instances to a filesystem directory for later analysis |
| [`SelfCorrectingInteractable`](selfcorrectinginteractable.md) | Decorator that wraps any `Interactable` with a self-correction loop |
| [`SelfCorrectionConfig`](selfcorrectionconfig.md) | Configuration for the self-correction loop in `SelfCorrectingInteractable` |

## :material-approximately-equal: Interfaces

| Name | Description |
|------|-------------|
| [`AgentHook`](agenthook.md) | Lifecycle hook that intercepts agent and tool execution events |
| [`ArtifactStore`](artifactstore.md) | Interface for reading and writing named artifacts (documents, scripts, reports) with versioning |

## :material-database: Records

| Name | Description |
|------|-------------|
| [`VerificationResult`](verificationresult.md) | The result of running a verification command (test suite, linter, etc |
