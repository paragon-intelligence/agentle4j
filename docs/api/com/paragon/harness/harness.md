# :material-code-braces: Harness

> This docs was updated at: 2026-03-09












`com.paragon.harness.Harness` &nbsp;·&nbsp; **Class**

---

Single fluent builder composing all harness policies around any `Interactable`.

A Harness wires together self-correction, lifecycle hooks, progress logging, artifact storage, and run reporting without requiring changes to the underlying agent.

*Since: 1.0*

## Methods

### `builder`

```java
public static @NonNull Harness.Builder builder()
```

Returns a new Harness builder.

## Builder Methods

### `selfCorrection(SelfCorrectionConfig)`

```java
public Builder selfCorrection(@NonNull SelfCorrectionConfig config)
```

Enables self-correction with the given configuration.

---

### `selfCorrection()`

```java
public Builder selfCorrection()
```

Enables self-correction with default settings (3 retries, retry on error).

---

### `hooks(HookRegistry)`

```java
public Builder hooks(@NonNull HookRegistry registry)
```

Sets the hook registry for lifecycle callbacks.

---

### `addHook(AgentHook)`

```java
public Builder addHook(@NonNull AgentHook hook)
```

Adds a single hook to the internal registry.

---

### `artifactStore(ArtifactStore)`

```java
public Builder artifactStore(@NonNull ArtifactStore store)
```

Attaches an artifact store and automatically exposes it as `write_artifact`, `read_artifact`, `list_artifacts` tools to the agent.

---

### `progressLog(ProgressLog)`

```java
public Builder progressLog(@NonNull ProgressLog log)
```

Attaches a progress log and automatically exposes it as `read_progress_log`, `append_progress_log` tools to the agent.

---

### `reportExporter(RunReportExporter)`

```java
public Builder reportExporter(@NonNull RunReportExporter exporter)
```

Attaches a run report exporter so each run is automatically recorded to disk.

---

### `wrap(Interactable)`

```java
public @NonNull Interactable wrap(@NonNull Interactable agent)
```

Wraps the agent immediately and returns the harnessed `Interactable`. Shorthand for `build().wrap(agent)`.

---

### `build()`

```java
public @NonNull Harness build()
```

Builds the `Harness` without wrapping an agent (use `wrap(agent)` separately).

## Usage

```java
ArtifactStore store = FilesystemArtifactStore.create(Path.of("./artifacts"));
ProgressLog log = ProgressLog.create();

Interactable harnessedAgent = Harness.builder()
    .selfCorrection(SelfCorrectionConfig.builder().maxRetries(3).build())
    .addHook(new AgentHook() {
        @Override
        public void beforeRun(AgenticContext ctx) {
            System.out.println("Agent starting...");
        }
        @Override
        public void afterRun(AgentResult result, AgenticContext ctx) {
            System.out.println("Agent done: " + (result.isSuccess() ? "SUCCESS" : "FAILURE"));
        }
    })
    .artifactStore(store)
    .progressLog(log)
    .reportExporter(RunReportExporter.create(Path.of("./reports")))
    .wrap(myAgent);

AgentResult result = harnessedAgent.interact("Build the feature");
```
