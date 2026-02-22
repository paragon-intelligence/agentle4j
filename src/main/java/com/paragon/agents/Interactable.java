package com.paragon.agents;

import com.paragon.prompts.Prompt;
import com.paragon.responses.TraceMetadata;
import com.paragon.responses.spec.File;
import com.paragon.responses.spec.Image;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.ResponseInputItem;
import com.paragon.responses.spec.Text;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Common interface for all agent patterns that can process input and produce output.
 *
 * <p>This interface enables polymorphic usage of different agent patterns, allowing services to
 * work with any agent type without knowing the specific implementation:
 *
 * <ul>
 *   <li>{@link Agent} - Single agent with tools and handoffs
 *   <li>{@link AgentNetwork} - Peer-to-peer agent discussion
 *   <li>{@link SupervisorAgent} - Coordinator with worker agents
 *   <li>{@link ParallelAgents} - Concurrent agent execution
 *   <li>{@link RouterAgent} - Classification and routing
 *   <li>{@link HierarchicalAgents} - Multi-level organizational structure
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Service that works with any agent pattern
 * public class AgentService {
 *     private final Interactable agent;
 *
 *     public AgentService(Interactable agent) {
 *         this.agent = agent;
 *     }
 *
 *     public String process(String input) {
 *         AgentResult result = agent.interact(input);
 *         return result.output();
 *     }
 * }
 *
 * // Can inject any agent type
 * new AgentService(singleAgent);
 * new AgentService(supervisorAgent);
 * new AgentService(agentNetwork);
 * }</pre>
 *
 * <h2>Structured Output</h2>
 *
 * <p>For agents that return structured output, use {@link Structured}:
 *
 * <pre>{@code
 * Interactable.Structured<Person> agent = ...;
 * StructuredAgentResult<Person> result = agent.interactStructured("Extract person info");
 * Person person = result.output();
 * }</pre>
 *
 * @see Agent
 * @see AgentNetwork
 * @see SupervisorAgent
 * @see ParallelAgents
 * @see RouterAgent
 * @see HierarchicalAgents
 * @since 1.0
 */
public interface Interactable {

  /**
   * Returns the name of this interactable.
   *
   * <p>The name is used for identification in multi-agent systems, logging, and user-facing
   * messages. It should be concise and descriptive.
   *
   * @return the name of this interactable
   */
  @NonNull String name();

