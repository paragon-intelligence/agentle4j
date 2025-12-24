package com.paragon.responses.spec;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The results of a file search tool call. See the <a
 * href="https://platform.openai.com/docs/guides/tools-file-search">file search guide</a> for more
 * information.
 */
public final class FileSearchToolCall extends ToolCall implements Item, ResponseOutput {
  private final @NonNull List<String> queries;
  private final @NonNull FileSearchToolCallStatus status;
  private final @Nullable List<FileSearchToolCallResult> fileSearchToolCallResultList;

  /**
   * @param id The unique ID of the file search tool call.
   * @param queries The queries used to search for files.
   * @param status The status of the file search tool call. One of in_progress, searching,
   *     incomplete or failed,
   * @param fileSearchToolCallResultList The results of the file search tool call.
   */
  public FileSearchToolCall(
      @NonNull String id,
      @NonNull List<String> queries,
      @NonNull FileSearchToolCallStatus status,
      @Nullable List<FileSearchToolCallResult> fileSearchToolCallResultList) {
    super(id);
    this.queries = queries;
    this.status = status;
    this.fileSearchToolCallResultList = fileSearchToolCallResultList;
  }

  @Override
  public @NonNull String toString() {
    var sb = new StringBuilder();

    sb.append("<queries>\n");
    for (var query : queries) {
      sb.append(String.format("<query>\n%s\n</query>\n", query));
    }

    if (fileSearchToolCallResultList != null && !fileSearchToolCallResultList.isEmpty()) {
      for (FileSearchToolCallResult fileSearchToolCallResult : fileSearchToolCallResultList) {
        sb.append(fileSearchToolCallResult.toString());
      }
    }

    return sb.toString();
  }

  @Override
  public @NonNull String id() {
    return id;
  }

  public @NonNull List<String> queries() {
    return queries;
  }

  public @NonNull FileSearchToolCallStatus status() {
    return status;
  }

  public @Nullable List<FileSearchToolCallResult> fileSearchToolCallResultList() {
    return fileSearchToolCallResultList;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (FileSearchToolCall) obj;
    return Objects.equals(this.id, that.id)
        && Objects.equals(this.queries, that.queries)
        && Objects.equals(this.status, that.status)
        && Objects.equals(this.fileSearchToolCallResultList, that.fileSearchToolCallResultList);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, queries, status, fileSearchToolCallResultList);
  }
}
