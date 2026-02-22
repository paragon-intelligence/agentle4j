package com.paragon.agents;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.paragon.agents.InteractableBlueprint.AgentBlueprint;
import com.paragon.agents.InteractableBlueprint.ContextBlueprint;
import com.paragon.agents.InteractableBlueprint.GuardrailReference;
import com.paragon.agents.InteractableBlueprint.HandoffDescriptor;
import com.paragon.agents.InteractableBlueprint.ResponderBlueprint;
import com.paragon.responses.Responder;
import com.paragon.responses.spec.FunctionTool;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A richly annotated agent definition record designed for <b>LLM structured output</b>.
 *
 * <p>Every field carries a {@link JsonPropertyDescription} that produces a JSON Schema description.
 * When used as the output type of {@code interactStructured()}, the LLM sees these descriptions
 * and knows exactly what each field means.
 *
 * <p>This record contains <b>only behavioral fields</b> — things the LLM can reason about.
 * Infrastructure concerns (model, API provider, API keys, HTTP config) are provided externally
 * via {@link #toInteractable(Responder, String)} or
 * {@link #toInteractable(Responder, String, List)}.
 *
 * <h2>Meta-Agent Pattern</h2>
 *
 * <pre>{@code
 * // A meta-agent that creates other agents
 * Interactable.Structured<AgentDefinition> metaAgent = Agent.builder()
 *     .name("AgentFactory")
 *     .model("openai/gpt-4o")
 *     .instructions("""
 *         You create agent definitions. Available tools you can assign:
 *         - "search_kb": Searches the company knowledge base
 *         - "create_ticket": Creates a support ticket
 *
 *         Available guardrails:
 *         - "profanity_filter": blocks profanity
 *         - "max_length": limits input to 10k chars
 *         """)
 *     .structured(AgentDefinition.class)
 *     .responder(responder)
 *     .build();
 *
 * AgentDefinition def = metaAgent.interactStructured(
 *     "Create a customer support agent that speaks Spanish"
 * ).output();
 *
 * // You provide infrastructure: responder, model, and available tools
 * Interactable agent = def.toInteractable(responder, "openai/gpt-4o", availableTools);
 * }</pre>
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
    @JsonProperty(value = "instructions", required = true)
        @JsonPropertyDescription(
            "System prompt that defines the agent's personality, behavior, constraints, and"
                + " capabilities. This is the most important field — it shapes everything the"
                + " agent does. Write clear, specific, and detailed instructions. Use newlines"
                + " to separate sections. Include guidelines, prohibited actions, output"
                + " formatting rules, and domain-specific knowledge.")
        @NonNull
        String instructions,
    @JsonProperty(value = "maxTurns", required = true)
        @JsonPropertyDescription(
            "Maximum number of LLM turns in the agentic loop. Each tool call consumes one"
                + " turn. Recommended values: 1 for simple Q&A without tools, 5-10 for agents"
                + " with a few tools, 10-20 for complex multi-step reasoning tasks. Must be at"
                + " least 1.")
        int maxTurns,
    @JsonProperty("temperature")
        @JsonPropertyDescription(
            "LLM temperature controlling response randomness. 0.0 = deterministic and focused,"
                + " 0.7 = balanced (good default), 1.0 = creative, 2.0 = very random. Use low"
                + " values (0.0-0.3) for factual/analytical tasks, medium (0.5-0.8) for general"
                + " conversation, high (0.8-1.2) for creative writing.")
        @Nullable
        Double temperature,
    @JsonProperty("toolNames")
        @JsonPropertyDescription(
            "Names of tools this agent should have access to. These are human-readable names"
                + " that match the names of tools registered in the application (e.g.,"
                + " 'search_knowledge_base', 'create_ticket', 'lookup_order'). The available"
                + " tools and their descriptions should be listed in your instructions. Omit if"
                + " the agent needs no tools.")
        @Nullable
        List<String> toolNames,
    @JsonProperty("inputGuardrails")
        @JsonPropertyDescription(
            "Names of input guardrails to apply before user input reaches the LLM. These are"
                + " IDs of guardrails registered at application startup (e.g.,"
                + " 'profanity_filter', 'max_length', 'topic_filter'). The available guardrails"
                + " and their behaviors should be listed in your instructions. Omit if no input"
                + " validation is needed.")
        @Nullable
        List<String> inputGuardrails,
    @JsonProperty("outputGuardrails")
        @JsonPropertyDescription(
            "Names of output guardrails to apply before returning LLM responses to the user."
                + " Same format as input guardrails. Use these to filter PII, enforce"
                + " formatting, or block inappropriate content. Omit if no output validation is"
                + " needed.")
        @Nullable
        List<String> outputGuardrails,
    @JsonProperty("handoffs")
        @JsonPropertyDescription(
            "Other agents that this agent can delegate conversations to. When the agent"
                + " detects that a user's request falls outside its expertise, it hands off to"
                + " a specialized agent. Each handoff defines the target agent inline. Omit if"
                + " the agent operates standalone.")
        @Nullable
        List<HandoffAgentDef> handoffs,
    @JsonProperty("contextManagement")
        @JsonPropertyDescription(
            "Strategy for managing the conversation context window. When conversation history"
                + " exceeds the token limit, this strategy decides what to keep. Choose"
                + " 'sliding' to trim old messages (simple, fast) or 'summarization' to"
                + " compress history into a summary (preserves more context). Omit to use no"
                + " context management.")
        @Nullable
        ContextDef contextManagement) {

  private static final Logger log = LoggerFactory.getLogger(AgentDefinition.class);

  // ===== Nested Records =====

  /**
   * Definition of an agent that this agent can hand off to. Defines the target agent inline.
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
                  + " topics, or signals. Example: 'Transfer to billing for invoices, refunds,"
                  + " and payment disputes'.")
          @NonNull
          String description,
      @JsonProperty(value = "target", required = true)
          @JsonPropertyDescription(
              "The target agent definition that will handle the conversation after handoff."
                  + " This is a full AgentDefinition — define the specialist agent inline.")
          @NonNull
          AgentDefinition target) {}

  /**
   * Context window management strategy configuration.
   */
  public record ContextDef(
      @JsonProperty(value = "strategyType", required = true)
          @JsonPropertyDescription(
              "The strategy type: 'sliding' drops oldest messages when context is full"
                  + " (simple, fast, recommended for most use cases), 'summarization' compresses"
                  + " old messages into a summary (preserves more context but costs an extra API"
                  + " call).")
          @NonNull
          String strategyType,
      @JsonProperty(value = "maxTokens", required = true)
          @JsonPropertyDescription(
              "Maximum number of tokens to keep in the context window. When exceeded, the"
                  + " strategy activates. Typical values: 4000 for simple agents, 8000 for"
                  + " conversational agents, 16000 for agents needing deep history.")
          int maxTokens,
      @JsonProperty("preserveDeveloperMessages")
          @JsonPropertyDescription(
              "For 'sliding' strategy only: whether to keep system/developer messages when"
                  + " trimming old conversation history. Recommended: true.")
          @Nullable
          Boolean preserveDeveloperMessages,
      @JsonProperty("summarizationModel")
          @JsonPropertyDescription(
              "For 'summarization' strategy only: the model to use for summarizing old"
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
              "For 'summarization' strategy only: custom prompt for the summarization. Omit"
                  + " to use the built-in default.")
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
  }

  // ===== Conversion Methods =====

  /**
   * Reconstructs a fully functional {@link Interactable} agent.
   *
   * <p>The caller provides all infrastructure: the Responder (API client) and the model to use.
   * Tool names from the definition are matched against the provided available tools by
   * comparing each tool's {@code name()} (from {@code @FunctionMetadata}).
   *
   * <pre>{@code
   * List<FunctionTool<?>> tools = List.of(searchTool, ticketTool, refundTool);
   * Interactable agent = definition.toInteractable(responder, "openai/gpt-4o", tools);
   * }</pre>
   *
   * @param responder the Responder for LLM API calls
   * @param model the LLM model identifier (e.g., "openai/gpt-4o")
   * @param availableTools all tools the agent may use; only those matching {@link #toolNames()}
   *     are attached
   * @return a fully functional Agent
   */
  public @NonNull Interactable toInteractable(
      @NonNull Responder responder,
      @NonNull String model,
      @NonNull List<FunctionTool<?>> availableTools) {

    Agent.Builder builder =
        Agent.builder()
            .name(name)
            .model(model)
            .instructions(instructions)
            .responder(responder)
            .maxTurns(maxTurns);

    if (temperature != null) builder.temperature(temperature);

    // Resolve tools by name
    if (toolNames != null && !toolNames.isEmpty()) {
      Map<String, FunctionTool<?>> toolMap =
          availableTools.stream().collect(Collectors.toMap(FunctionTool::getName, t -> t, (a, b) -> a));
      for (String toolName : toolNames) {
        FunctionTool<?> tool = toolMap.get(toolName);
        if (tool != null) {
          builder.addTool(tool);
        } else {
          log.warn("Tool '{}' requested by AgentDefinition '{}' not found in available tools",
              toolName, name);
        }
      }
    }

    // Resolve guardrails by registry ID
    if (inputGuardrails != null) {
      for (String id : inputGuardrails) {
        InputGuardrail g = GuardrailRegistry.getInput(id);
        if (g != null) {
          builder.addInputGuardrail(g);
        } else {
          log.warn("Input guardrail '{}' not found in GuardrailRegistry", id);
        }
      }
    }
    if (outputGuardrails != null) {
      for (String id : outputGuardrails) {
        OutputGuardrail g = GuardrailRegistry.getOutput(id);
        if (g != null) {
          builder.addOutputGuardrail(g);
        } else {
          log.warn("Output guardrail '{}' not found in GuardrailRegistry", id);
        }
      }
    }

    // Handoffs (recursive — nested agents use the same responder + model)
    if (handoffs != null) {
      for (HandoffAgentDef hd : handoffs) {
        Interactable target = hd.target().toInteractable(responder, model, availableTools);
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
      builder.contextManagement(contextManagement.toContextBlueprint().toConfig(responder));
    }

    return builder.build();
  }

  /**
   * Reconstructs a fully functional {@link Interactable} agent without tools.
   *
   * <p>Convenience overload for agents that don't use tools. Any tool names in the definition
   * are ignored.
   *
   * @param responder the Responder for LLM API calls
   * @param model the LLM model identifier
   * @return a fully functional Agent
   */
  public @NonNull Interactable toInteractable(
      @NonNull Responder responder, @NonNull String model) {
    return toInteractable(responder, model, List.of());
  }

  /**
   * Converts this definition to an {@link AgentBlueprint}, bridging to the blueprint
   * serialization system.
   *
   * <p>Requires a {@link ResponderBlueprint} and a model because blueprints are self-contained.
   *
   * @param responderBlueprint the responder configuration
   * @param model the LLM model identifier
   * @param availableTools tools to resolve tool names to class names
   * @return an AgentBlueprint equivalent
   */
  public @NonNull AgentBlueprint toBlueprint(
      @NonNull ResponderBlueprint responderBlueprint,
      @NonNull String model,
      @NonNull List<FunctionTool<?>> availableTools) {

    // Resolve tool names → class names
    Map<String, FunctionTool<?>> toolMap =
        availableTools.stream().collect(Collectors.toMap(FunctionTool::getName, t -> t, (a, b) -> a));
    List<String> toolClassNames = new ArrayList<>();
    if (toolNames != null) {
      for (String toolName : toolNames) {
        FunctionTool<?> tool = toolMap.get(toolName);
        if (tool != null) {
          toolClassNames.add(tool.getClass().getName());
        }
      }
    }

    // Guardrails → GuardrailReference
    List<GuardrailReference> inputRefs = new ArrayList<>();
    if (inputGuardrails != null) {
      for (String id : inputGuardrails) {
        inputRefs.add(new GuardrailReference(null, id));
      }
    }
    List<GuardrailReference> outputRefs = new ArrayList<>();
    if (outputGuardrails != null) {
      for (String id : outputGuardrails) {
        outputRefs.add(new GuardrailReference(null, id));
      }
    }

    // Handoffs
    List<HandoffDescriptor> handoffDescriptors = new ArrayList<>();
    if (handoffs != null) {
      for (HandoffAgentDef hd : handoffs) {
        handoffDescriptors.add(
            new HandoffDescriptor(
                hd.name(),
                hd.description(),
                hd.target().toBlueprint(responderBlueprint, model, availableTools)));
      }
    }

    return new AgentBlueprint(
        name,
        model,
        instructions,
        maxTurns,
        temperature,
        null, // outputType
        null, // traceMetadata
        responderBlueprint,
        toolClassNames,
        handoffDescriptors,
        inputRefs,
        outputRefs,
        contextManagement != null ? contextManagement.toContextBlueprint() : null);
  }

  /**
   * Creates an {@link AgentDefinition} from an existing {@link AgentBlueprint}.
   *
   * <p>Infrastructure fields (model, responder) are stripped; behavioral fields are preserved.
   * Tool class names are reverse-looked-up against the provided tools to recover human-readable
   * names. If a tool class name can't be resolved, it is skipped.
   *
   * @param blueprint the source blueprint
   * @param availableTools tools for reverse name lookup
   * @return an AgentDefinition with only behavioral fields
   */
  public static @NonNull AgentDefinition fromBlueprint(
      @NonNull AgentBlueprint blueprint, @NonNull List<FunctionTool<?>> availableTools) {

    // Reverse-lookup: class name → tool name
    Map<String, String> classToName =
        availableTools.stream()
            .collect(Collectors.toMap(t -> t.getClass().getName(), FunctionTool::getName, (a, b) -> a));

    List<String> toolNamesList = new ArrayList<>();
    for (String fqcn : blueprint.toolClassNames()) {
      String resolved = classToName.get(fqcn);
      if (resolved != null) {
        toolNamesList.add(resolved);
      }
    }

    // Guardrails → registry IDs
    List<String> inputIds = new ArrayList<>();
    for (GuardrailReference ref : blueprint.inputGuardrails()) {
      if (ref.registryId() != null) {
        inputIds.add(ref.registryId());
      }
    }
    List<String> outputIds = new ArrayList<>();
    for (GuardrailReference ref : blueprint.outputGuardrails()) {
      if (ref.registryId() != null) {
        outputIds.add(ref.registryId());
      }
    }

    // Handoffs
    List<HandoffAgentDef> handoffDefs = new ArrayList<>();
    for (HandoffDescriptor hd : blueprint.handoffs()) {
      if (hd.target() instanceof AgentBlueprint targetBp) {
        handoffDefs.add(
            new HandoffAgentDef(
                hd.name(), hd.description(), fromBlueprint(targetBp, availableTools)));
      }
    }

    // Context
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
        blueprint.instructions(),
        blueprint.maxTurns(),
        blueprint.temperature(),
        toolNamesList.isEmpty() ? null : toolNamesList,
        inputIds.isEmpty() ? null : inputIds,
        outputIds.isEmpty() ? null : outputIds,
        handoffDefs.isEmpty() ? null : handoffDefs,
        contextDef);
  }

  /**
   * Creates an {@link AgentDefinition} from an existing {@link AgentBlueprint} without tool
   * name resolution.
   *
   * @param blueprint the source blueprint
   * @return an AgentDefinition (tool names will be empty since they can't be resolved)
   */
  public static @NonNull AgentDefinition fromBlueprint(@NonNull AgentBlueprint blueprint) {
    return fromBlueprint(blueprint, List.of());
  }
}
