package com.paragon.agents;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.paragon.agents.InteractableBlueprint.AgentBlueprint;
import com.paragon.agents.InteractableBlueprint.ContextBlueprint;
import com.paragon.agents.InteractableBlueprint.GuardrailReference;
import com.paragon.agents.InteractableBlueprint.HandoffDescriptor;
import com.paragon.agents.InteractableBlueprint.ResponderBlueprint;
import com.paragon.agents.context.ContextManagementConfig;
import com.paragon.agents.context.SlidingWindowStrategy;
import com.paragon.agents.context.SummarizationStrategy;
import com.paragon.agents.context.TokenCounter;
import com.paragon.responses.Responder;
import com.paragon.responses.spec.FunctionTool;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A richly annotated agent definition record designed for <b>LLM structured output</b>.
 *
 * <p>Every field carries a {@link JsonPropertyDescription} annotation that produces a JSON Schema
 * description. When used as the output type of {@code interactStructured()}, the LLM sees these
 * descriptions and knows exactly what each field means and how to fill it.
 *
 * <h2>Meta-Agent Pattern</h2>
 *
 * <pre>{@code
 * // A meta-agent that creates other agents
 * Interactable.Structured<AgentDefinition> metaAgent = Agent.builder()
 *     .name("AgentFactory")
 *     .model("openai/gpt-4o")
 *     .instructions("You create agent definitions based on user requirements.")
 *     .structured(AgentDefinition.class)
 *     .responder(responder)
 *     .build();
 *
 * // Ask the LLM to define an agent
 * StructuredAgentResult<AgentDefinition> result = metaAgent.interactStructured(
 *     "Create a customer support agent that speaks Spanish and uses a knowledge base tool"
 * );
 *
 * // Convert to a live, functional agent
 * AgentDefinition definition = result.output();
 * Interactable newAgent = definition.toInteractable(responder);
 * AgentResult output = newAgent.interact("¿Cómo puedo recuperar mi contraseña?");
 * }</pre>
 *
 * <h2>Design Notes</h2>
 *
 * <ul>
 *   <li>This record contains <b>only behavioral fields</b> — things the LLM should decide (name,
 *       model, instructions, tools, guardrails, etc.)
 *   <li>Infrastructure concerns ({@link Responder}, API keys, HTTP clients) are <b>never</b> part
 *       of this record — they are provided externally via {@link #toInteractable(Responder)}
 *   <li>All list fields are {@code @Nullable} and default to empty lists — the LLM can omit them
 *   <li>Bridges to the {@link InteractableBlueprint} system via {@link #toBlueprint(ResponderBlueprint)}
 * </ul>
 *
 * @see InteractableBlueprint.AgentBlueprint
 * @see Interactable.Structured
 * @since 1.0
 */
public record AgentDefinition(
    @JsonProperty(value = "name", required = true)
        @JsonPropertyDescription(
            "Unique name for this agent. Used for identification in logs, handoffs, and"
                + " multi-agent systems. Should be descriptive and concise. Examples:"
                + " 'CustomerSupport', 'CodeReviewer', 'TranslationAgent', 'DataAnalyst'.")
        @NonNull
        String name,
    @JsonProperty(value = "model", required = true)
        @JsonPropertyDescription(
            "The LLM model identifier to use. Choose based on the agent's complexity and"
                + " cost/quality tradeoffs. Examples: 'openai/gpt-4o' (highest quality),"
                + " 'openai/gpt-4o-mini' (fast and cheap), 'anthropic/claude-3.5-sonnet',"
                + " 'google/gemini-2.0-flash-001'.")
        @NonNull
        String model,
    @JsonProperty(value = "instructions", required = true)
        @JsonPropertyDescription(
            "System prompt that defines the agent's personality, behavior, constraints, and"
                + " capabilities. This is the most important field — it shapes everything the agent"
                + " does. Write clear, specific, and detailed instructions. Use newlines to"
                + " separate sections. Include guidelines, prohibited actions, output formatting"
                + " rules, and domain-specific knowledge.")
        @NonNull
        String instructions,
    @JsonProperty(value = "maxTurns", required = true)
        @JsonPropertyDescription(
            "Maximum number of LLM turns in the agentic loop. Each tool call consumes one turn."
                + " Recommended values: 1 for simple Q&A without tools, 5-10 for agents with a"
                + " few tools, 10-20 for complex multi-step reasoning tasks. Must be at least 1.")
        int maxTurns,
    @JsonProperty("temperature")
        @JsonPropertyDescription(
            "LLM temperature controlling response randomness. 0.0 = deterministic and focused,"
                + " 0.7 = balanced (good default), 1.0 = creative, 2.0 = very random. Use low"
                + " values (0.0-0.3) for factual/analytical tasks, medium (0.5-0.8) for general"
                + " conversation, high (0.8-1.2) for creative writing.")
        @Nullable
        Double temperature,
    @JsonProperty("toolClassNames")
        @JsonPropertyDescription(
            "Fully qualified class names of FunctionTool implementations this agent can use."
                + " Each tool must have a no-argument constructor. The agent will automatically"
                + " call these tools when needed. Example: ['com.myapp.tools.SearchKnowledgeBase',"
                + " 'com.myapp.tools.LookupOrder']. Omit or leave empty if the agent needs no"
                + " tools.")
        @Nullable
        List<String> toolClassNames,
    @JsonProperty("inputGuardrails")
        @JsonPropertyDescription(
            "Guardrails that validate user input BEFORE it reaches the LLM. Each guardrail is"
                + " referenced either by a registry ID (for lambda guardrails registered at"
                + " startup) or by a fully qualified class name (for class-based guardrails with"
                + " no-arg constructors). Omit if no input validation is needed.")
        @Nullable
        List<GuardrailDef> inputGuardrails,
    @JsonProperty("outputGuardrails")
        @JsonPropertyDescription(
            "Guardrails that validate LLM responses BEFORE returning them to the user. Same"
                + " format as input guardrails. Use these to filter PII, enforce formatting, or"
                + " block inappropriate content. Omit if no output validation is needed.")
        @Nullable
        List<GuardrailDef> outputGuardrails,
    @JsonProperty("handoffs")
        @JsonPropertyDescription(
            "Other agents that this agent can delegate conversations to. When the agent detects"
                + " that a user's request falls outside its expertise, it hands off to a"
                + " specialized agent. Each handoff defines a target agent and a description of"
                + " when to use it. Omit if the agent operates standalone.")
        @Nullable
        List<HandoffAgentDef> handoffs,
    @JsonProperty("contextManagement")
        @JsonPropertyDescription(
            "Strategy for managing the conversation context window. When conversation history"
                + " exceeds the token limit, this strategy decides what to keep and what to"
                + " discard. Choose 'sliding' to trim old messages or 'summarization' to"
                + " compress history into a summary. Omit to use no context management.")
        @Nullable
        ContextDef contextManagement) {

  private static final Logger log = LoggerFactory.getLogger(AgentDefinition.class);

  // ===== Nested Records =====

  /**
   * Reference to a guardrail — either by registry ID (for lambda guardrails) or by fully
   * qualified class name (for class-based guardrails).
   */
  public record GuardrailDef(
      @JsonProperty("registryId")
          @JsonPropertyDescription(
              "ID of a guardrail previously registered via InputGuardrail.named() or"
                  + " OutputGuardrail.named(). Use this for lambda/anonymous guardrails that were"
                  + " registered at application startup. Example: 'profanity_filter',"
                  + " 'max_length', 'no_pii'.")
          @Nullable
          String registryId,
      @JsonProperty("className")
          @JsonPropertyDescription(
              "Fully qualified class name of a guardrail implementation with a no-argument"
                  + " constructor. Example: 'com.myapp.guards.ProfanityFilter'. Use this for"
                  + " guardrails implemented as standalone classes.")
          @Nullable
          String className) {

    /** Converts to the blueprint's {@link GuardrailReference}. */
    public GuardrailReference toGuardrailReference() {
      return new GuardrailReference(className, registryId);
    }
  }

  /**
   * Definition of an agent that this agent can hand off to. Contains a nested {@link
   * AgentDefinition} for the target agent.
   */
  public record HandoffAgentDef(
      @JsonProperty(value = "name", required = true)
          @JsonPropertyDescription(
              "A short, unique name for this handoff. Used as the tool name the LLM calls to"
                  + " trigger the handoff. Should be snake_case. Examples:"
                  + " 'escalate_to_billing', 'transfer_to_specialist'.")
          @NonNull
          String name,
      @JsonProperty(value = "description", required = true)
          @JsonPropertyDescription(
              "Description of WHEN this handoff should be triggered. The LLM reads this to"
                  + " decide whether to route the conversation. Be specific about the domains,"
                  + " topics, or signals that indicate this handoff is appropriate. Example:"
                  + " 'Transfer to billing for invoices, refunds, and payment disputes'.")
          @NonNull
          String description,
      @JsonProperty(value = "target", required = true)
          @JsonPropertyDescription(
              "The target agent definition that will handle the conversation after handoff."
                  + " This is a full AgentDefinition — you define the specialist agent inline.")
          @NonNull
          AgentDefinition target) {}

  /**
   * Configuration for context window management. Controls how conversation history is trimmed
   * when it grows too large.
   */
  public record ContextDef(
      @JsonProperty(value = "strategyType", required = true)
          @JsonPropertyDescription(
              "The strategy type: 'sliding' drops oldest messages when context is full"
                  + " (simple and fast), 'summarization' compresses old messages into a summary"
                  + " using a secondary LLM call (preserves more context but costs an extra API"
                  + " call). Choose 'sliding' for most use cases.")
          @NonNull
          String strategyType,
      @JsonProperty(value = "maxTokens", required = true)
          @JsonPropertyDescription(
              "Maximum number of tokens to keep in the context window. When exceeded, the"
                  + " strategy activates. Typical values: 4000 for gpt-4o-mini, 8000 for"
                  + " gpt-4o, 16000 for models with large context windows.")
          int maxTokens,
      @JsonProperty("preserveDeveloperMessages")
          @JsonPropertyDescription(
              "For 'sliding' strategy only: whether to keep system/developer messages when"
                  + " trimming old conversation history. Recommended: true.")
          @Nullable
          Boolean preserveDeveloperMessages,
      @JsonProperty("summarizationModel")
          @JsonPropertyDescription(
              "For 'summarization' strategy only: the LLM model to use for summarizing old"
                  + " messages. Should be a fast, cheap model. Example: 'openai/gpt-4o-mini'.")
          @Nullable
          String summarizationModel,
      @JsonProperty("keepRecentMessages")
          @JsonPropertyDescription(
              "For 'summarization' strategy only: number of recent messages to keep verbatim"
                  + " (not summarized). Recommended: 3-10.")
          @Nullable
          Integer keepRecentMessages,
      @JsonProperty("summarizationPrompt")
          @JsonPropertyDescription(
              "For 'summarization' strategy only: custom prompt for the summarization LLM."
                  + " Omit to use the built-in default.")
          @Nullable
          String summarizationPrompt) {

    /** Converts to the blueprint's {@link ContextBlueprint}. */
    public ContextBlueprint toContextBlueprint() {
      return new ContextBlueprint(
          strategyType,
          preserveDeveloperMessages,
          summarizationModel,
          keepRecentMessages,
          summarizationPrompt,
          maxTokens,
          null);
    }

    /** Builds a live {@link ContextManagementConfig} from this definition. */
    public ContextManagementConfig toConfig(@Nullable Responder responder) {
      return toContextBlueprint().toConfig(responder);
    }
  }

  // ===== Conversion Methods =====

  /**
   * Reconstructs a fully functional {@link Interactable} agent using the provided
   * {@link Responder}.
   *
   * <p>This is the primary method for the <b>meta-agent pattern</b>: the LLM generates the
   * agent definition (behavioral config), and the caller provides the infrastructure
   * (API client).
   *
   * <pre>{@code
   * AgentDefinition def = metaAgent.interactStructured("Create a support agent")
   *     .output();
   * Interactable agent = def.toInteractable(responder);
   * agent.interact("Hello!");
   * }</pre>
   *
   * @param responder the Responder to use for LLM API calls
   * @return a fully functional Agent
   */
  public @NonNull Interactable toInteractable(@NonNull Responder responder) {
    Agent.Builder builder =
        Agent.builder()
            .name(name)
            .model(model)
            .instructions(instructions)
            .responder(responder)
            .maxTurns(maxTurns);

    if (temperature != null) builder.temperature(temperature);

    // Tools via reflection
    if (toolClassNames != null) {
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
    }

    // Input guardrails
    if (inputGuardrails != null) {
      for (GuardrailDef ref : inputGuardrails) {
        try {
          builder.addInputGuardrail(ref.toGuardrailReference().toInputGuardrail());
        } catch (Exception e) {
          log.warn("Could not restore input guardrail: {}", ref, e);
        }
      }
    }

    // Output guardrails
    if (outputGuardrails != null) {
      for (GuardrailDef ref : outputGuardrails) {
        try {
          builder.addOutputGuardrail(ref.toGuardrailReference().toOutputGuardrail());
        } catch (Exception e) {
          log.warn("Could not restore output guardrail: {}", ref, e);
        }
      }
    }

    // Handoffs (recursive)
    if (handoffs != null) {
      for (HandoffAgentDef hd : handoffs) {
        Interactable target = hd.target().toInteractable(responder);
        if (target instanceof Agent targetAgent) {
          builder.addHandoff(
              Handoff.to(targetAgent)
                  .withName(hd.name())
                  .withDescription(hd.description())
                  .build());
        }
      }
    }

    // Context management
    if (contextManagement != null) {
      builder.contextManagement(contextManagement.toConfig(responder));
    }

    return builder.build();
  }

  /**
   * Converts this definition to an {@link AgentBlueprint}, bridging to the blueprint
   * serialization system.
   *
   * <p>Requires a {@link ResponderBlueprint} because blueprints are self-contained (they
   * include the responder configuration for environment-variable–based reconstruction).
   *
   * @param responderBlueprint the responder configuration for the resulting blueprint
   * @return an AgentBlueprint equivalent of this definition
   */
  public @NonNull AgentBlueprint toBlueprint(@NonNull ResponderBlueprint responderBlueprint) {
    List<GuardrailReference> inputRefs = new ArrayList<>();
    if (inputGuardrails != null) {
      for (GuardrailDef g : inputGuardrails) {
        inputRefs.add(g.toGuardrailReference());
      }
    }

    List<GuardrailReference> outputRefs = new ArrayList<>();
    if (outputGuardrails != null) {
      for (GuardrailDef g : outputGuardrails) {
        outputRefs.add(g.toGuardrailReference());
      }
    }

    List<HandoffDescriptor> handoffDescriptors = new ArrayList<>();
    if (handoffs != null) {
      for (HandoffAgentDef hd : handoffs) {
        handoffDescriptors.add(
            new HandoffDescriptor(
                hd.name(), hd.description(), hd.target().toBlueprint(responderBlueprint)));
      }
    }

    return new AgentBlueprint(
        name,
        model,
        instructions,
        maxTurns,
        temperature,
        null, // outputType — not part of behavioral definition
        null, // traceMetadata — infrastructure concern
        responderBlueprint,
        toolClassNames != null ? toolClassNames : List.of(),
        handoffDescriptors,
        inputRefs,
        outputRefs,
        contextManagement != null ? contextManagement.toContextBlueprint() : null);
  }

  /**
   * Creates an {@link AgentDefinition} from an existing {@link AgentBlueprint}.
   *
   * <p>This is useful for extracting the behavioral configuration from a serialized blueprint,
   * for example to edit it or use it as a template.
   *
   * @param blueprint the source blueprint
   * @return an AgentDefinition with the behavioral fields from the blueprint
   */
  public static @NonNull AgentDefinition fromBlueprint(@NonNull AgentBlueprint blueprint) {
    List<GuardrailDef> inputDefs = new ArrayList<>();
    for (GuardrailReference ref : blueprint.inputGuardrails()) {
      inputDefs.add(new GuardrailDef(ref.registryId(), ref.className()));
    }

    List<GuardrailDef> outputDefs = new ArrayList<>();
    for (GuardrailReference ref : blueprint.outputGuardrails()) {
      outputDefs.add(new GuardrailDef(ref.registryId(), ref.className()));
    }

    List<HandoffAgentDef> handoffDefs = new ArrayList<>();
    for (HandoffDescriptor hd : blueprint.handoffs()) {
      if (hd.target() instanceof AgentBlueprint targetBlueprint) {
        handoffDefs.add(
            new HandoffAgentDef(hd.name(), hd.description(), fromBlueprint(targetBlueprint)));
      }
    }

    ContextDef contextDef = null;
    if (blueprint.contextManagement() != null) {
      ContextBlueprint cb = blueprint.contextManagement();
      contextDef =
          new ContextDef(
              cb.strategyType(),
              cb.maxTokens(),
              cb.preserveDeveloperMessages(),
              cb.summarizationModel(),
              cb.keepRecentMessages(),
              cb.summarizationPrompt());
    }

    return new AgentDefinition(
        blueprint.name(),
        blueprint.model(),
        blueprint.instructions(),
        blueprint.maxTurns(),
        blueprint.temperature(),
        blueprint.toolClassNames().isEmpty() ? null : blueprint.toolClassNames(),
        inputDefs.isEmpty() ? null : inputDefs,
        outputDefs.isEmpty() ? null : outputDefs,
        handoffDefs.isEmpty() ? null : handoffDefs,
        contextDef);
  }
}
