package com.paragon.harness.tools;

import com.paragon.harness.ArtifactStore;
import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Exposes an {@link ArtifactStore} as {@link FunctionTool}s for read/write/list operations.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * ArtifactStore store = FilesystemArtifactStore.create(Path.of("./artifacts"));
 *
 * Agent agent = Agent.builder()
 *     .addTools(ArtifactStoreTool.all(store).toArray(new FunctionTool[0]))
 *     .build();
 * }</pre>
 *
 * @since 1.0
 */
public final class ArtifactStoreTool {

  private ArtifactStoreTool() {}

  /**
   * Creates all artifact store tools (read, write, list).
   *
   * @param store the artifact store to expose
   * @return list of tools
   */
  public static @NonNull List<FunctionTool<?>> all(@NonNull ArtifactStore store) {
    Objects.requireNonNull(store, "store cannot be null");
    return List.of(
        new WriteArtifactTool(store),
        new ReadArtifactTool(store),
        new ListArtifactsTool(store));
  }

  // ===== Request Records =====

  public record WriteArtifactRequest(@NonNull String name, @NonNull String content) {}

  public record ReadArtifactRequest(@NonNull String name, @Nullable String version) {}

  public record ListArtifactsRequest() {}

  // ===== Tool Implementations =====

  @FunctionMetadata(
      name = "write_artifact",
      description =
          "Write or update a named artifact (document, script, report). "
              + "A new version is created automatically. Returns the version ID.")
  public static final class WriteArtifactTool extends FunctionTool<WriteArtifactRequest> {
    private final ArtifactStore store;

    public WriteArtifactTool(@NonNull ArtifactStore store) {
      this.store = Objects.requireNonNull(store);
    }

    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable WriteArtifactRequest params) {
      if (params == null) return FunctionToolCallOutput.error("No parameters provided");
      try {
        String version = store.write(params.name(), params.content());
        return FunctionToolCallOutput.success(
            "Artifact '" + params.name() + "' written successfully. Version: " + version);
      } catch (Exception e) {
        return FunctionToolCallOutput.error("Failed to write artifact: " + e.getMessage());
      }
    }
  }

  @FunctionMetadata(
      name = "read_artifact",
      description =
          "Read a named artifact. Optionally specify a version; defaults to the latest version.")
  public static final class ReadArtifactTool extends FunctionTool<ReadArtifactRequest> {
    private final ArtifactStore store;

    public ReadArtifactTool(@NonNull ArtifactStore store) {
      this.store = Objects.requireNonNull(store);
    }

    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable ReadArtifactRequest params) {
      if (params == null) return FunctionToolCallOutput.error("No parameters provided");
      try {
        Optional<String> content = params.version() != null
            ? store.read(params.name(), params.version())
            : store.read(params.name());
        return content.isPresent()
            ? FunctionToolCallOutput.success(content.get())
            : FunctionToolCallOutput.error("Artifact not found: " + params.name());
      } catch (Exception e) {
        return FunctionToolCallOutput.error("Failed to read artifact: " + e.getMessage());
      }
    }
  }

  @FunctionMetadata(
      name = "list_artifacts",
      description = "List all artifact names in the store.")
  public static final class ListArtifactsTool extends FunctionTool<ListArtifactsRequest> {
    private final ArtifactStore store;

    public ListArtifactsTool(@NonNull ArtifactStore store) {
      this.store = Objects.requireNonNull(store);
    }

    @Override
    public @Nullable FunctionToolCallOutput call(@Nullable ListArtifactsRequest params) {
      List<String> names = store.list();
      if (names.isEmpty()) {
        return FunctionToolCallOutput.success("No artifacts found in the store.");
      }
      return FunctionToolCallOutput.success(
          "Artifacts (" + names.size() + "):\n" + String.join("\n", names));
    }
  }
}
