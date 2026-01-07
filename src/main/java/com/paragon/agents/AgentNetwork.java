package com.paragon.agents;

import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.ResponseInputItem;
import com.paragon.responses.spec.Text;
import com.paragon.telemetry.processors.TraceIdGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.StructuredTaskScope;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
public final class AgentNetwork {

  private final @NonNull List<Agent> peers;
  private final int maxRounds;
  private final @Nullable Agent synthesizer;

  private AgentNetwork(Builder builder) {
    this.peers = List.copyOf(builder.peers);
    this.maxRounds = builder.maxRounds;
    this.synthesizer = builder.synthesizer;

    if (peers.size() < 2) {
      throw new IllegalArgumentException("At least two peers are required for a network");
    }
  }

  /** Creates a new AgentNetwork builder. */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Returns the peer agents in this network.
   *
   * @return unmodifiable list of peers
   */
  public @NonNull List<Agent> peers() {
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
   * Returns the synthesizer agent if configured.
   *
   * @return the synthesizer agent, or null if not set
   */
  @Nullable Agent getSynthesizer() {
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
    AgentContext context = AgentContext.create();
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
    AgentContext context = AgentContext.create();
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
    AgentContext context = AgentContext.create();
    context.addInput(message);
    return discuss(context);
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
  public @NonNull NetworkResult discuss(@NonNull AgentContext context) {
    Objects.requireNonNull(context, "context cannot be null");

    // Ensure trace correlation
    String parentTraceId = TraceIdGenerator.generateTraceId();
    String parentSpanId = TraceIdGenerator.generateSpanId();
    context.withTraceContext(parentTraceId, parentSpanId);

    // Extract original topic for synthesis
    String originalTopic = extractLastUserMessage(context);

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

    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
      List<StructuredTaskScope.Subtask<Contribution>> subtasks = new ArrayList<>();

      for (Agent peer : peers) {
        AgentContext ctx = AgentContext.create();
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
      scope.throwIfFailed();

      List<Contribution> contributions = new ArrayList<>();
      for (var subtask : subtasks) {
        contributions.add(subtask.get());
      }
      return contributions;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Broadcast interrupted", e);
    } catch (java.util.concurrent.ExecutionException e) {
      throw new RuntimeException("Broadcast failed", e.getCause() != null ? e.getCause() : e);
    }
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
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(topic));
    return discussStream(context);
  }

  /**
   * Initiates a streaming discussion using an existing context.
   *
   * @param context the context with discussion history
   * @return a NetworkStream for processing streaming events
   */
  public @NonNull NetworkStream discussStream(@NonNull AgentContext context) {
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
    AgentContext context = AgentContext.create();
    context.addInput(Message.user(message));
    return new NetworkStream(this, context, NetworkStream.Mode.BROADCAST);
  }

  private List<Contribution> runDiscussionRounds(
      AgentContext sharedContext, String parentTraceId, String parentSpanId) {

    List<Contribution> allContributions = Collections.synchronizedList(new ArrayList<>());

    // Run rounds sequentially
    for (int round = 1; round <= maxRounds; round++) {
      // Within each round, agents contribute sequentially so they can see previous contributions
      for (Agent peer : peers) {
        // Build context with discussion history and role-specific prompt
        AgentContext peerContext = buildPeerContext(sharedContext, peer, round);
        peerContext.withTraceContext(parentTraceId, parentSpanId);

        AgentResult result = peer.interact(peerContext);
        String output = result.output();
        boolean isError = result.isError();

        // Add contribution to shared context for next agents
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

  private AgentContext buildPeerContext(AgentContext sharedContext, Agent peer, int round) {
    AgentContext peerContext = sharedContext.copy();

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
      synthPrompt.append("**").append(c.agent().name()).append("** (Round ").append(c.round());
      synthPrompt.append("): ");
      if (c.isError()) {
        synthPrompt.append("[Error occurred]\n");
      } else {
        synthPrompt.append(c.output()).append("\n");
      }
      synthPrompt.append("\n");
    }

    synthPrompt.append("Please synthesize these viewpoints into a coherent summary.");

    AgentContext synthContext = AgentContext.create();
    synthContext.addInput(Message.user(synthPrompt.toString()));
    return synthesizer.interact(synthContext);
  }

  private String extractLastUserMessage(AgentContext context) {
    List<ResponseInputItem> history = context.getHistory();
    for (int i = history.size() - 1; i >= 0; i--) {
      ResponseInputItem item = history.get(i);
      if (item instanceof Message msg && "user".equals(msg.role())) {
        if (msg.content() != null) {
          for (var content : msg.content()) {
            if (content instanceof Text text) {
              return text.text();
            }
          }
        }
      }
    }
    return "[No topic provided]";
  }

  /** Represents a contribution from a peer agent. */
  public record Contribution(
      @NonNull Agent agent, int round, @Nullable String output, boolean isError) {
    public Contribution {
      Objects.requireNonNull(agent, "agent cannot be null");
      if (round < 1) {
        throw new IllegalArgumentException("round must be at least 1");
      }
    }
  }

  /** Result of a network discussion. */
  public record NetworkResult(
      @NonNull List<Contribution> contributions, @Nullable String synthesis) {
    public NetworkResult {
      Objects.requireNonNull(contributions, "contributions cannot be null");
    }

    /** Returns all contributions from a specific agent. */
    public @NonNull List<Contribution> contributionsFrom(@NonNull Agent agent) {
      Objects.requireNonNull(agent, "agent cannot be null");
      return contributions.stream().filter(c -> c.agent().equals(agent)).toList();
    }

    /** Returns all contributions from a specific round. */
    public @NonNull List<Contribution> contributionsFromRound(int round) {
      return contributions.stream().filter(c -> c.round() == round).toList();
    }

    /** Returns the final contribution (last in sequence). */
    public @Nullable Contribution lastContribution() {
      return contributions.isEmpty() ? null : contributions.get(contributions.size() - 1);
    }
  }

  /** Builder for AgentNetwork. */
  public static final class Builder {
    private final List<Agent> peers = new ArrayList<>();
    private int maxRounds = 2;
    private @Nullable Agent synthesizer;

    private Builder() {}

    /**
     * Adds a peer agent to the network.
     *
     * @param peer the agent to add
     * @return this builder
     */
    public @NonNull Builder addPeer(@NonNull Agent peer) {
      Objects.requireNonNull(peer, "peer cannot be null");
      this.peers.add(peer);
      return this;
    }

    /**
     * Adds multiple peer agents to the network.
     *
     * @param peers the agents to add
     * @return this builder
     */
    public @NonNull Builder addPeers(@NonNull Agent... peers) {
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
     * Sets an optional synthesizer agent that combines all contributions.
     *
     * @param synthesizer the synthesizer agent
     * @return this builder
     */
    public @NonNull Builder synthesizer(@NonNull Agent synthesizer) {
      this.synthesizer = Objects.requireNonNull(synthesizer);
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
