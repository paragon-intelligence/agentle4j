package com.paragon.harness;

import com.paragon.agents.AgentResult;
import com.paragon.agents.AgenticContext;
import com.paragon.agents.Interactable;
import com.paragon.harness.tools.ArtifactStoreTool;
import com.paragon.harness.tools.ProgressLogTool;
import com.paragon.responses.TraceMetadata;
import com.paragon.responses.spec.FunctionTool;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Cohesive builder that composes all harness features around any {@link Interactable}.
 *
 * <p>A Harness wires together self-correction, lifecycle hooks, progress logging, artifact storage,
 * and run reporting in a single fluent API — without requiring changes to the underlying agent.
 *
 * <p>Example:
 *
 * <pre>{@code
 * ArtifactStore store = FilesystemArtifactStore.create(Path.of("./artifacts"));
 * ProgressLog log = ProgressLog.create();
 *
 * Interactable harnessedAgent = Harness.builder()
 *     .selfCorrection(SelfCorrectionConfig.builder().maxRetries(3).build())
 *     .hooks(HookRegistry.of(new LoggingHook(), new CostTrackingHook()))
 *     .artifactStore(store)
 *     .progressLog(log)
 *     .reportExporter(RunReportExporter.create(Path.of("./reports")))
 *     .wrap(myAgent);
 *
 * AgentResult result = harnessedAgent.interact("Build the feature");
 * }</pre>
 *
 * @since 1.0
 */
public final class Harness {

  private final @Nullable SelfCorrectionConfig selfCorrectionConfig;
  private final @NonNull HookRegistry hookRegistry;
  private final @Nullable ArtifactStore artifactStore;
  private final @Nullable ProgressLog progressLog;
  private final @Nullable RunReportExporter reportExporter;

  private Harness(Builder builder) {
    this.selfCorrectionConfig = builder.selfCorrectionConfig;
    this.hookRegistry = builder.hookRegistry != null ? builder.hookRegistry : HookRegistry.create();
    this.artifactStore = builder.artifactStore;
    this.progressLog = builder.progressLog;
    this.reportExporter = builder.reportExporter;
  }

  /**
   * Returns a new Harness builder.
   *
   * @return a new builder
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Wraps the given agent with all configured harness policies.
   *
   * @param agent the agent to wrap
   * @return an {@link Interactable} that enforces all harness policies
   */
  public @NonNull Interactable wrap(@NonNull Interactable agent) {
    Objects.requireNonNull(agent, "agent cannot be null");

    // Collect extra tools to inject if an artifact store or progress log is configured
    List<FunctionTool<?>> extraTools = new ArrayList<>();
    if (artifactStore != null) {
      extraTools.addAll(ArtifactStoreTool.all(artifactStore));
    }
    if (progressLog != null) {
      extraTools.addAll(ProgressLogTool.all(progressLog));
    }

    // Build the wrapped interactable
    Interactable wrapped =
        selfCorrectionConfig != null
            ? SelfCorrectingInteractable.wrap(agent, selfCorrectionConfig)
            : agent;

    return new HarnessedInteractable(wrapped, hookRegistry, reportExporter, extraTools);
  }

  // ===== Inner Harness Interactable =====

  private static final class HarnessedInteractable implements Interactable {
    private final Interactable delegate;
    private final HookRegistry hooks;
    private final @Nullable RunReportExporter reportExporter;
    private final List<FunctionTool<?>> extraTools;

    private HarnessedInteractable(
        Interactable delegate,
        HookRegistry hooks,
        @Nullable RunReportExporter reportExporter,
        List<FunctionTool<?>> extraTools) {
      this.delegate = delegate;
      this.hooks = hooks;
      this.reportExporter = reportExporter;
      this.extraTools = List.copyOf(extraTools);
    }

    @Override
    public @NonNull String name() {
      return delegate.name() + "[Harnessed]";
    }

