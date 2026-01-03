package com.paragon.prompts;

import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Provider interface for retrieving prompts from various sources.
 *
 * <p>Implementations may fetch prompts from local files, remote services (e.g., Langfuse), 
 * databases, or any other storage mechanism. This abstraction allows applications to 
 * centralize prompt management and version control.
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Filesystem provider
 * PromptProvider fileProvider = FilesystemPromptProvider.create(Path.of("./prompts"));
 * Prompt prompt = fileProvider.providePrompt("greeting.txt", null);
 *
 * // Langfuse provider with version filter
 * PromptProvider langfuseProvider = LangfusePromptProvider.builder()
 *     .httpClient(okHttpClient)
 *     .publicKey("pk-xxx")
 *     .secretKey("sk-xxx")
 *     .build();
 * Prompt prompt = langfuseProvider.providePrompt("my-prompt", Map.of("version", "2"));
 * }</pre>
 *
 * @author Agentle Framework
 * @since 1.0
 */
public interface PromptProvider {

  /**
   * Retrieves a prompt by its identifier.
   *
   * @param promptId the unique identifier for the prompt (e.g., file path, prompt name)
   * @param filters optional key-value pairs to filter the prompt (e.g., version, label).
   *                Supported filters depend on the implementation.
   * @return the retrieved {@link Prompt}
   * @throws NullPointerException if promptId is null
   * @throws PromptProviderException if the prompt cannot be retrieved
   */
  @NonNull
  Prompt providePrompt(@NonNull String promptId, @Nullable Map<String, String> filters);

  /**
   * Retrieves a prompt by its identifier without filters.
   *
   * @param promptId the unique identifier for the prompt
   * @return the retrieved {@link Prompt}
   * @throws NullPointerException if promptId is null
   * @throws PromptProviderException if the prompt cannot be retrieved
   */
  @NonNull
  default Prompt providePrompt(@NonNull String promptId) {
    return providePrompt(promptId, null);
  }
}
