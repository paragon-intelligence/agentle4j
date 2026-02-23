# Package `com.paragon.telemetry.events`

> This docs was updated at: 2026-02-23

---

## :material-approximately-equal: Interfaces

| Name | Description |
|------|-------------|
| [`TelemetryEvent`](telemetryevent.md) | Base sealed interface for all telemetry events emitted by the Responder |

## :material-database: Records

| Name | Description |
|------|-------------|
| [`AgentFailedEvent`](agentfailedevent.md) | Telemetry event emitted when an agent execution fails |
| [`ResponseCompletedEvent`](responsecompletedevent.md) | Event emitted when a respond() call completes successfully |
| [`ResponseFailedEvent`](responsefailedevent.md) | Event emitted when a respond() call fails with an error |
| [`ResponseStartedEvent`](responsestartedevent.md) | Event emitted when a respond() call begins |
