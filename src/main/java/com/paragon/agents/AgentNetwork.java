package com.paragon.agents;

import com.paragon.prompts.Prompt;
import com.paragon.responses.TraceMetadata;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.ResponseInputItem;
import com.paragon.responses.spec.Text;
import com.paragon.telemetry.processors.TraceIdGenerator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.StructuredTaskScope;

/**
 * Implements the Network pattern: decentralized peer-to-peer agent communication.
 *
 * <p>Unlike {@link SupervisorAgent} which has a central coordinator, or {@link ParallelAgents}
 * which runs agents independently, AgentNetwork enables agents to communicate with each other in
 * rounds, building on each other's contributions.
 *
 * <p>Key characteristics:
 *
 * <ul>
 *   <li>No central coordinator - agents are peers
 *   <li>Each agent sees previous agents' contributions
 *   <li>Communication happens in rounds until convergence or max rounds
 *   <li>Resilient - failure of one agent doesn't cripple the network
 * </ul>
 *
 * <p><b>Virtual Thread Design:</b> Uses synchronous API optimized for Java 21+ virtual threads.
 * Blocking calls are cheap and efficient with virtual threads.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * Agent optimist = Agent.builder().name("Optimist")
 *     .instructions("Argue the positive aspects").build();
 * Agent pessimist = Agent.builder().name("Pessimist")
 *     .instructions("Argue the negative aspects").build();
 * Agent moderate = Agent.builder().name("Moderate")
 *     .instructions("Find balanced middle ground").build();
 *
 * AgentNetwork network = AgentNetwork.builder()
 *     .addPeer(optimist)
 *     .addPeer(pessimist)
 *     .addPeer(moderate)
 *     .maxRounds(3)
 *     .build();
 *
 * // Agents discuss in rounds, each seeing previous contributions
 * NetworkResult result = network.discuss("Should we adopt AI widely?");
 * result.contributions().forEach(c ->
 *     System.out.println(c.agent().name() + ": " + c.output()));
 * }</pre>
 *
 * @since 1.0
 */
public final class AgentNetwork implements Interactable {

  private final @NonNull String name;
  private final @NonNull List<Interactable> peers;
  private final int maxRounds;
  private final @Nullable Interactable synthesizer;
  private final @Nullable TraceMetadata traceMetadata;

  private AgentNetwork(Builder builder) {
    this.name = builder.name != null ? builder.name : "AgentNetwork";
    this.peers = List.copyOf(builder.peers);
    this.maxRounds = builder.maxRounds;
    this.synthesizer = builder.synthesizer;
    this.traceMetadata = builder.traceMetadata;

    if (peers.size() < 2) {
      throw new IllegalArgumentException("At least two peers are required for a network");
    }
  }

  /**
   * Creates a new AgentNetwork builder.
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String name() {
    return name;
  }

  /**
   * Returns the peers in this network.
   *
   * @return unmodifiable list of peers
   */
  public @NonNull List<Interactable> peers() {
    return peers;
  }

  /**
   * Returns the maximum number of discussion rounds.
   *
   * @return the max rounds
   */
  public int maxRounds() {
    return maxRounds;
  }

  /**
   * Returns the synthesizer if configured.
   *
   * @return the synthesizer, or null if not set
   */
  @Nullable
  Interactable getSynthesizer() {
    return synthesizer;
  }

  /**
   * Initiates a discussion among all peer agents.
   *
   * <p>Agents contribute in sequence within each round, with each agent seeing all previous
   * contributions. Discussion continues for the configured number of rounds.
   *
   * @param topic the discussion topic
   * @return the network result containing all contributions
   */
  public @NonNull NetworkResult discuss(@NonNull String topic) {
    Objects.requireNonNull(topic, "topic cannot be null");
    AgenticContext context = AgenticContext.create();
    context.addInput(Message.user(topic));
    return discuss(context);
  }

