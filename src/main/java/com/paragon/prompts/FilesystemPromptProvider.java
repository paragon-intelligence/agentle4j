package com.paragon.prompts;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A {@link PromptProvider} that reads prompts from the local filesystem.
 *
 * <p>Prompts are stored as text files, where the prompt ID is treated as a relative path from the
 * configured base directory.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Create provider with base directory
 * PromptProvider provider = FilesystemPromptProvider.create(Path.of("./prompts"));
 *
 * // Load prompt from ./prompts/greeting.txt
 * Prompt greeting = provider.providePrompt("greeting.txt");
 *
 * // Load prompt from subdirectory ./prompts/templates/email.txt
 * Prompt email = provider.providePrompt("templates/email.txt");
 *
 * // Compile the prompt with variables
 * Prompt compiled = greeting.compile(Map.of("name", "World"));
 * }</pre>
 *
 * <p><strong>Note:</strong> The filters parameter is ignored by this implementation as the
 * filesystem does not support versioning or labeling.
 *
 * @author Agentle Framework
 * @since 1.0
 */
public final class FilesystemPromptProvider implements PromptProvider {

  private final Path baseDirectory;

  private FilesystemPromptProvider(@NonNull Path baseDirectory) {
    this.baseDirectory = Objects.requireNonNull(baseDirectory, "baseDirectory must not be null");
  }

  /**
   * Creates a new filesystem prompt provider with the given base directory.
   *
   * @param baseDirectory the base directory where prompts are stored
   * @return a new {@link FilesystemPromptProvider}
   * @throws NullPointerException if baseDirectory is null
   */
  public static @NonNull FilesystemPromptProvider create(@NonNull Path baseDirectory) {
    return new FilesystemPromptProvider(baseDirectory);
  }

  /**
   * Creates a new filesystem prompt provider from a directory path string.
   *
   * @param baseDirectory the base directory path as a string
   * @return a new {@link FilesystemPromptProvider}
   * @throws NullPointerException if baseDirectory is null
   */
  public static @NonNull FilesystemPromptProvider create(@NonNull String baseDirectory) {
    Objects.requireNonNull(baseDirectory, "baseDirectory must not be null");
    return new FilesystemPromptProvider(Path.of(baseDirectory));
  }

  /**
   * Returns the base directory for this provider.
   *
   * @return the base directory path
   */
  public @NonNull Path baseDirectory() {
    return baseDirectory;
  }

  @Override
  public @NonNull Prompt providePrompt(
      @NonNull String promptId, @Nullable Map<String, String> filters) {
    Objects.requireNonNull(promptId, "promptId must not be null");

    if (promptId.isEmpty()) {
      throw new PromptProviderException("Prompt ID cannot be empty", promptId);
    }

    Path promptPath = baseDirectory.resolve(promptId);

    // Security check: prevent path traversal attacks
    if (!promptPath.normalize().startsWith(baseDirectory.normalize())) {
      throw new PromptProviderException("Invalid prompt path: path traversal detected", promptId);
    }

    try {
      String content = Files.readString(promptPath, StandardCharsets.UTF_8);
      return Prompt.of(content);
    } catch (NoSuchFileException e) {
      throw new PromptProviderException("Prompt file not found: " + promptPath, promptId, e);
    } catch (IOException e) {
      throw new PromptProviderException(
          "Failed to read prompt file: " + promptPath, promptId, e, true);
    }
  }

  @Override
  public boolean exists(@NonNull String promptId) {
    Objects.requireNonNull(promptId, "promptId must not be null");

    if (promptId.isEmpty()) {
      return false;
    }

    Path promptPath = baseDirectory.resolve(promptId);

    // Security check: prevent path traversal attacks
    if (!promptPath.normalize().startsWith(baseDirectory.normalize())) {
      return false;
    }

    return Files.exists(promptPath) && Files.isRegularFile(promptPath);
  }

  @Override
  public java.util.Set<String> listPromptIds() {
    if (!Files.exists(baseDirectory)) {
      return java.util.Set.of();
    }

    try (java.util.stream.Stream<Path> paths = Files.walk(baseDirectory)) {
      return paths
          .filter(Files::isRegularFile)
          .map(path -> baseDirectory.relativize(path).toString().replace('\\', '/'))
          .collect(java.util.stream.Collectors.toUnmodifiableSet());
    } catch (IOException e) {
      throw new PromptProviderException(
          "Failed to list prompts in directory: " + baseDirectory, null, e, true);
    }
  }
}