  /**
   * Creates a serializable blueprint of this interactable.
   *
   * <p>The blueprint captures all declarative configuration and can be serialized to JSON via
   * Jackson. Use {@link InteractableBlueprint#toInteractable()} to reconstruct a fully functional
   * interactable from the blueprint — no external dependencies needed.
   *
   * <pre>{@code
   * InteractableBlueprint blueprint = agent.toBlueprint();
   * String json = objectMapper.writeValueAsString(blueprint);
   *
   * InteractableBlueprint restored = objectMapper.readValue(json, InteractableBlueprint.class);
   * Interactable agent = restored.toInteractable();
   * }</pre>
   *
   * @return a serializable blueprint of this interactable
   * @throws UnsupportedOperationException if the implementation does not support blueprints
   */
  @NonNull
  default InteractableBlueprint toBlueprint() {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + " does not support blueprint serialization");
  }

  /**
   * Interacts with the agent using a text input.
   *
   * <p>Default implementation creates a fresh context with the input as a user message.
   *
   * @param input the user's text input
   * @return the agent's result
   */
  @NonNull
  default AgentResult interact(@NonNull String input) {
    return interact(input, null);
  }

  /**
   * Interacts with the agent using a text input with optional trace metadata.
   *
   * <p>Default implementation creates a fresh context with the input as a user message.
   *
   * @param input the user's text input
   * @param trace optional trace metadata (overrides agent-level configuration)
   * @return the agent's result
   */
  @NonNull
  default AgentResult interact(@NonNull String input, @Nullable TraceMetadata trace) {
    AgenticContext context = AgenticContext.create();
    context.addInput(Message.user(input));
    return interact(context, trace);
  }

  /**
   * Interacts with the agent using Text content.
   *
   * <p>Default implementation creates a fresh context with the text as a user message.
   *
   * @param text the text content
   * @return the agent's result
   */
  @NonNull
  default AgentResult interact(@NonNull Text text) {
    return interact(text, null);
  }

  /**
   * Interacts with the agent using Text content with optional trace metadata.
   *
   * <p>Default implementation creates a fresh context with the text as a user message.
   *
   * @param text the text content
   * @param trace optional trace metadata (overrides agent-level configuration)
   * @return the agent's result
   */
  @NonNull
  default AgentResult interact(@NonNull Text text, @Nullable TraceMetadata trace) {
    AgenticContext context = AgenticContext.create();
    context.addInput(Message.user(text));
    return interact(context, trace);
  }

  /**
   * Interacts with the agent using a Message.
   *
   * <p>Default implementation creates a fresh context with the message.
   *
   * @param message the message input
   * @return the agent's result
   */
  @NonNull
  default AgentResult interact(@NonNull Message message) {
    return interact(message, null);
  }

  /**
   * Interacts with the agent using a Message with optional trace metadata.
   *
   * <p>Default implementation creates a fresh context with the message.
   *
   * @param message the message input
   * @param trace optional trace metadata (overrides agent-level configuration)
   * @return the agent's result
   */
  @NonNull
  default AgentResult interact(@NonNull Message message, @Nullable TraceMetadata trace) {
    AgenticContext context = AgenticContext.create();
    context.addInput(message);
    return interact(context, trace);
  }

  /**
   * Interacts with the agent using a Prompt.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt the prompt input
   * @return the agent's result
   */
  @NonNull
  default AgentResult interact(@NonNull Prompt prompt) {
    return interact(prompt, null);
  }

  /**
   * Interacts with the agent using a Prompt with optional trace metadata.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt the prompt input
   * @param trace optional trace metadata (overrides agent-level configuration)
   * @return the agent's result
   */
  @NonNull
  default AgentResult interact(@NonNull Prompt prompt, @Nullable TraceMetadata trace) {
    return interact(prompt.text(), trace);
  }

  /**
   * Interacts with the agent using a file. Creates a fresh context.
   *
   * <p>Default implementation creates a fresh context with the file as a user message.
   *
   * @param file the file input
   * @return the agent's result
   */
  @NonNull
  default AgentResult interact(@NonNull File file) {
    return interact(file, null);
  }

  /**
   * Interacts with the agent using a file with optional trace metadata.
   *
   * <p>Default implementation creates a fresh context with the file as a user message.
   *
   * @param file the file input
   * @param trace optional trace metadata (overrides agent-level configuration)
   * @return the agent's result
   */
  @NonNull
  default AgentResult interact(@NonNull File file, @Nullable TraceMetadata trace) {
    AgenticContext context = AgenticContext.create();
    context.addInput(Message.user(file));
    return interact(context, trace);
  }

  /**
   * Interacts with the agent using an image. Creates a fresh context.
   *
   * <p>Default implementation creates a fresh context with the image as a user message.
   *
   * @param image the image input
   * @return the agent's result
   */
  @NonNull
  default AgentResult interact(@NonNull Image image) {
    return interact(image, null);
  }

  /**
   * Interacts with the agent using an image with optional trace metadata.
   *
   * <p>Default implementation creates a fresh context with the image as a user message.
   *
   * @param image the image input
   * @param trace optional trace metadata (overrides agent-level configuration)
   * @return the agent's result
   */
  @NonNull
  default AgentResult interact(@NonNull Image image, @Nullable TraceMetadata trace) {
    AgenticContext context = AgenticContext.create();
    context.addInput(Message.user(image));
    return interact(context, trace);
  }

  /**
   * Interacts with the agent using a ResponseInputItem. Creates a fresh context.
   *
   * <p>Default implementation creates a fresh context with the input item.
   *
   * @param input the input item
   * @return the agent's result
   */
  @NonNull
  default AgentResult interact(@NonNull ResponseInputItem input) {
    return interact(input, null);
  }

  /**
   * Interacts with the agent using a ResponseInputItem with optional trace metadata.
   *
   * <p>Default implementation creates a fresh context with the input item.
   *
   * @param input the input item
   * @param trace optional trace metadata (overrides agent-level configuration)
   * @return the agent's result
   */
  @NonNull
  default AgentResult interact(@NonNull ResponseInputItem input, @Nullable TraceMetadata trace) {
    AgenticContext context = AgenticContext.create();
    context.addInput(input);
    return interact(context, trace);
  }

  /**
   * Interacts with the agent using multiple inputs. Creates a fresh context.
   *
   * <p>Default implementation creates a fresh context and adds all input items.
   *
   * @param input the input items
   * @return the agent's result
   */
  @NonNull
  default AgentResult interact(java.util.@NonNull List<ResponseInputItem> input) {
    return interact(input, null);
  }

  /**
   * Interacts with the agent using multiple inputs with optional trace metadata.
   *
   * <p>Default implementation creates a fresh context and adds all input items.
   *
   * @param input the input items
   * @param trace optional trace metadata (overrides agent-level configuration)
   * @return the agent's result
   */
  @NonNull
  default AgentResult interact(
      java.util.@NonNull List<ResponseInputItem> input, @Nullable TraceMetadata trace) {
    AgenticContext context = AgenticContext.create();
    for (ResponseInputItem item : input) {
      context.addInput(item);
    }
    return interact(context, trace);
  }

  /**
   * Interacts with the agent using an existing context.
   *
   * @param context the conversation context containing history
   * @return the agent's result
   */
  @NonNull
  default AgentResult interact(@NonNull AgenticContext context) {
    return interact(context, null);
  }

  /**
   * Interacts with the agent using an existing context with optional trace metadata.
   *
   * <p>This is the main interact method that all other overloads delegate to.
   *
   * @param context the conversation context containing history
   * @param trace optional trace metadata (overrides agent-level configuration)
   * @return the agent's result
   */
  @NonNull AgentResult interact(@NonNull AgenticContext context, @Nullable TraceMetadata trace);

  /**
   * Interacts with the agent with streaming support.
   *
   * <p>Default implementation creates a fresh context with the input as a user message.
   *
   * @param input the user's text input
   * @return an AgentStream for processing streaming events
   */
  @NonNull
  default AgentStream interactStream(@NonNull String input) {
    return interactStream(input, null);
  }

  /**
   * Interacts with the agent with streaming support and trace metadata.
   *
   * <p>Default implementation creates a fresh context with the input as a user message.
   *
   * @param input the user's text input
   * @param trace optional trace metadata (overrides agent-level configuration)
   * @return an AgentStream for processing streaming events
   */
  @NonNull
  default AgentStream interactStream(@NonNull String input, @Nullable TraceMetadata trace) {
    AgenticContext context = AgenticContext.create();
    context.addInput(Message.user(input));
    return interactStream(context, trace);
  }

  /**
   * Interacts with the agent with streaming using a Prompt.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt the prompt input
   * @return an AgentStream for processing streaming events
   */
  @NonNull
  default AgentStream interactStream(@NonNull Prompt prompt) {
    return interactStream(prompt, null);
  }

  /**
   * Interacts with the agent with streaming using a Prompt and trace metadata.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt the prompt input
   * @param trace optional trace metadata (overrides agent-level configuration)
   * @return an AgentStream for processing streaming events
   */
  @NonNull
  default AgentStream interactStream(@NonNull Prompt prompt, @Nullable TraceMetadata trace) {
    return interactStream(prompt.text(), trace);
  }

  /**
   * Interacts with the agent with streaming using an existing context.
   *
   * @param context the conversation context containing history
   * @return an AgentStream for processing streaming events
   */
  @NonNull
  default AgentStream interactStream(@NonNull AgenticContext context) {
    return interactStream(context, null);
  }

  /**
   * Interacts with the agent with streaming using an existing context and trace metadata.
   *
   * <p>This is the main streaming method that all other streaming overloads delegate to.
   *
   * @param context the conversation context containing history
   * @param trace optional trace metadata (overrides agent-level configuration)
   * @return an AgentStream for processing streaming events
   */
  @NonNull AgentStream interactStream(
      @NonNull AgenticContext context, @Nullable TraceMetadata trace);

  /**
   * Extended interface for agents that return structured (typed) output.
   *
   * <p>Use this interface when you need type-safe parsed output from agents configured with
   * structured output schemas.
   *
   * <h2>Example</h2>
   *
   * <pre>{@code
   * record Person(String name, int age) {}
   *
   * // Create a structured agent
   * Interactable.Structured<Person> agent = Agent.builder()
   *     .name("PersonExtractor")
   *     .instructions("Extract person info from text")
   *     .structured(Person.class)
   *     .responder(responder)
   *     .build();
   *
   * // Get typed result
   * StructuredAgentResult<Person> result = agent.interactStructured("John is 30 years old");
   * Person person = result.output();
   * }</pre>
   *
   * @param <T> the type of the structured output
   */
  interface Structured<T> extends Interactable {

    /**
     * Interacts with the agent and returns a structured result.
     *
     * <p>Default implementation creates a fresh context with the input as a user message.
     *
     * @param input the user's text input
     * @return the structured result with parsed output
     */
    @NonNull
    default StructuredAgentResult<T> interactStructured(@NonNull String input) {
      return interactStructured(input, null);
    }

    /**
     * Interacts with the agent and returns a structured result with trace metadata.
     *
     * <p>Default implementation creates a fresh context with the input as a user message.
     *
     * @param input the user's text input
     * @param trace optional trace metadata (overrides agent-level configuration)
     * @return the structured result with parsed output
     */
    @NonNull
    default StructuredAgentResult<T> interactStructured(
        @NonNull String input, @Nullable TraceMetadata trace) {
      AgenticContext context = AgenticContext.create();
      context.addInput(Message.user(input));
      return interactStructured(context, trace);
    }

    /**
     * Interacts with the agent with Text content and returns a structured result.
     *
     * <p>Default implementation creates a fresh context with the text as a user message.
     *
     * @param text the text content
     * @return the structured result with parsed output
     */
    @NonNull
    default StructuredAgentResult<T> interactStructured(@NonNull Text text) {
      return interactStructured(text, null);
    }

    /**
     * Interacts with the agent with Text content and returns a structured result with trace
     * metadata.
     *
     * <p>Default implementation creates a fresh context with the text as a user message.
     *
     * @param text the text content
     * @param trace optional trace metadata (overrides agent-level configuration)
     * @return the structured result with parsed output
     */
    @NonNull
    default StructuredAgentResult<T> interactStructured(
        @NonNull Text text, @Nullable TraceMetadata trace) {
      AgenticContext context = AgenticContext.create();
      context.addInput(Message.user(text));
      return interactStructured(context, trace);
    }

    /**
     * Interacts with the agent with a Message and returns a structured result.
     *
     * <p>Default implementation creates a fresh context with the message.
     *
     * @param message the message input
     * @return the structured result with parsed output
     */
    @NonNull
    default StructuredAgentResult<T> interactStructured(@NonNull Message message) {
      return interactStructured(message, null);
    }

    /**
     * Interacts with the agent with a Message and returns a structured result with trace metadata.
     *
     * <p>Default implementation creates a fresh context with the message.
     *
     * @param message the message input
     * @param trace optional trace metadata (overrides agent-level configuration)
     * @return the structured result with parsed output
     */
    @NonNull
    default StructuredAgentResult<T> interactStructured(
        @NonNull Message message, @Nullable TraceMetadata trace) {
      AgenticContext context = AgenticContext.create();
      context.addInput(message);
      return interactStructured(context, trace);
    }

    /**
     * Interacts with the agent with a Prompt and returns a structured result.
     *
     * <p>The prompt's text content is extracted and used as the input.
     *
     * @param prompt the prompt input
     * @return the structured result with parsed output
     */
    @NonNull
    default StructuredAgentResult<T> interactStructured(@NonNull Prompt prompt) {
      return interactStructured(prompt, null);
    }

    /**
     * Interacts with the agent with a Prompt and returns a structured result with trace metadata.
     *
     * <p>The prompt's text content is extracted and used as the input.
     *
     * @param prompt the prompt input
     * @param trace optional trace metadata (overrides agent-level configuration)
     * @return the structured result with parsed output
     */
    @NonNull
    default StructuredAgentResult<T> interactStructured(
        @NonNull Prompt prompt, @Nullable TraceMetadata trace) {
      return interactStructured(prompt.text(), trace);
    }

    /**
     * Interacts with the agent with an existing context and returns a structured result.
     *
     * @param context the conversation context
     * @return the structured result with parsed output
     */
    @NonNull
    default StructuredAgentResult<T> interactStructured(@NonNull AgenticContext context) {
      return interactStructured(context, null);
    }

    /**
     * Interacts with the agent with an existing context and returns a structured result with trace
     * metadata.
     *
     * <p>This is the main structured method that all other structured overloads delegate to.
     *
     * @param context the conversation context
     * @param trace optional trace metadata (overrides agent-level configuration)
     * @return the structured result with parsed output
     */
    @NonNull StructuredAgentResult<T> interactStructured(
        @NonNull AgenticContext context, @Nullable TraceMetadata trace);
  }
}
