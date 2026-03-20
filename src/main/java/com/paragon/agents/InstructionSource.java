package com.paragon.agents;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.annotation.JsonDeserialize;
import com.paragon.prompts.FilesystemPromptProvider;
import com.paragon.prompts.Prompt;
import com.paragon.prompts.PromptProvider;
import com.paragon.prompts.PromptProviderRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A sealed interface representing the source of an agent's instructions.
 *
 * <p>Instructions can come from three sources:
 *
 * <ul>
 *   <li>{@link Inline} — raw text embedded directly in the blueprint
 *   <li>{@link FileRef} — a path to a text file on disk
 *   <li>{@link ProviderRef} — a reference to a prompt in a {@link PromptProvider} (e.g., Langfuse)
 * </ul>
 *
 * <h2>JSON/YAML Format</h2>
 *
 * <p><b>Inline (backward-compatible)</b> — a plain string is automatically wrapped:
 *
 * <pre>{@code
 * "instructions": "You are a helpful assistant."
 * }</pre>
 *
 * <p><b>File reference:</b>
 *
 * <pre>{@code
 * "instructions": {
 *   "source": "file",
 *   "path": "./prompts/support-agent.txt"
 * }
 * }</pre>
 *
 * <p><b>Provider reference (e.g., Langfuse):</b>
 *
 * <pre>{@code
 * "instructions": {
 *   "source": "provider",
 *   "providerId": "langfuse",
 *   "promptId": "support-agent-v2",
 *   "filters": { "label": "production" }
 * }
 * }</pre>
 *
 * @see PromptProviderRegistry
 * @since 1.0
 */
