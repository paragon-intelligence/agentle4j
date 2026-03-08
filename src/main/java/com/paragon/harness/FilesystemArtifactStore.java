package com.paragon.harness;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;

/**
 * Filesystem-backed implementation of {@link ArtifactStore}.
 *
 * <p>Artifacts are stored as files in the pattern:
 * <pre>{@code baseDir/{name}/{version}.txt}</pre>
 *
 * <p>Versions are epoch-millisecond timestamps. The "latest" version is the file with the
 * highest timestamp. Atomic writes are used to prevent partial file reads.
 *
 * @see ArtifactStore
 * @since 1.0
 */
public final class FilesystemArtifactStore implements ArtifactStore {

  private final Path baseDir;

  private FilesystemArtifactStore(Path baseDir) {
    this.baseDir = Objects.requireNonNull(baseDir, "baseDir cannot be null");
    try {
      Files.createDirectories(baseDir);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot create artifact store directory: " + baseDir, e);
    }
  }

  /**
   * Creates a FilesystemArtifactStore rooted at {@code baseDir}.
   *
   * @param baseDir the directory to store artifacts in
   * @return a new store instance
   */
  public static @NonNull FilesystemArtifactStore create(@NonNull Path baseDir) {
    return new FilesystemArtifactStore(baseDir);
  }

  @Override
  public @NonNull String write(@NonNull String name, @NonNull String content) {
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(content, "content cannot be null");
    validateName(name);

    String version = String.valueOf(Instant.now().toEpochMilli());
    Path artifactDir = artifactDir(name);
    try {
      Files.createDirectories(artifactDir);
      Path versionFile = artifactDir.resolve(version + ".txt");
      Path tempFile = artifactDir.resolve(version + ".tmp");
      Files.writeString(tempFile, content, StandardCharsets.UTF_8,
          StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      Files.move(tempFile, versionFile,
          java.nio.file.StandardCopyOption.REPLACE_EXISTING,
          java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write artifact: " + name, e);
    }
    return version;
  }

  @Override
  public @NonNull Optional<String> read(@NonNull String name) {
    Objects.requireNonNull(name, "name cannot be null");
    List<String> allVersions = versions(name);
    if (allVersions.isEmpty()) return Optional.empty();
    return read(name, allVersions.get(allVersions.size() - 1));
  }

  @Override
  public @NonNull Optional<String> read(@NonNull String name, @NonNull String version) {
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(version, "version cannot be null");
    Path versionFile = artifactDir(name).resolve(version + ".txt");
    if (!Files.exists(versionFile)) return Optional.empty();
    try {
      return Optional.of(Files.readString(versionFile, StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read artifact: " + name + "@" + version, e);
    }
  }

  @Override
  public @NonNull List<String> list() {
    if (!Files.exists(baseDir)) return List.of();
    try (var stream = Files.list(baseDir)) {
      return stream
          .filter(Files::isDirectory)
          .map(p -> p.getFileName().toString())
          .sorted()
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to list artifacts", e);
    }
  }

  @Override
  public @NonNull List<String> versions(@NonNull String name) {
    Objects.requireNonNull(name, "name cannot be null");
    Path artifactDir = artifactDir(name);
    if (!Files.exists(artifactDir)) return List.of();
    try (var stream = Files.list(artifactDir)) {
      return stream
          .filter(p -> p.toString().endsWith(".txt"))
          .map(p -> {
            String filename = p.getFileName().toString();
            return filename.substring(0, filename.length() - 4); // strip .txt
          })
          .sorted(Comparator.comparingLong(v -> {
            try { return Long.parseLong(v); } catch (NumberFormatException e) { return 0L; }
          }))
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to list versions for artifact: " + name, e);
    }
  }

  @Override
  public boolean delete(@NonNull String name) {
    Objects.requireNonNull(name, "name cannot be null");
    Path artifactDir = artifactDir(name);
    if (!Files.exists(artifactDir)) return false;
    try {
      deleteDirectory(artifactDir);
      return true;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to delete artifact: " + name, e);
    }
  }

  // ===== Private Helpers =====

  private Path artifactDir(String name) {
    String safeName = name.replaceAll("[^a-zA-Z0-9_\\-.]", "_");
    return baseDir.resolve(safeName);
  }

  private void validateName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Artifact name cannot be blank");
    }
  }

  private void deleteDirectory(Path dir) throws IOException {
    if (Files.exists(dir)) {
      try (var stream = Files.walk(dir)) {
        List<Path> paths = stream.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        for (Path path : paths) {
          Files.deleteIfExists(path);
        }
      }
    }
  }
}
