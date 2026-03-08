package com.paragon.agents;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.paragon.agents.context.ContextManagementConfig;
import com.paragon.agents.context.SlidingWindowStrategy;
import com.paragon.agents.context.SummarizationStrategy;
import com.paragon.agents.context.TokenCounter;
import com.paragon.http.RetryPolicy;
import com.paragon.responses.Responder;
import com.paragon.responses.TraceMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.ResponsesAPIProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import okhttp3.HttpUrl;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A sealed interface representing the serializable blueprint of any {@link Interactable}.
 *
 * <p>Blueprints capture all declarative configuration of an agent in a format that is fully
 * Jackson-serializable. Runtime dependencies (HTTP clients, connection pools, etc.) are
 * reconstructed automatically during deserialization — API keys are resolved from environment
 * variables.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Serialize any Interactable to JSON
 * InteractableBlueprint blueprint = agent.toBlueprint();
 * String json = objectMapper.writeValueAsString(blueprint);
 *
 * // Deserialize and reconstruct (zero external dependencies!)
 * InteractableBlueprint restored = objectMapper.readValue(json, InteractableBlueprint.class);
 * Interactable agent = restored.toInteractable();
 * }</pre>
 *
 * @see Interactable#toBlueprint()
 * @since 1.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = InteractableBlueprint.AgentBlueprint.class, name = "agent"),
  @JsonSubTypes.Type(value = InteractableBlueprint.AgentNetworkBlueprint.class, name = "network"),
  @JsonSubTypes.Type(
      value = InteractableBlueprint.SupervisorAgentBlueprint.class,
      name = "supervisor"),
  @JsonSubTypes.Type(
      value = InteractableBlueprint.ParallelAgentsBlueprint.class,
      name = "parallel"),
  @JsonSubTypes.Type(value = InteractableBlueprint.RouterAgentBlueprint.class, name = "router"),
  @JsonSubTypes.Type(
      value = InteractableBlueprint.HierarchicalAgentsBlueprint.class,
      name = "hierarchical")
})
public sealed interface InteractableBlueprint
    permits InteractableBlueprint.AgentBlueprint,
        InteractableBlueprint.AgentNetworkBlueprint,
        InteractableBlueprint.SupervisorAgentBlueprint,
        InteractableBlueprint.ParallelAgentsBlueprint,
        InteractableBlueprint.RouterAgentBlueprint,
        InteractableBlueprint.HierarchicalAgentsBlueprint {

  /** Returns the name of the interactable this blueprint describes. */
  String name();

  /**
   * Reconstructs a fully functional {@link Interactable} from this blueprint.
   *
   * <p>All runtime dependencies (Responder, ObjectMapper, etc.) are created automatically. API keys
   * are resolved from environment variables based on the configured provider.
   *
   * @return a fully functional Interactable
   * @throws IllegalStateException if required environment variables are not set
   */
  Interactable toInteractable();

  /**
   * Serializes this blueprint to a JSON string using the provided {@link ObjectMapper}.
   *
   * @param mapper the ObjectMapper to use for serialization
   * @return a JSON string representation of this blueprint
   * @throws java.io.UncheckedIOException if serialization fails
   */
  default @NonNull String toJson(@NonNull ObjectMapper mapper) {
    try {
      return mapper.writeValueAsString(this);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  /**
   * Serializes this blueprint to a JSON string using a default {@link ObjectMapper}.
   *
   * @return a JSON string representation of this blueprint
   * @throws java.io.UncheckedIOException if serialization fails
   */
  default @NonNull String toJson() {
    return toJson(new ObjectMapper());
  }

  /**
   * Serializes this blueprint to a YAML string using the provided {@link YAMLMapper}.
   *
   * @param mapper the YAMLMapper to use for serialization
   * @return a YAML string representation of this blueprint
   * @throws java.io.UncheckedIOException if serialization fails
   */
  default @NonNull String toYaml(@NonNull YAMLMapper mapper) {
    try {
      return mapper.writeValueAsString(this);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  /**
   * Serializes this blueprint to a YAML string using a default {@link YAMLMapper}.
   *
   * @return a YAML string representation of this blueprint
   * @throws java.io.UncheckedIOException if serialization fails
   */
  default @NonNull String toYaml() {
    return toYaml(new YAMLMapper());
  }

  /**
   * Deserializes an {@link InteractableBlueprint} from a YAML string.
   *
   * @param yaml the YAML string
   * @return the deserialized blueprint
   * @throws java.io.UncheckedIOException if deserialization fails
   */
  static @NonNull InteractableBlueprint fromYaml(@NonNull String yaml) {
    try {
      return new YAMLMapper()
          .configure(
              com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
              false)
          .readValue(yaml, InteractableBlueprint.class);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  /**
   * Deserializes an {@link InteractableBlueprint} from a JSON string.
   *
   * @param json the JSON string
   * @return the deserialized blueprint
   * @throws java.io.UncheckedIOException if deserialization fails
   */
  static @NonNull InteractableBlueprint fromJson(@NonNull String json) {
    try {
      return new ObjectMapper()
          .configure(
              com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
              false)
          .readValue(json, InteractableBlueprint.class);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  // ===== Helper Records (not part of sealed hierarchy) =====

  /**
   * Serializable descriptor for a {@link Responder} configuration.
   *
   * <p>On reconstruction, the API key is resolved automatically from environment variables based on
   * the provider (e.g., {@code OPENROUTER_API_KEY} for OpenRouter).
   */
  record ResponderBlueprint(
      @JsonProperty("provider") @Nullable String provider,
      @JsonProperty("baseUrl") @Nullable String baseUrl,
      @JsonProperty("apiKeyEnvVar") @Nullable String apiKeyEnvVar,
      @JsonProperty("retryPolicy") @Nullable RetryPolicyBlueprint retryPolicy,
      @JsonProperty("traceMetadata") @Nullable TraceMetadata traceMetadata) {

    /** Extracts a blueprint from an existing {@link Responder}. */
    public static ResponderBlueprint from(@NonNull Responder responder) {
      String providerName = responder.provider() != null ? responder.provider().name() : null;
      String envVar = responder.provider() != null ? responder.provider().getEnvKey() : null;
      String url = responder.provider() == null ? responder.baseUrlString() : null;
      return new ResponderBlueprint(
          providerName,
          url,
          envVar,
          RetryPolicyBlueprint.from(responder.retryPolicy()),
          responder.traceMetadata());
    }

    /** Reconstructs a {@link Responder} from this blueprint. */
    public Responder toResponder() {
      var builder = Responder.builder();
      if (provider != null) {
        builder.provider(ResponsesAPIProvider.valueOf(provider));
      } else if (baseUrl != null) {
        builder.baseUrl(
            Objects.requireNonNull(HttpUrl.parse(baseUrl), "Invalid baseUrl: " + baseUrl));
      }
      if (apiKeyEnvVar != null) {
        String key = System.getenv(apiKeyEnvVar);
        if (key != null) {
          builder.apiKey(key);
        }
      }
      if (retryPolicy != null) {
        builder.retryPolicy(retryPolicy.toRetryPolicy());
      }
      if (traceMetadata != null) {
        builder.traceMetadata(traceMetadata);
      }
      return builder.build();
    }
  }

  /** Serializable representation of a {@link RetryPolicy}. Uses millis for Duration fields. */
  record RetryPolicyBlueprint(
      @JsonProperty("maxRetries") int maxRetries,
      @JsonProperty("initialDelayMs") long initialDelayMs,
      @JsonProperty("maxDelayMs") long maxDelayMs,
      @JsonProperty("multiplier") double multiplier,
      @JsonProperty("retryableStatusCodes") Set<Integer> retryableStatusCodes) {

    public static RetryPolicyBlueprint from(@NonNull RetryPolicy rp) {
      return new RetryPolicyBlueprint(
          rp.maxRetries(),
          rp.initialDelay().toMillis(),
          rp.maxDelay().toMillis(),
          rp.multiplier(),
          rp.retryableStatusCodes());
    }

    public RetryPolicy toRetryPolicy() {
      return RetryPolicy.builder()
          .maxRetries(maxRetries)
          .initialDelay(Duration.ofMillis(initialDelayMs))
          .maxDelay(Duration.ofMillis(maxDelayMs))
          .multiplier(multiplier)
          .retryableStatusCodes(retryableStatusCodes)
          .build();
    }
  }

  /** Reference to a guardrail — either by class name (reflection) or registry ID. */
  record GuardrailReference(
      @JsonProperty("className") @Nullable String className,
      @JsonProperty("registryId") @Nullable String registryId) {

    @SuppressWarnings("unchecked")
    public InputGuardrail toInputGuardrail() {
      if (registryId != null) {
        InputGuardrail g = GuardrailRegistry.getInput(registryId);
        if (g != null) return g;
        throw new IllegalStateException(
            "InputGuardrail with registry ID '"
                + registryId
                + "' not found. "
                + "Register it with InputGuardrail.named(\""
                + registryId
                + "\", impl) before deserialization.");
      }
      if (className != null) {
        try {
          return (InputGuardrail) Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
          throw new IllegalStateException("Cannot instantiate InputGuardrail: " + className, e);
        }
      }
      throw new IllegalStateException(
          "GuardrailReference must have either className or registryId");
    }

    @SuppressWarnings("unchecked")
    public OutputGuardrail toOutputGuardrail() {
      if (registryId != null) {
        OutputGuardrail g = GuardrailRegistry.getOutput(registryId);
        if (g != null) return g;
        throw new IllegalStateException(
            "OutputGuardrail with registry ID '"
                + registryId
                + "' not found. "
                + "Register it with OutputGuardrail.named(\""
                + registryId
                + "\", impl) before deserialization.");
      }
      if (className != null) {
        try {
          return (OutputGuardrail) Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
          throw new IllegalStateException("Cannot instantiate OutputGuardrail: " + className, e);
        }
      }
      throw new IllegalStateException(
          "GuardrailReference must have either className or registryId");
    }

    /** Creates a reference from a live guardrail instance. */
    static GuardrailReference fromInput(InputGuardrail g) {
      if (g instanceof NamedInputGuardrail named) {
        return new GuardrailReference(null, named.id());
      }
      Class<?> clazz = g.getClass();
      if (clazz.isAnonymousClass() || clazz.isSynthetic()) {
        throw new IllegalStateException(
            "Cannot serialize anonymous/lambda InputGuardrail. "
                + "Use InputGuardrail.named(\"id\", impl) to make it serializable.");
      }
      return new GuardrailReference(clazz.getName(), null);
    }

    /** Creates a reference from a live guardrail instance. */
    static GuardrailReference fromOutput(OutputGuardrail g) {
      if (g instanceof NamedOutputGuardrail named) {
        return new GuardrailReference(null, named.id());
      }
      Class<?> clazz = g.getClass();
      if (clazz.isAnonymousClass() || clazz.isSynthetic()) {
        throw new IllegalStateException(
            "Cannot serialize anonymous/lambda OutputGuardrail. "
                + "Use OutputGuardrail.named(\"id\", impl) to make it serializable.");
      }
      return new GuardrailReference(clazz.getName(), null);
    }
  }

  /** Serializable descriptor for a {@link Handoff}. */
  record HandoffDescriptor(
      @JsonProperty("name") @NonNull String name,
      @JsonProperty("description") @NonNull String description,
      @JsonProperty("target")
          @JsonDeserialize(using = BlueprintDeserializer.class)
          @NonNull
          InteractableBlueprint target) {

    public static HandoffDescriptor from(@NonNull Handoff handoff) {
      return new HandoffDescriptor(
          handoff.name(), handoff.description(), handoff.targetAgent().toBlueprint());
    }
  }

  /** Serializable descriptor for a {@link SupervisorAgent.Worker}. */
  record WorkerBlueprint(
      @JsonProperty("worker")
          @JsonDeserialize(using = BlueprintDeserializer.class)
          @NonNull
          InteractableBlueprint worker,
      @JsonProperty("description") @NonNull String description) {}

  /** Serializable descriptor for a {@link RouterAgent.Route}. */
  record RouteBlueprint(
      @JsonProperty("target")
          @JsonDeserialize(using = BlueprintDeserializer.class)
          @NonNull
          InteractableBlueprint target,
      @JsonProperty("description") @NonNull String description) {}

  /** Serializable descriptor for a {@link HierarchicalAgents.Department}. */
  record DepartmentBlueprint(
      @JsonProperty("manager") @NonNull AgentBlueprint manager,
      @JsonProperty("workers")
          @JsonDeserialize(contentUsing = BlueprintDeserializer.class)
          @NonNull
          List<InteractableBlueprint> workers) {}

  /** Serializable descriptor for {@link ContextManagementConfig}. */
  record ContextBlueprint(
      @JsonProperty("strategyType") @NonNull String strategyType,
      @JsonProperty("preserveDeveloperMessages") @Nullable Boolean preserveDeveloperMessages,
      @JsonProperty("summarizationModel") @Nullable String summarizationModel,
      @JsonProperty("keepRecentMessages") @Nullable Integer keepRecentMessages,
      @JsonProperty("summarizationPrompt") @Nullable String summarizationPrompt,
      @JsonProperty("maxTokens") int maxTokens,
      @JsonProperty("tokenCounterClassName") @Nullable String tokenCounterClassName) {

    ContextManagementConfig toConfig(@Nullable Responder responder) {
      var builder = ContextManagementConfig.builder().maxTokens(maxTokens);

      // Token counter
      if (tokenCounterClassName != null) {
        try {
          builder.tokenCounter(
              (TokenCounter)
                  Class.forName(tokenCounterClassName).getDeclaredConstructor().newInstance());
        } catch (Exception e) {
          // Fall back to default
        }
      }

      // Strategy
      if ("summarization".equals(strategyType)) {
        var summBuilder = SummarizationStrategy.builder();
        if (responder != null) summBuilder.responder(responder);
        if (summarizationModel != null) summBuilder.model(summarizationModel);
        if (keepRecentMessages != null) summBuilder.keepRecentMessages(keepRecentMessages);
        if (summarizationPrompt != null) summBuilder.summarizationPrompt(summarizationPrompt);
        builder.strategy(summBuilder.build());
      } else {
        boolean preserve = preserveDeveloperMessages != null && preserveDeveloperMessages;
        builder.strategy(new SlidingWindowStrategy(preserve));
      }

      return builder.build();
    }
  }

  // ===== Sealed Hierarchy: Blueprint Variants =====

  /** Blueprint for an {@link Agent}. */
  record AgentBlueprint(
      @JsonProperty("name") @NonNull String name,
      @JsonProperty("model") @NonNull String model,
      @JsonProperty("instructions") @NonNull InstructionSource instructions,
      @JsonProperty("maxTurns") int maxTurns,
      @JsonProperty("temperature") @Nullable Double temperature,
      @JsonProperty("outputType") @Nullable String outputType,
      @JsonProperty("traceMetadata") @Nullable TraceMetadata traceMetadata,
      @JsonProperty("responder") @NonNull ResponderBlueprint responder,
      @JsonProperty("toolClassNames") @NonNull List<String> toolClassNames,
      @JsonProperty("handoffs") @NonNull List<HandoffDescriptor> handoffs,
      @JsonProperty("inputGuardrails") @NonNull List<GuardrailReference> inputGuardrails,
      @JsonProperty("outputGuardrails") @NonNull List<GuardrailReference> outputGuardrails,
      @JsonProperty("contextManagement") @Nullable ContextBlueprint contextManagement)
      implements InteractableBlueprint {

    private static final Logger log = LoggerFactory.getLogger(AgentBlueprint.class);

    @Override
    public Interactable toInteractable() {
      Responder resp = responder.toResponder();
      Agent.Builder builder =
          Agent.builder()
              .name(name)
              .model(model)
              .instructions(instructions.resolve())
              .responder(resp)
              .maxTurns(maxTurns);

      if (temperature != null) builder.temperature(temperature);
      if (traceMetadata != null) builder.traceMetadata(traceMetadata);

      // Output type
      if (outputType != null) {
        try {
          builder.outputType(Class.forName(outputType));
        } catch (ClassNotFoundException e) {
          log.warn("Could not load output type class: {}", outputType, e);
        }
      }

      // Tools via reflection
      for (String toolClassName : toolClassNames) {
        try {
          @SuppressWarnings("unchecked")
          FunctionTool<?> tool =
              (FunctionTool<?>) Class.forName(toolClassName).getDeclaredConstructor().newInstance();
          builder.addTool(tool);
        } catch (Exception e) {
          log.warn("Could not instantiate tool: {}", toolClassName, e);
        }
      }

      // Handoffs (recursive)
      for (HandoffDescriptor hd : handoffs) {
        Interactable target = hd.target().toInteractable();
        if (target instanceof Agent targetAgent) {
          builder.addHandoff(
              Handoff.to(targetAgent)
                  .withName(hd.name())
                  .withDescription(hd.description())
                  .build());
        }
      }

      // Guardrails
      for (GuardrailReference ref : inputGuardrails) {
        try {
          builder.addInputGuardrail(ref.toInputGuardrail());
        } catch (Exception e) {
          log.warn("Could not restore input guardrail: {}", ref, e);
        }
      }
      for (GuardrailReference ref : outputGuardrails) {
        try {
          builder.addOutputGuardrail(ref.toOutputGuardrail());
        } catch (Exception e) {
          log.warn("Could not restore output guardrail: {}", ref, e);
        }
      }

      // Context management
      if (contextManagement != null) {
        ContextManagementConfig config = contextManagement.toConfig(resp);
        builder.contextManagement(config);
      }

      return builder.build();
    }
  }

  /** Blueprint for an {@link AgentNetwork}. */
  record AgentNetworkBlueprint(
      @JsonProperty("name") @NonNull String name,
      @JsonProperty("peers")
          @JsonDeserialize(contentUsing = BlueprintDeserializer.class)
          @NonNull
          List<InteractableBlueprint> peers,
      @JsonProperty("maxRounds") int maxRounds,
      @JsonProperty("synthesizer")
          @JsonDeserialize(using = BlueprintDeserializer.class)
          @Nullable
          InteractableBlueprint synthesizer,
      @JsonProperty("traceMetadata") @Nullable TraceMetadata traceMetadata)
      implements InteractableBlueprint {

    @Override
    public Interactable toInteractable() {
      AgentNetwork.Builder builder = AgentNetwork.builder().name(name).maxRounds(maxRounds);

      for (InteractableBlueprint p : peers) {
        builder.addPeer(p.toInteractable());
      }

      if (synthesizer != null) {
        builder.synthesizer(synthesizer.toInteractable());
      }
      if (traceMetadata != null) {
        builder.traceMetadata(traceMetadata);
      }

      return builder.build();
    }
  }

  /** Blueprint for a {@link SupervisorAgent}. */
  record SupervisorAgentBlueprint(
      @JsonProperty("name") @NonNull String name,
      @JsonProperty("model") @NonNull String model,
      @JsonProperty("instructions") @NonNull String instructions,
      @JsonProperty("maxTurns") int maxTurns,
      @JsonProperty("workers") @NonNull List<WorkerBlueprint> workers,
      @JsonProperty("responder") @NonNull ResponderBlueprint responder,
      @JsonProperty("traceMetadata") @Nullable TraceMetadata traceMetadata)
      implements InteractableBlueprint {

    @Override
    public Interactable toInteractable() {
      Responder resp = responder.toResponder();
      SupervisorAgent.Builder builder =
          SupervisorAgent.builder()
              .name(name)
              .model(model)
              .instructions(instructions)
              .responder(resp)
              .maxTurns(maxTurns);

      for (WorkerBlueprint wb : workers) {
        builder.addWorker(wb.worker().toInteractable(), wb.description());
      }
      if (traceMetadata != null) {
        builder.traceMetadata(traceMetadata);
      }
      return builder.build();
    }
  }

  /** Blueprint for {@link ParallelAgents}. */
  record ParallelAgentsBlueprint(
      @JsonProperty("name") @NonNull String name,
      @JsonProperty("members")
          @JsonDeserialize(contentUsing = BlueprintDeserializer.class)
          @NonNull
          List<InteractableBlueprint> members,
      @JsonProperty("traceMetadata") @Nullable TraceMetadata traceMetadata)
      implements InteractableBlueprint {

    @Override
    public Interactable toInteractable() {
      List<Interactable> restored =
          members.stream().map(InteractableBlueprint::toInteractable).toList();
      return ParallelAgents.named(name, restored.toArray(new Interactable[0]));
    }
  }

  /** Blueprint for a {@link RouterAgent}. */
  record RouterAgentBlueprint(
      @JsonProperty("name") @NonNull String name,
      @JsonProperty("model") @NonNull String model,
      @JsonProperty("routes") @NonNull List<RouteBlueprint> routes,
      @JsonProperty("fallback")
          @JsonDeserialize(using = BlueprintDeserializer.class)
          @Nullable
          InteractableBlueprint fallback,
      @JsonProperty("responder") @NonNull ResponderBlueprint responder,
      @JsonProperty("traceMetadata") @Nullable TraceMetadata traceMetadata)
      implements InteractableBlueprint {

    @Override
    public Interactable toInteractable() {
      Responder resp = responder.toResponder();
      RouterAgent.Builder builder = RouterAgent.builder().name(name).model(model).responder(resp);

      for (RouteBlueprint rb : routes) {
        builder.addRoute(rb.target().toInteractable(), rb.description());
      }
      if (fallback != null) {
        builder.fallback(fallback.toInteractable());
      }
      if (traceMetadata != null) {
        builder.traceMetadata(traceMetadata);
      }
      return builder.build();
    }
  }

  /** Blueprint for {@link HierarchicalAgents}. */
  record HierarchicalAgentsBlueprint(
      @JsonProperty("executive") @NonNull AgentBlueprint executive,
      @JsonProperty("departments") @NonNull Map<String, DepartmentBlueprint> departments,
      @JsonProperty("maxTurns") int maxTurns,
      @JsonProperty("traceMetadata") @Nullable TraceMetadata traceMetadata)
      implements InteractableBlueprint {

    @Override
    public @NonNull String name() {
      return executive.name() + "_Hierarchy";
    }

    @Override
    public Interactable toInteractable() {
      Agent exec = (Agent) executive.toInteractable();
      HierarchicalAgents.Builder builder =
          HierarchicalAgents.builder().executive(exec).maxTurns(maxTurns);

      for (var entry : departments.entrySet()) {
        DepartmentBlueprint db = entry.getValue();
        Agent manager = (Agent) db.manager().toInteractable();
        List<Interactable> workers =
            db.workers().stream().map(InteractableBlueprint::toInteractable).toList();
        builder.addDepartment(entry.getKey(), manager, workers);
      }
      if (traceMetadata != null) {
        builder.traceMetadata(traceMetadata);
      }
      return builder.build();
    }
  }

  // ===== Custom Deserializer for $ref support =====

  /**
   * Custom deserializer that handles {@code $ref} file references.
   *
   * <p>When a JSON/YAML node contains a {@code "$ref"} field, the deserializer reads the
   * referenced file and parses it as an {@link InteractableBlueprint}. The path is resolved
   * relative to the current working directory. Both absolute and relative paths are supported.
   *
   * <p>For nodes without {@code $ref}, deserialization delegates to Jackson's standard
   * polymorphic type resolution using the {@code "type"} discriminator field.
   *
   * <h2>Usage in YAML</h2>
   *
   * <pre>{@code
   * # Instead of inlining the full agent definition:
   * target:
   *   $ref: ./agents/cardiologista.yaml
   *
   * # Which is equivalent to copying the contents of cardiologista.yaml here
   * }</pre>
   *
   * @since 1.0
   */
  final class BlueprintDeserializer extends JsonDeserializer<InteractableBlueprint> {

    /**
     * Internal interface that mirrors the polymorphic type annotations of
     * {@link InteractableBlueprint}. Used to delegate non-{@code $ref} deserialization
     * back to Jackson's standard type resolution without infinite recursion.
     */
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
    @JsonSubTypes({
      @JsonSubTypes.Type(value = AgentBlueprint.class, name = "agent"),
      @JsonSubTypes.Type(value = AgentNetworkBlueprint.class, name = "network"),
      @JsonSubTypes.Type(value = SupervisorAgentBlueprint.class, name = "supervisor"),
      @JsonSubTypes.Type(value = ParallelAgentsBlueprint.class, name = "parallel"),
      @JsonSubTypes.Type(value = RouterAgentBlueprint.class, name = "router"),
      @JsonSubTypes.Type(value = HierarchicalAgentsBlueprint.class, name = "hierarchical")
    })
    interface Delegate {}

    @Override
    public InteractableBlueprint deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException {
      ObjectMapper mapper = (ObjectMapper) p.getCodec();
      JsonNode node = mapper.readTree(p);

      // Legacy $ref — kept for backward compatibility
      if (node.has("$ref")) {
        String refPath = node.get("$ref").asText();
        return resolveFileRef(refPath, p);
      }

      // New discriminated union: source: file | registry
      if (node.has("source")) {
        String source = node.get("source").asText();
        return switch (source) {
          case "file" -> {
            String path = requireField(node, "path", "source: file", p);
            yield resolveFileRef(path, p);
          }
          case "registry" -> {
            String id = requireField(node, "id", "source: registry", p);
            InteractableBlueprint bp = BlueprintRegistry.get(id);
            if (bp == null) {
              throw new com.fasterxml.jackson.core.JsonParseException(
                  p,
                  "Blueprint not found in registry: '"
                      + id
                      + "'. Ensure BlueprintRegistry.register(\""
                      + id
                      + "\", ...) is called at startup.");
            }
            yield bp;
          }
          default -> throw new com.fasterxml.jackson.core.JsonParseException(
              p,
              "Unknown blueprint source: '"
                  + source
                  + "'. Valid values: 'file', 'registry'.");
        };
      }

      // Inline blueprint — dispatch to concrete class by type discriminator.
      JsonNode typeNode = node.get("type");
      if (typeNode == null || typeNode.isNull()) {
        throw new com.fasterxml.jackson.core.JsonParseException(
            p,
            "Blueprint is missing required 'type' field. "
                + "Expected one of: agent, network, supervisor, parallel, router, hierarchical.");
      }
      return switch (typeNode.asText()) {
        case "agent" -> mapper.treeToValue(node, AgentBlueprint.class);
        case "network" -> mapper.treeToValue(node, AgentNetworkBlueprint.class);
        case "supervisor" -> mapper.treeToValue(node, SupervisorAgentBlueprint.class);
        case "parallel" -> mapper.treeToValue(node, ParallelAgentsBlueprint.class);
        case "router" -> mapper.treeToValue(node, RouterAgentBlueprint.class);
        case "hierarchical" -> mapper.treeToValue(node, HierarchicalAgentsBlueprint.class);
        default -> throw new com.fasterxml.jackson.core.JsonParseException(
            p,
            "Unknown blueprint type: '"
                + typeNode.asText()
                + "'. Expected one of: agent, network, supervisor, parallel, router, hierarchical.");
      };
    }

    /**
     * Requires a field to be present in the node, throwing a descriptive error if missing.
     *
     * @param node the JSON node
     * @param field the required field name
     * @param context a human-readable description of the context (e.g. "source: file")
     * @param p the parser (for error location)
     * @return the field value as text
     * @throws com.fasterxml.jackson.core.JsonParseException if the field is absent
     */
    private String requireField(JsonNode node, String field, String context, JsonParser p)
        throws IOException {
      if (!node.has(field) || node.get(field).isNull()) {
        throw new com.fasterxml.jackson.core.JsonParseException(
            p, "Missing required field '" + field + "' for " + context + " blueprint reference.");
      }
      return node.get(field).asText();
    }

    /**
     * Resolves a file path by reading the file and parsing it as an {@link InteractableBlueprint}.
     *
     * @param refPath the file path (relative to CWD or absolute)
     * @param p the parser (for error messages)
     * @return the deserialized blueprint
     * @throws IOException if the file cannot be read or parsed
     */
    private InteractableBlueprint resolveFileRef(String refPath, JsonParser p) throws IOException {
      Path path = Path.of(refPath);
      if (!Files.exists(path)) {
        throw new IOException(
            "Blueprint file not found: "
                + path.toAbsolutePath()
                + ". Ensure the path is correct relative to the working directory.");
      }
      String content = Files.readString(path, StandardCharsets.UTF_8);

      // Detect format by file extension
      String fileName = path.getFileName().toString().toLowerCase();
      if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
        return InteractableBlueprint.fromYaml(content);
      } else {
        return InteractableBlueprint.fromJson(content);
      }
    }
  }
}
