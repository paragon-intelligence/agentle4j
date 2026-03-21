package com.paragon.harness;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Append-only log of work items for tracking agent progress across sessions.
 *
 * <p>Each entry records a work item with its status (DONE, FAILED, IN_PROGRESS) and optional notes.
 * The log is append-only to prevent accidental overwrites of completed work.
 *
 * <p>Use {@link FilesystemArtifactStore} or a custom store to persist the log across sessions.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ProgressLog log = ProgressLog.create();
 * log.append("Analyzed requirements", Status.DONE, "Extracted 12 features");
 * log.append("Write database schema", Status.IN_PROGRESS, null);
 *
 * // In another session:
 * List<Entry> pending = log.byStatus(Status.IN_PROGRESS);
 * }</pre>
 *
 * @since 1.0
 */
public final class ProgressLog {

  /** Status of a log entry. */
  public enum Status {
    /** Work item is currently in progress. */
    IN_PROGRESS,
    /** Work item completed successfully. */
    DONE,
    /** Work item failed. */
    FAILED
  }

  /**
   * A single entry in the progress log.
   *
   * @param id unique identifier for this entry
   * @param timestamp when this entry was created
   * @param description what work item this entry describes
   * @param status current status of the work item
   * @param notes optional notes or details
   */
  public record Entry(
      @NonNull String id,
      @NonNull Instant timestamp,
      @NonNull String description,
      @NonNull Status status,
      @Nullable String notes) {

    /** Creates an entry with an auto-generated ID and current timestamp. */
    public static @NonNull Entry of(
        @NonNull String description, @NonNull Status status, @Nullable String notes) {
      return new Entry(UUID.randomUUID().toString(), Instant.now(), description, status, notes);
    }
  }

  private final CopyOnWriteArrayList<Entry> entries;

  private ProgressLog(List<Entry> entries) {
    this.entries = new CopyOnWriteArrayList<>(entries);
  }

  /** Creates an empty progress log. */
  public static @NonNull ProgressLog create() {
    return new ProgressLog(List.of());
  }

  /**
   * Creates a progress log from an existing list of entries (e.g., loaded from storage).
   *
   * @param entries previously persisted entries
   * @return a new ProgressLog pre-populated with those entries
   */
  public static @NonNull ProgressLog from(@NonNull List<Entry> entries) {
    return new ProgressLog(Objects.requireNonNull(entries, "entries cannot be null"));
  }

  /**
   * Appends a new entry to the log.
   *
   * @param description what was done
   * @param status the status of the work item
   * @param notes optional notes
   * @return the entry that was added
   */
  public @NonNull Entry append(
      @NonNull String description, @NonNull Status status, @Nullable String notes) {
    Entry entry = Entry.of(description, status, notes);
    entries.add(entry);
    return entry;
  }

  /**
   * Returns all entries in append order.
   *
   * @return unmodifiable view of all entries
   */
  public @NonNull List<Entry> all() {
    return Collections.unmodifiableList(new ArrayList<>(entries));
  }

  /**
   * Returns entries filtered by status.
   *
   * @param status the status to filter by
   * @return list of entries with the given status
   */
  public @NonNull List<Entry> byStatus(@NonNull Status status) {
    Objects.requireNonNull(status, "status cannot be null");
    return entries.stream().filter(e -> e.status() == status).toList();
  }

  /**
   * Returns the total number of entries.
   *
   * @return entry count
   */
  public int size() {
    return entries.size();
  }

  /**
   * Returns a formatted summary of the log suitable for injection into agent context.
   *
   * @return a multi-line string summarizing all entries
   */
  public @NonNull String toSummary() {
    if (entries.isEmpty()) {
      return "Progress log is empty.";
    }
    StringBuilder sb = new StringBuilder("## Progress Log\n");
    for (Entry e : entries) {
      sb.append(String.format("- [%s] %s: %s", e.status(), e.timestamp(), e.description()));
      if (e.notes() != null && !e.notes().isBlank()) {
        sb.append(" (").append(e.notes()).append(")");
      }
      sb.append("\n");
    }
    return sb.toString();
  }
}
