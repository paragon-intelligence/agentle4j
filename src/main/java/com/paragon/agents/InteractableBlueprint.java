package com.paragon.agents;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.paragon.agents.context.ContextManagementConfig;
import com.paragon.agents.context.SlidingWindowStrategy;
import com.paragon.agents.context.SummarizationStrategy;
import com.paragon.agents.context.TokenCounter;
import com.paragon.http.RetryPolicy;
import com.paragon.responses.Responder;
import com.paragon.responses.TraceMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.ReasoningConfig;
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
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.dataformat.yaml.YAMLMapper;

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
   * Reconstructs a fully functional {@link Interactable.Structured} from this blueprint.
   *
   * <p>Equivalent to {@code toStructured(outputType, new ObjectMapper())}.
   *
   * @param <T> the structured output type
   * @param outputType the class to parse the agent's output into
   * @return a fully functional {@code Interactable.Structured<T>}
   * @throws UnsupportedOperationException if the blueprint type does not support structured output
   */
  default <T> Interactable.@NonNull Structured<T> toStructured(@NonNull Class<T> outputType) {
    return toStructured(outputType, new ObjectMapper());
  }

  /**
   * Reconstructs a fully functional {@link Interactable.Structured} from this blueprint.
   *
   * <p>Supported blueprint types: {@code agent}, {@code router}, {@code supervisor}.
   *
   * <p>Example:
   *
   * <pre>{@code
   * Interactable.Structured<ResultadoConsulta> agente = InteractableBlueprint
   *     .fromYaml(yaml)
   *     .toStructured(ResultadoConsulta.class);
   * }</pre>
   *
   * @param <T> the structured output type
   * @param outputType the class to parse the agent's output into
   * @param objectMapper the ObjectMapper to use for JSON parsing
   * @return a fully functional {@code Interactable.Structured<T>}
   * @throws UnsupportedOperationException if the blueprint type does not support structured output
   */
  default <T> Interactable.@NonNull Structured<T> toStructured(
      @NonNull Class<T> outputType, @NonNull ObjectMapper objectMapper) {
    return switch (toInteractable()) {
      case Agent agent -> new Agent.Structured<>(agent, outputType);
      case RouterAgent router -> RouterAgent.Structured.of(router, outputType, objectMapper);
      case SupervisorAgent supervisor ->
          new SupervisorAgent.Structured<>(supervisor, outputType, objectMapper);
      case Interactable other ->
          throw new UnsupportedOperationException(
              other.getClass().getSimpleName()
                  + " does not support structured output. "
                  + "Supported blueprint types: agent, router, supervisor.");
    };
  }

  /**
   * Serializes this blueprint to a JSON string using the provided {@link ObjectMapper}.
   *
   * @param mapper the ObjectMapper to use for serialization
   * @return a JSON string representation of this blueprint
   * @throws IllegalStateException if serialization fails
   */
  default @NonNull String toJson(@NonNull ObjectMapper mapper) {
    try {
      return mapper.writeValueAsString(this);
    } catch (tools.jackson.core.JacksonException e) {
      throw new IllegalStateException("Failed to serialize blueprint to JSON", e);
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
   * @throws IllegalStateException if serialization fails
   */
  default @NonNull String toYaml(@NonNull YAMLMapper mapper) {
    try {
      return mapper.writeValueAsString(this);
    } catch (tools.jackson.core.JacksonException e) {
      throw new IllegalStateException("Failed to serialize blueprint to YAML", e);
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
   * @throws IllegalStateException if deserialization fails
   */
  static @NonNull InteractableBlueprint fromYaml(@NonNull String yaml) {
    try {
      return new YAMLMapper()
          .rebuild()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .build()
          .readValue(yaml, InteractableBlueprint.class);
    } catch (tools.jackson.core.JacksonException e) {
      throw new IllegalStateException("Failed to deserialize blueprint from YAML", e);
    }
  }

  /**
   * Deserializes an {@link InteractableBlueprint} from a JSON string.
   *
   * @param json the JSON string
   * @return the deserialized blueprint
   * @throws IllegalStateException if deserialization fails
   */
  static @NonNull InteractableBlueprint fromJson(@NonNull String json) {
    try {
      return new ObjectMapper()
          .rebuild()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .build()
          .readValue(json, InteractableBlueprint.class);
    } catch (tools.jackson.core.JacksonException e) {
      throw new IllegalStateException("Failed to deserialize blueprint from JSON", e);
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
      @JsonProperty("target") @JsonDeserialize(using = BlueprintDeserializer.class)
          @NonNull InteractableBlueprint target) {

    public static HandoffDescriptor from(@NonNull Handoff handoff) {
      return new HandoffDescriptor(
          handoff.name(), handoff.description(), handoff.targetAgent().toBlueprint());
    }
  }

  /** Serializable descriptor for a {@link SupervisorAgent.Worker}. */
  record WorkerBlueprint(
      @JsonProperty("worker") @JsonDeserialize(using = BlueprintDeserializer.class)
          @NonNull InteractableBlueprint worker,
      @JsonProperty("description") @NonNull String description) {}

  /** Serializable descriptor for a {@link RouterAgent.Route}. */
  record RouteBlueprint(
      @JsonProperty("target") @JsonDeserialize(using = BlueprintDeserializer.class)
          @NonNull InteractableBlueprint target,
      @JsonProperty("description") @NonNull String description) {}

  /** Serializable descriptor for a {@link HierarchicalAgents.Department}. */
  record DepartmentBlueprint(
      @JsonProperty("manager") @NonNull AgentBlueprint manager,
      @JsonProperty("workers") @JsonDeserialize(contentUsing = BlueprintDeserializer.class)
          @NonNull List<InteractableBlueprint> workers) {}

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
      @JsonProperty("contextManagement") @Nullable ContextBlueprint contextManagement,
      @JsonProperty("reasoning") @Nullable ReasoningConfig reasoning)
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

      // Reasoning config
      if (reasoning != null) {
        builder.reasoning(reasoning);
      }

      return builder.build();
    }
  }

  /** Blueprint for an {@link AgentNetwork}. */
  record AgentNetworkBlueprint(
      @JsonProperty("name") @NonNull String name,
      @JsonProperty("peers") @JsonDeserialize(contentUsing = BlueprintDeserializer.class)
          @NonNull List<InteractableBlueprint> peers,
      @JsonProperty("maxRounds") int maxRounds,
      @JsonProperty("synthesizer") @JsonDeserialize(using = BlueprintDeserializer.class)
          @Nullable InteractableBlueprint synthesizer,
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
      @JsonDeserialize(contentUsing = BlueprintDeserializer.class) @JsonProperty("members")
          @NonNull List<InteractableBlueprint> members,
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
      @JsonDeserialize(using = BlueprintDeserializer.class) @JsonProperty("fallback")
          @Nullable InteractableBlueprint fallback,
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
   * <p>When a JSON/YAML node contains a {@code "$ref"} field, the deserializer reads the referenced
   * file and parses it as an {@link InteractableBlueprint}. The path is resolved relative to the
   * current working directory. Both absolute and relative paths are supported.
   *
   * <p>For nodes without {@code $ref}, deserialization delegates to Jackson's standard polymorphic
   * type resolution using the {@code "type"} discriminator field.
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
  final class BlueprintDeserializer extends ValueDeserializer<InteractableBlueprint> {

    /**
     * Called by Jackson when both {@code @JsonTypeInfo} and {@code @JsonDeserialize} are present on
     * the same type. Overriding this gives us full control of the token stream <em>before</em> the
     * {@code TypeDeserializer} reads and consumes the {@code type} property.
     *
     * <ul>
     *   <li>For {@code $ref} / {@code source: file|registry} nodes we resolve the reference
     *       directly — no {@code type} field is required.
     *   <li>For inline definitions we replay the full node into {@code typeDeserializer} so it can
     *       read {@code type} and dispatch to the correct concrete class as normal.
     * </ul>
     */
    @Override
    public InteractableBlueprint deserializeWithType(
        JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer)
        throws tools.jackson.core.JacksonException {
      // Fast path: if the format provides a native type id (e.g. YAML !<agent> tags),
      // delegate directly to the standard TypeDeserializer — no need to read the tree.
      if (p.canReadTypeId()) {
        Object nativeTypeId = p.getTypeId();
        if (nativeTypeId != null) {
          return (InteractableBlueprint) typeDeserializer.deserializeTypedFromAny(p, ctxt);
        }
      }

      // Slow path: read the full node so we can inspect it before type dispatch.
      // This handles $ref, source:, and property-based type ids (JSON).
      JsonNode node = p.readValueAsTree();

      // $ref / source: — no type field required
      if (node.has("$ref")) {
        return resolveFileRef(node.get("$ref").asText(), p);
      }
      if (node.has("source")) {
        return resolveSource(node, p);
      }

      // Inline blueprint with property-based type id: convert to TreeTraversingParser
      // (avoids YAML-specific tokenizer quirks) and delegate to AsPropertyTypeDeserializer.
      // Concrete classes carry no @JsonDeserialize → plain record deserializer, no recursion.
      JsonParser nodeParser = node.traverse(ctxt);
      nodeParser.nextToken(); // advance to START_OBJECT
      return (InteractableBlueprint) typeDeserializer.deserializeTypedFromObject(nodeParser, ctxt);
    }

    @Override
    public InteractableBlueprint deserialize(JsonParser p, DeserializationContext ctxt)
        throws tools.jackson.core.JacksonException {
      JsonNode node = p.readValueAsTree();

      // $ref / source: — same logic shared with deserializeWithType()
      if (node.has("$ref")) {
        return resolveFileRef(node.get("$ref").asText(), p);
      }
      if (node.has("source")) {
        return resolveSource(node, p);
      }

      // Inline blueprint — manually dispatch to the concrete class by type discriminator.
      return dispatchByType(node, ctxt, p);
    }

    /**
     * Dispatches to the concrete blueprint class by reading the {@code type} field from {@code
     * node}. Each concrete class carries {@code @JsonDeserialize(using=None.class)} to suppress the
     * inherited {@link BlueprintDeserializer}, so {@code treeToValue(node, ConcreteClass.class)}
     * uses the default record deserializer — no recursion.
     */
    private InteractableBlueprint dispatchByType(
        JsonNode node, DeserializationContext ctxt, JsonParser p)
        throws tools.jackson.core.JacksonException {
      JsonNode typeNode = node.get("type");
      if (typeNode == null || typeNode.isNull()) {
        throw MismatchedInputException.from(
            p,
            InteractableBlueprint.class,
            "Blueprint is missing required 'type' field. "
                + "Expected one of: agent, network, supervisor, parallel, router, hierarchical.");
      }
      return switch (typeNode.asText()) {
        case "agent" -> ctxt.readTreeAsValue(node, AgentBlueprint.class);
        case "network" -> ctxt.readTreeAsValue(node, AgentNetworkBlueprint.class);
        case "supervisor" -> ctxt.readTreeAsValue(node, SupervisorAgentBlueprint.class);
        case "parallel" -> ctxt.readTreeAsValue(node, ParallelAgentsBlueprint.class);
        case "router" -> ctxt.readTreeAsValue(node, RouterAgentBlueprint.class);
        case "hierarchical" -> ctxt.readTreeAsValue(node, HierarchicalAgentsBlueprint.class);
        default ->
            throw MismatchedInputException.from(
                p,
                InteractableBlueprint.class,
                "Unknown blueprint type: '"
                    + typeNode.asText()
                    + "'. Expected one of: agent, network, supervisor, parallel, router,"
                    + " hierarchical.");
      };
    }

    private InteractableBlueprint resolveSource(JsonNode node, JsonParser p)
        throws tools.jackson.core.JacksonException {
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
            throw MismatchedInputException.from(
                p,
                InteractableBlueprint.class,
                "Blueprint not found in registry: '"
                    + id
                    + "'. Ensure BlueprintRegistry.register(\""
                    + id
                    + "\", ...) is called at startup.");
          }
          yield bp;
        }
        default ->
            throw MismatchedInputException.from(
                p,
                InteractableBlueprint.class,
                "Unknown blueprint source: '" + source + "'. Valid values: 'file', 'registry'.");
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
     * @throws tools.jackson.core.JacksonException if the field is absent
     */
    private String requireField(JsonNode node, String field, String context, JsonParser p)
        throws tools.jackson.core.JacksonException {
      if (!node.has(field) || node.get(field).isNull()) {
        throw MismatchedInputException.from(
            p,
            InteractableBlueprint.class,
            "Missing required field '" + field + "' for " + context + " blueprint reference.");
      }
      return node.get(field).asText();
    }

    /**
     * Resolves a file path by reading the file and parsing it as an {@link InteractableBlueprint}.
     *
     * @param refPath the file path (relative to CWD or absolute)
     * @param p the parser (for error messages)
     * @return the deserialized blueprint
     * @throws tools.jackson.core.JacksonException if the file cannot be read or parsed
     */
    private InteractableBlueprint resolveFileRef(String refPath, JsonParser p)
        throws tools.jackson.core.JacksonException {
      Path path = Path.of(refPath);
      if (!Files.exists(path)) {
        throw MismatchedInputException.from(
            p,
            InteractableBlueprint.class,
            "Blueprint file not found: "
                + path.toAbsolutePath()
                + ". Ensure the path is correct relative to the working directory.");
      }
      final String content;
      try {
        content = Files.readString(path, StandardCharsets.UTF_8);
      } catch (IOException e) {
        throw DatabindException.from(
            p, "Failed to read blueprint file: " + path.toAbsolutePath(), e);
      }

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
