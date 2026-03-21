# :material-code-braces: Harness

> This docs was updated at: 2026-03-21

`com.paragon.harness.Harness` &nbsp;·&nbsp; **Class**

---

Cohesive builder that composes all harness features around any `Interactable`.

A Harness wires together self-correction, lifecycle hooks, progress logging,
artifact storage, and run reporting in a single fluent API — without requiring changes
to the underlying agent.

Example:

```java
ArtifactStore store = FilesystemArtifactStore.create(Path.of("./artifacts"));
ProgressLog log = ProgressLog.create();
Interactable harnessedAgent = Harness.builder()
    .selfCorrection(SelfCorrectionConfig.builder().maxRetries(3).build())
    .hooks(HookRegistry.of(new LoggingHook(), new CostTrackingHook()))
    .artifactStore(store)
    .progressLog(log)
    .reportExporter(RunReportExporter.create(Path.of("./reports")))
    .wrap(myAgent);
AgentResult result = harnessedAgent.interact("Build the feature");
```

*Since: 1.0*

## Methods

### `builder`

```java
public static @NonNull Builder builder()
```

Returns a new Harness builder.

**Returns**

a new builder

---

### `wrap`

```java
public @NonNull Interactable wrap(@NonNull Interactable agent)
```

Wraps the given agent with all configured harness policies.

**Parameters**

| Name | Description |
|------|-------------|
| `agent` | the agent to wrap |

**Returns**

an `Interactable` that enforces all harness policies

---

### `asStreaming`

```java
public com.paragon.agents.Interactable.@NonNull Streaming asStreaming()
```

Returns a streaming view backed by the delegate's streaming, with harness hooks applied.

Fires `beforeRun` before the stream starts and `afterRun` when
`onComplete` or `onError` fires. Tool-level hooks are covered by the agent's
own `HookRegistry` (wired in `com.paragon.agents.AgentStream`).

**Returns**

an `com.paragon.agents.Interactable.Streaming` backed by the delegate

---

### `selfCorrection`

```java
public @NonNull Builder selfCorrection(@NonNull SelfCorrectionConfig config)
```

Enables self-correction with the given configuration.

**Parameters**

| Name | Description |
|------|-------------|
| `config` | the self-correction config |

**Returns**

this builder

---

### `selfCorrection`

```java
public @NonNull Builder selfCorrection()
```

Enables self-correction with default settings (3 retries, retry on error).

**Returns**

this builder

---

### `hooks`

```java
public @NonNull Builder hooks(@NonNull HookRegistry registry)
```

Sets the hook registry.

**Parameters**

| Name | Description |
|------|-------------|
| `registry` | the hook registry |

**Returns**

this builder

---

### `addHook`

```java
public @NonNull Builder addHook(@NonNull AgentHook hook)
```

Adds a single hook to the registry.

**Parameters**

| Name | Description |
|------|-------------|
| `hook` | the hook to add |

**Returns**

this builder

---

### `artifactStore`

```java
public @NonNull Builder artifactStore(@NonNull ArtifactStore store)
```

Attaches an artifact store and exposes it as tools to the agent.

**Parameters**

| Name | Description |
|------|-------------|
| `store` | the artifact store |

**Returns**

this builder

---

### `progressLog`

```java
public @NonNull Builder progressLog(@NonNull ProgressLog log)
```

Attaches a progress log and exposes it as tools to the agent.

**Parameters**

| Name | Description |
|------|-------------|
| `log` | the progress log |

**Returns**

this builder

---

### `reportExporter`

```java
public @NonNull Builder reportExporter(@NonNull RunReportExporter exporter)
```

Attaches a run report exporter so each run is automatically recorded.

**Parameters**

| Name | Description |
|------|-------------|
| `exporter` | the exporter |

**Returns**

this builder

---

### `wrap`

```java
public @NonNull Interactable wrap(@NonNull Interactable agent)
```

Wraps the given agent immediately and returns the harnessed interactable.

**Parameters**

| Name | Description |
|------|-------------|
| `agent` | the agent to wrap |

**Returns**

the harnessed interactable

---

### `build`

```java
public @NonNull Harness build()
```

Builds the Harness (without wrapping an agent yet).
