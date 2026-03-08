package com.paragon.harness;

import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

/**
 * Interface for reading and writing named artifacts (documents, scripts, reports) with versioning.
 *
 * <p>Artifacts are identified by a name and optionally a version. If no version is specified,
 * the latest version is returned. Implementations may persist artifacts to the filesystem, a
 * database, or an object store.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ArtifactStore store = FilesystemArtifactStore.create(Path.of("./artifacts"));
 *
 * // Write a new artifact (version is auto-assigned)
 * store.write("schema.sql", "CREATE TABLE users ...");
 *
 * // Read the latest version
 * Optional<String> schema = store.read("schema.sql");
 *
 * // List all artifact names
 * List<String> names = store.list();
 * }</pre>
 *
 * @since 1.0
 */
public interface ArtifactStore {

  /**
   * Writes an artifact, creating a new version.
   *
   * @param name the artifact name (e.g., "schema.sql", "feature-list.md")
   * @param content the artifact content
   * @return the version identifier assigned to this write
   */
  @NonNull String write(@NonNull String name, @NonNull String content);

  /**
   * Reads the latest version of an artifact.
   *
   * @param name the artifact name
   * @return the content, or empty if the artifact does not exist
   */
  @NonNull Optional<String> read(@NonNull String name);

  /**
   * Reads a specific version of an artifact.
   *
   * @param name the artifact name
   * @param version the version identifier returned from {@link #write}
   * @return the content for that version, or empty if not found
   */
  @NonNull Optional<String> read(@NonNull String name, @NonNull String version);

  /**
   * Lists all artifact names in the store.
   *
   * @return list of artifact names
   */
  @NonNull List<String> list();

  /**
   * Lists all versions for an artifact, oldest first.
   *
   * @param name the artifact name
   * @return list of version identifiers
   */
  @NonNull List<String> versions(@NonNull String name);

  /**
   * Deletes all versions of an artifact.
   *
   * @param name the artifact name
   * @return true if the artifact existed and was deleted
   */
  boolean delete(@NonNull String name);
}
