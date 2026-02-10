package com.paragon.agents;

import com.paragon.prompts.Prompt;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.Text;
import org.jspecify.annotations.NonNull;

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
  @NonNull
  String name();

  /**
   * Interacts with the agent using a text input.
   *
   * @param input the user's text input
   * @return the agent's result
   */
  @NonNull
  AgentResult interact(@NonNull String input);

  /**
   * Interacts with the agent using Text content.
   *
   * @param text the text content
   * @return the agent's result
   */
  @NonNull
  AgentResult interact(@NonNull Text text);

  /**
   * Interacts with the agent using a Message.
   *
   * @param message the message input
   * @return the agent's result
   */
  @NonNull
  AgentResult interact(@NonNull Message message);

  /**
   * Interacts with the agent using a Prompt.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt the prompt input
   * @return the agent's result
   */
  @NonNull
  AgentResult interact(@NonNull Prompt prompt);

  /**
   * Interacts with the agent using an existing context.
   *
   * @param context the conversation context containing history
   * @return the agent's result
   */
  @NonNull
  AgentResult interact(@NonNull AgenticContext context);

  /**
   * Interacts with the agent with streaming support.
   *
   * @param input the user's text input
   * @return an AgentStream for processing streaming events
   */
  @NonNull
  AgentStream interactStream(@NonNull String input);

  /**
   * Interacts with the agent with streaming using a Prompt.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt the prompt input
   * @return an AgentStream for processing streaming events
   */
  @NonNull
  AgentStream interactStream(@NonNull Prompt prompt);

  /**
   * Interacts with the agent with streaming using an existing context.
   *
   * @param context the conversation context containing history
   * @return an AgentStream for processing streaming events
   */
  @NonNull
  AgentStream interactStream(@NonNull AgenticContext context);

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
     * @param input the user's text input
     * @return the structured result with parsed output
     */
    @NonNull
    StructuredAgentResult<T> interactStructured(@NonNull String input);

    /**
     * Interacts with the agent with Text content and returns a structured result.
     *
     * @param text the text content
     * @return the structured result with parsed output
     */
    @NonNull
    StructuredAgentResult<T> interactStructured(@NonNull Text text);

    /**
     * Interacts with the agent with a Message and returns a structured result.
     *
     * @param message the message input
     * @return the structured result with parsed output
     */
    @NonNull
    StructuredAgentResult<T> interactStructured(@NonNull Message message);

    /**
     * Interacts with the agent with a Prompt and returns a structured result.
     *
     * @param prompt the prompt input
     * @return the structured result with parsed output
     */
    @NonNull
    StructuredAgentResult<T> interactStructured(@NonNull Prompt prompt);

    /**
     * Interacts with the agent with an existing context and returns a structured result.
     *
     * @param context the conversation context
     * @return the structured result with parsed output
     */
    @NonNull
    StructuredAgentResult<T> interactStructured(@NonNull AgenticContext context);
  }
}
