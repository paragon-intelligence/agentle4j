package com.paragon.harness.tools;

import com.paragon.harness.ProgressLog;
import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Exposes a {@link ProgressLog} as two {@link FunctionTool}s: one for reading and one for
 * appending.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * ProgressLog log = ProgressLog.create();
 *
 * Agent agent = Agent.builder()
 *     .addTools(ProgressLogTool.all(log).toArray(new FunctionTool[0]))
 *     .build();
 * }</pre>
 *
 * @since 1.0
 */
public final class ProgressLogTool {

  private ProgressLogTool() {}

  /**
   * Creates all progress log tools (read + append).
   *
   * @param log the progress log to expose
   * @return list of tools
   */
  public static @NonNull List<FunctionTool<?>> all(@NonNull ProgressLog log) {
    Objects.requireNonNull(log, "log cannot be null");
    return List.of(new ReadProgressLogTool(log), new AppendProgressLogTool(log));
  }

  // ===== Request Records =====

  public record ReadProgressLogRequest(
      @Nullable String statusFilter // "DONE", "FAILED", "IN_PROGRESS", or null for all
      ) {}

  public record AppendProgressLogRequest(
      @NonNull String description,
      @NonNull String status, // "DONE", "FAILED", "IN_PROGRESS"
      @Nullable String notes) {}

  // ===== Tool Implementations =====

  @FunctionMetadata(
      name = "read_progress_log",
      description =
          "Read the current progress log. Optionally filter by status: DONE, FAILED, or"
              + " IN_PROGRESS.")
  public static final class ReadProgressLogTool extends FunctionTool<ReadProgressLogRequest> {
    private final ProgressLog log;

    public ReadProgressLogTool(@NonNull ProgressLog log) {
      this.log = Objects.requireNonNull(log);
    }

    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable ReadProgressLogRequest params) {
      if (params == null || params.statusFilter() == null) {
        return FunctionToolCallOutput.success(log.toSummary());
      }
      try {
        ProgressLog.Status status = ProgressLog.Status.valueOf(params.statusFilter().toUpperCase());
        List<ProgressLog.Entry> entries = log.byStatus(status);
        if (entries.isEmpty()) {
          return FunctionToolCallOutput.success("No entries with status: " + status);
        }
        StringBuilder sb = new StringBuilder("Entries with status " + status + ":\n");
        for (ProgressLog.Entry e : entries) {
          sb.append(String.format("- [%s] %s", e.timestamp(), e.description()));
          if (e.notes() != null) sb.append(" (").append(e.notes()).append(")");
          sb.append("\n");
        }
        return FunctionToolCallOutput.success(sb.toString());
      } catch (IllegalArgumentException e) {
        return FunctionToolCallOutput.error(
            "Invalid status filter. Use one of: DONE, FAILED, IN_PROGRESS");
      }
    }
  }

  @FunctionMetadata(
      name = "append_progress_log",
      description =
          "Append a new entry to the progress log. Status must be one of: DONE, FAILED,"
              + " IN_PROGRESS.")
  public static final class AppendProgressLogTool extends FunctionTool<AppendProgressLogRequest> {
    private final ProgressLog log;

    public AppendProgressLogTool(@NonNull ProgressLog log) {
      this.log = Objects.requireNonNull(log);
    }

    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable AppendProgressLogRequest params) {
      if (params == null) {
        return FunctionToolCallOutput.error("No parameters provided");
      }
      try {
        ProgressLog.Status status = ProgressLog.Status.valueOf(params.status().toUpperCase());
        ProgressLog.Entry entry = log.append(params.description(), status, params.notes());
        return FunctionToolCallOutput.success("Progress log entry added with id: " + entry.id());
      } catch (IllegalArgumentException e) {
        return FunctionToolCallOutput.error(
            "Invalid status '" + params.status() + "'. Use one of: DONE, FAILED, IN_PROGRESS");
      }
    }
  }
}
