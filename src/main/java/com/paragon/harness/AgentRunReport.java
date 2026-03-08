package com.paragon.harness;

import com.paragon.agents.AgentResult;
import com.paragon.agents.ToolExecution;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Enriched summary of an {@link AgentResult} annotated with timing, retry counts,
 * tool error statistics, and guardrail trigger counts.
 *
 * <p>Reports can be written to disk via {@link RunReportExporter} and later fed to a
 * meta-agent for harness improvement analysis.
 *
 * @since 1.0
 */
public final class AgentRunReport {

  private final String reportId;
  private final String agentName;
  private final Instant startedAt;
  private final Instant completedAt;
  private final Duration totalDuration;
  private final boolean success;
  private final @Nullable String errorMessage;
  private final int turnsUsed;
  private final int retryCount;
  private final int totalToolCalls;
  private final int failedToolCalls;
  private final Map<String, Integer> toolCallCounts;
  private final Map<String, Integer> toolErrorCounts;
  private final int guardrailTriggerCount;
  private final @Nullable String finalOutput;

  private AgentRunReport(Builder builder) {
    this.reportId = builder.reportId != null ? builder.reportId : UUID.randomUUID().toString();
    this.agentName = Objects.requireNonNull(builder.agentName, "agentName cannot be null");
    this.startedAt = Objects.requireNonNull(builder.startedAt, "startedAt cannot be null");
    this.completedAt = Objects.requireNonNull(builder.completedAt, "completedAt cannot be null");
    this.totalDuration = Duration.between(startedAt, completedAt);
    this.success = builder.success;
    this.errorMessage = builder.errorMessage;
    this.turnsUsed = builder.turnsUsed;
    this.retryCount = builder.retryCount;
    this.totalToolCalls = builder.totalToolCalls;
    this.failedToolCalls = builder.failedToolCalls;
    this.toolCallCounts = Map.copyOf(builder.toolCallCounts);
    this.toolErrorCounts = Map.copyOf(builder.toolErrorCounts);
    this.guardrailTriggerCount = builder.guardrailTriggerCount;
    this.finalOutput = builder.finalOutput;
  }

  /**
   * Creates an AgentRunReport from an AgentResult and timing information.
   *
   * @param agentName the agent name
   * @param result the agent result to report on
   * @param startedAt when the run started
   * @param completedAt when the run completed
   * @param retryCount how many self-correction retries occurred
   * @param guardrailTriggerCount how many guardrails fired
   * @return a new report
   */
  public static @NonNull AgentRunReport from(
      @NonNull String agentName,
      @NonNull AgentResult result,
      @NonNull Instant startedAt,
      @NonNull Instant completedAt,
      int retryCount,
      int guardrailTriggerCount) {
    Objects.requireNonNull(agentName, "agentName cannot be null");
    Objects.requireNonNull(result, "result cannot be null");

    Builder builder = new Builder()
        .agentName(agentName)
        .startedAt(startedAt)
        .completedAt(completedAt)
        .success(result.isSuccess() || result.isHandoff())
        .turnsUsed(result.turnsUsed())
        .retryCount(retryCount)
        .guardrailTriggerCount(guardrailTriggerCount)
        .finalOutput(result.output());

    if (result.error() != null) {
      builder.errorMessage(result.error().getMessage());
    }

    // Compute tool statistics
    List<ToolExecution> executions = result.toolExecutions();
    builder.totalToolCalls(executions.size());
    int failedCount = 0;
    Map<String, Integer> callCounts = new LinkedHashMap<>();
    Map<String, Integer> errorCounts = new LinkedHashMap<>();
    for (ToolExecution exec : executions) {
      callCounts.merge(exec.toolName(), 1, Integer::sum);
      if (!exec.isSuccess()) {
        failedCount++;
        errorCounts.merge(exec.toolName(), 1, Integer::sum);
      }
    }
    builder.failedToolCalls(failedCount)
        .toolCallCounts(callCounts)
        .toolErrorCounts(errorCounts);

    return builder.build();
  }

  // ===== Getters =====

  public @NonNull String reportId() { return reportId; }
  public @NonNull String agentName() { return agentName; }
  public @NonNull Instant startedAt() { return startedAt; }
  public @NonNull Instant completedAt() { return completedAt; }
  public @NonNull Duration totalDuration() { return totalDuration; }
  public boolean isSuccess() { return success; }
  public @Nullable String errorMessage() { return errorMessage; }
  public int turnsUsed() { return turnsUsed; }
  public int retryCount() { return retryCount; }
  public int totalToolCalls() { return totalToolCalls; }
  public int failedToolCalls() { return failedToolCalls; }
  public @NonNull Map<String, Integer> toolCallCounts() { return toolCallCounts; }
  public @NonNull Map<String, Integer> toolErrorCounts() { return toolErrorCounts; }
  public int guardrailTriggerCount() { return guardrailTriggerCount; }
  public @Nullable String finalOutput() { return finalOutput; }

  /** Returns a human-readable summary of this report. */
  public @NonNull String toSummary() {
    return String.format(
        """
        === Agent Run Report [%s] ===
        Agent:       %s
        Status:      %s
        Duration:    %dms
        Turns:       %d
        Retries:     %d
        Tool Calls:  %d (failed: %d)
        Guardrails:  %d triggered
        Error:       %s
        """,
        reportId,
        agentName,
        success ? "SUCCESS" : "FAILURE",
        totalDuration.toMillis(),
        turnsUsed,
        retryCount,
        totalToolCalls,
        failedToolCalls,
        guardrailTriggerCount,
        errorMessage != null ? errorMessage : "none");
  }

  // ===== Builder =====

  public static final class Builder {
    private String reportId;
    private String agentName;
    private Instant startedAt;
    private Instant completedAt;
    private boolean success;
    private String errorMessage;
    private int turnsUsed;
    private int retryCount;
    private int totalToolCalls;
    private int failedToolCalls;
    private Map<String, Integer> toolCallCounts = new LinkedHashMap<>();
    private Map<String, Integer> toolErrorCounts = new LinkedHashMap<>();
    private int guardrailTriggerCount;
    private String finalOutput;

    public Builder reportId(String id) { this.reportId = id; return this; }
    public Builder agentName(String name) { this.agentName = name; return this; }
    public Builder startedAt(Instant t) { this.startedAt = t; return this; }
    public Builder completedAt(Instant t) { this.completedAt = t; return this; }
    public Builder success(boolean s) { this.success = s; return this; }
    public Builder errorMessage(String msg) { this.errorMessage = msg; return this; }
    public Builder turnsUsed(int t) { this.turnsUsed = t; return this; }
    public Builder retryCount(int r) { this.retryCount = r; return this; }
    public Builder totalToolCalls(int t) { this.totalToolCalls = t; return this; }
    public Builder failedToolCalls(int f) { this.failedToolCalls = f; return this; }
    public Builder toolCallCounts(Map<String, Integer> m) { this.toolCallCounts = m; return this; }
    public Builder toolErrorCounts(Map<String, Integer> m) { this.toolErrorCounts = m; return this; }
    public Builder guardrailTriggerCount(int g) { this.guardrailTriggerCount = g; return this; }
    public Builder finalOutput(String o) { this.finalOutput = o; return this; }

    public AgentRunReport build() { return new AgentRunReport(this); }
  }
}
