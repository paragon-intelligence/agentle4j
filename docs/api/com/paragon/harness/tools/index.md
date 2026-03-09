# Package `com.paragon.harness.tools`

> This docs was updated at: 2026-03-09








`FunctionTool` wrappers that expose harness primitives — progress logs, artifact stores, shell verification, and failure analysis — as native agent tools.

---

## :material-code-braces: Classes

| Name | Description |
|------|-------------|
| [`ArtifactStoreTool`](artifactstoretool.md) | Exposes an `ArtifactStore` as `write_artifact`, `read_artifact`, and `list_artifacts` tools |
| [`FailureAnalysisTool`](failureanalysistool.md) | Reads a batch of `AgentRunReport` files and returns a structured failure summary |
| [`ProgressLogTool`](progresslogtool.md) | Exposes a `ProgressLog` as `read_progress_log` and `append_progress_log` tools |
| [`ShellVerificationTool`](shellverificationtool.md) | Runs a fixed shell command (test suite, linter) and returns the result — command is injection-safe |