    @Override
    public @NonNull AgentResult interact(
        @NonNull AgenticContext context, @Nullable TraceMetadata trace) {
      Instant startedAt = Instant.now();
      hooks.fireBeforeRun(context);

      AgentResult result = delegate.interact(context, trace);

      hooks.fireAfterRun(result, context);

      if (reportExporter != null) {
        AgentRunReport report =
            AgentRunReport.from(delegate.name(), result, startedAt, Instant.now(), 0, 0);
        reportExporter.export(report);
      }

      return result;
    }

    /**
     * Returns a streaming view backed by the delegate's streaming, with harness hooks applied.
     *
     * <p>Fires {@code beforeRun} before the stream starts and {@code afterRun} when {@code
     * onComplete} or {@code onError} fires. Tool-level hooks are covered by the agent's own {@link
     * HookRegistry} (wired in {@link com.paragon.agents.AgentStream}).
     *
     * @return an {@link com.paragon.agents.Interactable.Streaming} backed by the delegate
     */
    public com.paragon.agents.Interactable.@NonNull Streaming asStreaming() {
      return (context, trace) -> {
        hooks.fireBeforeRun(context);
        com.paragon.agents.AgentStream stream = delegate.asStreaming().interact(context, trace);
        return stream
            .onComplete(result -> hooks.fireAfterRun(result, context))
            .onError(
                e -> {
                  AgentResult err = AgentResult.error(e, context, 0);
                  hooks.fireAfterRun(err, context);
                });
      };
    }
  }

  // ===== Builder =====

  /** Builder for composing a Harness. */
  public static final class Builder {
    private @Nullable SelfCorrectionConfig selfCorrectionConfig;
    private @Nullable HookRegistry hookRegistry;
    private @Nullable ArtifactStore artifactStore;
    private @Nullable ProgressLog progressLog;
    private @Nullable RunReportExporter reportExporter;

    private Builder() {}

    /**
     * Enables self-correction with the given configuration.
     *
     * @param config the self-correction config
     * @return this builder
     */
    public @NonNull Builder selfCorrection(@NonNull SelfCorrectionConfig config) {
      this.selfCorrectionConfig = Objects.requireNonNull(config);
      return this;
    }

    /**
     * Enables self-correction with default settings (3 retries, retry on error).
     *
     * @return this builder
     */
    public @NonNull Builder selfCorrection() {
      this.selfCorrectionConfig = SelfCorrectionConfig.builder().build();
      return this;
    }

    /**
     * Sets the hook registry.
     *
     * @param registry the hook registry
     * @return this builder
     */
    public @NonNull Builder hooks(@NonNull HookRegistry registry) {
      this.hookRegistry = Objects.requireNonNull(registry);
      return this;
    }

    /**
     * Adds a single hook to the registry.
     *
     * @param hook the hook to add
     * @return this builder
     */
    public @NonNull Builder addHook(@NonNull AgentHook hook) {
      if (this.hookRegistry == null) this.hookRegistry = HookRegistry.create();
      this.hookRegistry.add(hook);
      return this;
    }

    /**
     * Attaches an artifact store and exposes it as tools to the agent.
     *
     * @param store the artifact store
     * @return this builder
     */
    public @NonNull Builder artifactStore(@NonNull ArtifactStore store) {
      this.artifactStore = Objects.requireNonNull(store);
      return this;
    }

    /**
     * Attaches a progress log and exposes it as tools to the agent.
     *
     * @param log the progress log
     * @return this builder
     */
    public @NonNull Builder progressLog(@NonNull ProgressLog log) {
      this.progressLog = Objects.requireNonNull(log);
      return this;
    }

    /**
     * Attaches a run report exporter so each run is automatically recorded.
     *
     * @param exporter the exporter
     * @return this builder
     */
    public @NonNull Builder reportExporter(@NonNull RunReportExporter exporter) {
      this.reportExporter = Objects.requireNonNull(exporter);
      return this;
    }

    /**
     * Wraps the given agent immediately and returns the harnessed interactable.
     *
     * @param agent the agent to wrap
     * @return the harnessed interactable
     */
    public @NonNull Interactable wrap(@NonNull Interactable agent) {
      return build().wrap(agent);
    }

    /** Builds the Harness (without wrapping an agent yet). */
    public @NonNull Harness build() {
      return new Harness(this);
    }
  }
}