@JsonDeserialize(using = InstructionSource.Deserializer.class)
public sealed interface InstructionSource
    permits InstructionSource.Inline, InstructionSource.FileRef, InstructionSource.ProviderRef {

  /**
   * Resolves this instruction source to its actual text content.
   *
   * <p>For {@link Inline}, returns the text directly. For {@link FileRef}, reads the file. For
   * {@link ProviderRef}, fetches from the registered {@link PromptProvider}.
   *
   * @return the resolved instruction text
   * @throws IllegalStateException if resolution fails (file not found, provider not registered,
   *     etc.)
   */
  @NonNull
  String resolve();

  // ===== Variants =====

  /**
   * Instructions embedded directly as text.
   *
   * <p>This is the default and most common variant. When a plain string is provided for
   * {@code instructions} in JSON/YAML, it is automatically wrapped in an {@code Inline} instance.
   *
   * @param text the raw instruction text
   */
  record Inline(@JsonProperty(value = "text", required = true) @NonNull String text)
      implements InstructionSource {

    public Inline {
      Objects.requireNonNull(text, "text must not be null");
    }

    @JsonProperty("source")
    public String source() {
      return "inline";
    }

    @Override
    public @NonNull String resolve() {
      return text;
    }
  }

  /**
   * Instructions loaded from a file on disk.
   *
   * <p>The path is resolved relative to the current working directory at the time
   * {@link #resolve()} is called. Supports absolute paths as well.
   *
   * <pre>{@code
   * // JSON/YAML format:
   * "instructions": {
   *   "source": "file",
   *   "path": "./prompts/support-agent.txt"
   * }
   * }</pre>
   *
   * @param path the file path (relative or absolute)
   */
  record FileRef(@JsonProperty(value = "path", required = true) @NonNull String path)
      implements InstructionSource {

    public FileRef {
      Objects.requireNonNull(path, "path must not be null");
      if (path.isEmpty()) {
        throw new IllegalArgumentException("path must not be empty");
      }
    }

    @JsonProperty("source")
    public String source() {
      return "file";
    }

    @Override
    public @NonNull String resolve() {
      try {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
      } catch (IOException e) {
        throw new IllegalStateException(
            "Failed to read instructions from file: " + path, e);
      }
    }
  }

  /**
   * Instructions fetched from a registered {@link PromptProvider}.
   *
   * <p>The provider is looked up by {@code providerId} in the {@link PromptProviderRegistry}.
   * The registry must be populated at application startup before calling {@link #resolve()}.
   *
   * <pre>{@code
   * // Register at startup:
   * PromptProviderRegistry.register("langfuse",
   *     LangfusePromptProvider.builder()
   *         .httpClient(httpClient)
   *         .publicKey("pk-xxx")
   *         .secretKey("sk-xxx")
   *         .build());
   *
   * // JSON/YAML format:
   * "instructions": {
   *   "source": "provider",
   *   "providerId": "langfuse",
   *   "promptId": "support-agent-v2",
   *   "filters": { "label": "production" }
   * }
   * }</pre>
   *
   * @param providerId the ID of the registered provider (e.g., "langfuse", "local")
   * @param promptId the prompt identifier within the provider
   * @param filters optional key-value filters (e.g., version, label)
   */
  record ProviderRef(
      @JsonProperty(value = "providerId", required = true) @NonNull String providerId,
      @JsonProperty(value = "promptId", required = true) @NonNull String promptId,
      @JsonProperty("filters") @Nullable Map<String, String> filters)
      implements InstructionSource {

    public ProviderRef {
      Objects.requireNonNull(providerId, "providerId must not be null");
      Objects.requireNonNull(promptId, "promptId must not be null");
      if (providerId.isEmpty()) {
        throw new IllegalArgumentException("providerId must not be empty");
      }
      if (promptId.isEmpty()) {
        throw new IllegalArgumentException("promptId must not be empty");
      }
    }

    @JsonProperty("source")
    public String source() {
      return "provider";
    }

    @Override
    public @NonNull String resolve() {
      PromptProvider provider = PromptProviderRegistry.get(providerId);
      if (provider == null) {
        throw new IllegalStateException(
            "PromptProvider '"
                + providerId
                + "' not found in PromptProviderRegistry. "
                + "Register it with PromptProviderRegistry.register(\""
                + providerId
                + "\", provider) before calling toInteractable().");
      }
      Prompt prompt = provider.providePrompt(promptId, filters);
      return prompt.text();
    }
  }

  // ===== Custom Deserializer for backward compatibility =====

  /**
   * Custom deserializer that handles both plain strings and structured objects.
   *
   * <ul>
   *   <li>Plain string {@code "You are a helpful assistant."} → {@link Inline}
   *   <li>Object with {@code "source"} field → polymorphic dispatch to {@link Inline},
   *       {@link FileRef}, or {@link ProviderRef}
   * </ul>
   */
  final class Deserializer extends ValueDeserializer<InstructionSource> {

    @Override
    public InstructionSource deserialize(JsonParser p, DeserializationContext ctxt)
        throws tools.jackson.core.JacksonException {
      JsonNode node = p.readValueAsTree();

      // Plain string → Inline
      if (node.isTextual()) {
        return new Inline(node.asText());
      }

      // Object → check "source" field for polymorphic dispatch
      if (node.isObject()) {
        String source = node.has("source") ? node.get("source").asText() : "inline";

        return switch (source) {
          case "inline" -> {
            String text = node.has("text") ? node.get("text").asText() : "";
            yield new Inline(text);
          }
          case "file" -> {
            String path = node.has("path") ? node.get("path").asText() : "";
            yield new FileRef(path);
          }
          case "provider" -> {
            String providerId = node.has("providerId") ? node.get("providerId").asText() : "";
            String promptId = node.has("promptId") ? node.get("promptId").asText() : "";
            Map<String, String> filters = null;
            if (node.has("filters") && !node.get("filters").isNull()) {
              filters =
                  ctxt.readTreeAsValue(
                      node.get("filters"),
                      ctxt.getTypeFactory()
                          .constructMapType(java.util.HashMap.class, String.class, String.class));
            }
            yield new ProviderRef(providerId, promptId, filters);
          }
          default ->
              throw DatabindException.from(
                  p, "Unknown instruction source type: " + source);
        };
      }

      throw DatabindException.from(
          p,
          "Invalid instructions format: expected a string or an object, got " + node.getNodeType());
    }
  }
}