  /**
   * Initiates a discussion with Text content.
   *
   * @param text the discussion topic
   * @return the network result
   */
  public @NonNull NetworkResult discuss(@NonNull Text text) {
    Objects.requireNonNull(text, "text cannot be null");
    AgenticContext context = AgenticContext.create();
    context.addInput(Message.user(text));
    return discuss(context);
  }

  /**
   * Initiates a discussion with a Message.
   *
   * @param message the discussion message
   * @return the network result
   */
  public @NonNull NetworkResult discuss(@NonNull Message message) {
    Objects.requireNonNull(message, "message cannot be null");
    AgenticContext context = AgenticContext.create();
    context.addInput(message);
    return discuss(context);
  }

  /**
   * Initiates a discussion with a Prompt.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt the discussion prompt
   * @return the network result
   */
  public @NonNull NetworkResult discuss(@NonNull Prompt prompt) {
    Objects.requireNonNull(prompt, "prompt cannot be null");
    return discuss(prompt.text());
  }

  /**
   * Initiates a discussion using an existing context.
   *
   * <p>This is the core discuss method. The context is shared across all agents, building up a
   * conversation history as agents contribute.
   *
   * @param context the context with discussion history
   * @return the network result
   */
  public @NonNull NetworkResult discuss(@NonNull AgenticContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    // Ensure trace correlation
    String parentTraceId = TraceIdGenerator.generateTraceId();
    String parentSpanId = TraceIdGenerator.generateSpanId();
    context.withTraceContext(parentTraceId, parentSpanId);

    // Extract original topic for synthesis
    String originalTopic = context.extractLastUserMessageText("[No topic provided]");

    // Run discussion rounds
    List<Contribution> contributions =
            runDiscussionRounds(context, parentTraceId, parentSpanId);

    // Synthesize if configured
    if (synthesizer != null) {
      AgentResult synthesisResult = synthesizeContributions(contributions, originalTopic);
      return new NetworkResult(contributions, synthesisResult.output());
    }

    return new NetworkResult(contributions, null);
  }

  /**
   * Broadcasts a message to all peers simultaneously and collects responses.
   *
   * <p>Unlike discuss(), broadcast() runs all agents in parallel without sequential visibility.
   * Each agent only sees the original message, not other agents' responses.
   *
   * <p>Uses structured concurrency (Java 21+) for parallel execution.
   *
   * @param message the message to broadcast
   * @return list of contributions
   */
  public @NonNull List<Contribution> broadcast(@NonNull String message) {
    Objects.requireNonNull(message, "message cannot be null");

    String parentTraceId = TraceIdGenerator.generateTraceId();
    String parentSpanId = TraceIdGenerator.generateSpanId();

    try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow())) {
      List<StructuredTaskScope.Subtask<Contribution>> subtasks = new ArrayList<>();

      for (Interactable peer : peers) {
        AgenticContext ctx = AgenticContext.create();
        ctx.addInput(Message.user(message));
        ctx.withTraceContext(parentTraceId, parentSpanId);

        subtasks.add(
                scope.fork(
                        () -> {
                          AgentResult result = peer.interact(ctx);
                          return new Contribution(peer, 1, result.output(), result.isError());
                        }));
      }

      scope.join();

      List<Contribution> contributions = new ArrayList<>();
      for (var subtask : subtasks) {
        contributions.add(subtask.get());
      }
      return contributions;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Broadcast interrupted", e);
    }
  }

  /**
   * Broadcasts a Prompt to all peers simultaneously and collects responses.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt the prompt to broadcast
   * @return list of contributions
   */
  public @NonNull List<Contribution> broadcast(@NonNull Prompt prompt) {
    Objects.requireNonNull(prompt, "prompt cannot be null");
    return broadcast(prompt.text());
  }

  // ===== Streaming Methods =====

  /**
   * Initiates a streaming discussion among all peer agents.
   *
   * <p>Returns a {@link NetworkStream} that allows registering callbacks for text deltas, round
   * progression, and completion events.
   *
   * @param topic the discussion topic
   * @return a NetworkStream for processing streaming events
   */
  public @NonNull NetworkStream discussStream(@NonNull String topic) {
    Objects.requireNonNull(topic, "topic cannot be null");
    AgenticContext context = AgenticContext.create();
    context.addInput(Message.user(topic));
    return discussStream(context);
  }

  /**
   * Initiates a streaming discussion with a Prompt.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt the discussion prompt
   * @return a NetworkStream for processing streaming events
   */
  public @NonNull NetworkStream discussStream(@NonNull Prompt prompt) {
    Objects.requireNonNull(prompt, "prompt cannot be null");
    return discussStream(prompt.text());
  }

  /**
   * Initiates a streaming discussion using an existing context.
   *
   * @param context the context with discussion history
   * @return a NetworkStream for processing streaming events
   */
  public @NonNull NetworkStream discussStream(@NonNull AgenticContext context) {
    Objects.requireNonNull(context, "context cannot be null");
    return new NetworkStream(this, context, NetworkStream.Mode.DISCUSS);
  }

  /**
   * Broadcasts a message to all peers simultaneously with streaming.
   *
   * <p>Unlike discussStream(), broadcastStream() runs all agents in parallel without sequential
   * visibility. Each agent only sees the original message, not other agents' responses.
   *
   * @param message the message to broadcast
   * @return a NetworkStream for processing streaming events
   */
  public @NonNull NetworkStream broadcastStream(@NonNull String message) {
    Objects.requireNonNull(message, "message cannot be null");
    AgenticContext context = AgenticContext.create();
    context.addInput(Message.user(message));
    return new NetworkStream(this, context, NetworkStream.Mode.BROADCAST);
  }

  /**
   * Broadcasts a Prompt to all peers simultaneously with streaming.
   *
   * <p>The prompt's text content is extracted and used as the input.
   *
   * @param prompt the prompt to broadcast
   * @return a NetworkStream for processing streaming events
   */
  public @NonNull NetworkStream broadcastStream(@NonNull Prompt prompt) {
    Objects.requireNonNull(prompt, "prompt cannot be null");
    return broadcastStream(prompt.text());
  }

  private List<Contribution> runDiscussionRounds(
          AgenticContext sharedContext, String parentTraceId, String parentSpanId) {

    List<Contribution> allContributions = Collections.synchronizedList(new ArrayList<>());

    // Run rounds sequentially
    for (int round = 1; round <= maxRounds; round++) {
      // Within each round, peers contribute sequentially so they can see previous contributions
      for (Interactable peer : peers) {
        // Build context with discussion history and role-specific prompt
        AgenticContext peerContext = buildPeerContext(sharedContext, peer, round);
        peerContext.withTraceContext(parentTraceId, parentSpanId);

        AgentResult result = peer.interact(peerContext);
        String output = result.output();
        boolean isError = result.isError();

        // Add contribution to shared context for next peers
        if (!isError && output != null) {
          Message contribution =
                  Message.assistant(Text.valueOf("[" + peer.name() + "]: " + output));
          sharedContext.addInput(contribution);
        }

        allContributions.add(new Contribution(peer, round, output, isError));
      }
    }

    return new ArrayList<>(allContributions);
  }

  private AgenticContext buildPeerContext(AgenticContext sharedContext, Interactable peer, int round) {
    AgenticContext peerContext = sharedContext.copy();

    // Add role reminder for this peer
    String roleReminder =
            String.format(
                    "You are %s participating in round %d of a discussion. "
                            + "Consider the previous contributions and add your unique perspective. "
                            + "Be constructive and build on others' ideas.",
                    peer.name(), round);

    peerContext.addInput(Message.developer(Text.valueOf(roleReminder)));
    return peerContext;
  }

  private AgentResult synthesizeContributions(
          List<Contribution> contributions, String originalTopic) {

    StringBuilder synthPrompt = new StringBuilder();
    synthPrompt.append("Original discussion topic: ").append(originalTopic).append("\n\n");
    synthPrompt.append("The following contributions were made:\n\n");

    for (Contribution c : contributions) {
      synthPrompt.append("**").append(c.peer().name()).append("** (Round ").append(c.round());
      synthPrompt.append("): ");
      if (c.isError()) {
        synthPrompt.append("[Error occurred]\n");
      } else {
        synthPrompt.append(c.output()).append("\n");
      }
      synthPrompt.append("\n");
    }

    synthPrompt.append("Please synthesize these viewpoints into a coherent summary.");

    AgenticContext synthContext = AgenticContext.create();
    synthContext.addInput(Message.user(synthPrompt.toString()));
    return synthesizer.interact(synthContext);
  }

  // ===== Interactable Interface Implementation =====

  /**
   * {@inheritDoc}
   *
   * <p>Runs a discussion and returns an AgentResult. If a synthesizer is configured, returns the
   * synthesized output. Otherwise, returns the last contribution's output.
   */
  @Override
  public @NonNull AgentResult interact(@NonNull AgenticContext context) {
    return toAgentResult(discuss(context));
  }

  /**
   * {@inheritDoc}
   *
   * <p>Runs a discussion and returns an AgentResult. Trace metadata is propagated through peer
   * interactions.
   */
  @Override
  public @NonNull AgentResult interact(@NonNull AgenticContext context, @Nullable TraceMetadata trace) {
    // AgentNetwork doesn't directly use trace metadata; peers handle their own traces
    return toAgentResult(discuss(context));
  }

  /**
   * {@inheritDoc}
   *
   * <p>If a synthesizer is configured, runs the discussion and streams the synthesis. Otherwise,
   * returns a completed stream with the discussion result.
   */
  @Override
  public @NonNull AgentStream interactStream(@NonNull AgenticContext context) {
    if (synthesizer == null) {
      NetworkResult result = discuss(context);
      return AgentStream.completed(toAgentResult(result));
    }
    NetworkResult result = discuss(context);
    String synthesisPrompt = buildSynthesisPrompt(result);
    return synthesizer.interactStream(synthesisPrompt);
  }

  /**
   * {@inheritDoc}
   *
   * <p>If a synthesizer is configured, runs the discussion and streams the synthesis. Trace
   * metadata is propagated through peer interactions.
   */
  @Override
  public @NonNull AgentStream interactStream(@NonNull AgenticContext context, @Nullable TraceMetadata trace) {
    // AgentNetwork doesn't directly use trace metadata; peers handle their own traces
    if (synthesizer == null) {
      NetworkResult result = discuss(context);
      return AgentStream.completed(toAgentResult(result));
    }
    NetworkResult result = discuss(context);
    String synthesisPrompt = buildSynthesisPrompt(result);
    return synthesizer.interactStream(synthesisPrompt);
  }


  private AgentResult toAgentResult(NetworkResult networkResult) {
    String output;
    if (networkResult.synthesis() != null) {
      output = networkResult.synthesis();
    } else {
      Contribution last = networkResult.lastContribution();
      output = last != null ? last.output() : "";
    }
    AgenticContext ctx = AgenticContext.create();
    return AgentResult.success(output, null, ctx, List.of(), 0);
  }

  private String buildSynthesisPrompt(NetworkResult result) {
    StringBuilder sb = new StringBuilder();
    sb.append("The following contributions were made in a discussion:\n\n");
    for (Contribution c : result.contributions()) {
      sb.append("**").append(c.peer().name()).append("** (Round ").append(c.round()).append("): ");
      if (c.isError()) {
        sb.append("[Error occurred]\n");
      } else {
        sb.append(c.output()).append("\n");
      }
      sb.append("\n");
    }
    sb.append("Please synthesize these viewpoints into a coherent summary.");
    return sb.toString();
  }

  /**
   * Represents a contribution from a peer.
   */
  public record Contribution(
          @NonNull Interactable peer, int round, @Nullable String output, boolean isError) {
    public Contribution {
      Objects.requireNonNull(peer, "peer cannot be null");
      if (round < 1) {
        throw new IllegalArgumentException("round must be at least 1");
      }
    }
  }

  /**
   * Result of a network discussion.
   */
  public record NetworkResult(
          @NonNull List<Contribution> contributions, @Nullable String synthesis) {
    public NetworkResult {
      Objects.requireNonNull(contributions, "contributions cannot be null");
    }

    /**
     * Returns all contributions from a specific peer.
     */
    public @NonNull List<Contribution> contributionsFrom(@NonNull Interactable peer) {
      Objects.requireNonNull(peer, "peer cannot be null");
      return contributions.stream().filter(c -> c.peer().equals(peer)).toList();
    }

    /**
     * Returns all contributions from a specific round.
     */
    public @NonNull List<Contribution> contributionsFromRound(int round) {
      return contributions.stream().filter(c -> c.round() == round).toList();
    }

    /**
     * Returns the final contribution (last in sequence).
     */
    public @Nullable Contribution lastContribution() {
      return contributions.isEmpty() ? null : contributions.get(contributions.size() - 1);
    }
  }

  /**
   * Builder for AgentNetwork.
   */
  public static final class Builder {
    private final List<Interactable> peers = new ArrayList<>();
    private @Nullable String name;
    private int maxRounds = 2;
    private @Nullable Interactable synthesizer;
    private @Nullable TraceMetadata traceMetadata;

    private Builder() {
    }

    /**
     * Sets the name for this network.
     *
     * @param name the network name
     * @return this builder
     */
    public @NonNull Builder name(@NonNull String name) {
      this.name = Objects.requireNonNull(name, "name cannot be null");
      return this;
    }

    /**
     * Adds a peer to the network.
     *
     * <p>Peers can be any Interactable: Agent, RouterAgent, ParallelAgents, etc.
     *
     * @param peer the peer to add
     * @return this builder
     */
    public @NonNull Builder addPeer(@NonNull Interactable peer) {
      Objects.requireNonNull(peer, "peer cannot be null");
      this.peers.add(peer);
      return this;
    }

    /**
     * Adds multiple peers to the network.
     *
     * @param peers the peers to add
     * @return this builder
     */
    public @NonNull Builder addPeers(@NonNull Interactable... peers) {
      Objects.requireNonNull(peers, "peers cannot be null");
      this.peers.addAll(Arrays.asList(peers));
      return this;
    }

    /**
     * Sets the maximum number of discussion rounds.
     *
     * <p>In each round, all peers contribute sequentially.
     *
     * @param maxRounds the max rounds (default: 2)
     * @return this builder
     */
    public @NonNull Builder maxRounds(int maxRounds) {
      if (maxRounds < 1) {
        throw new IllegalArgumentException("maxRounds must be at least 1");
      }
      this.maxRounds = maxRounds;
      return this;
    }

    /**
     * Sets an optional synthesizer that combines all contributions.
     *
     * <p>The synthesizer can be any Interactable: Agent, RouterAgent, etc.
     *
     * @param synthesizer the synthesizer
     * @return this builder
     */
    public @NonNull Builder synthesizer(@NonNull Interactable synthesizer) {
      this.synthesizer = Objects.requireNonNull(synthesizer);
      return this;
    }

    /**
     * Sets the trace metadata for API requests (optional).
     *
     * @param trace the trace metadata
     * @return this builder
     */
    public @NonNull Builder traceMetadata(@Nullable TraceMetadata trace) {
      this.traceMetadata = trace;
      return this;
    }

    /**
     * Builds the AgentNetwork.
     *
     * @return the configured network
     */
    public @NonNull AgentNetwork build() {
      return new AgentNetwork(this);
    }
  }
}
