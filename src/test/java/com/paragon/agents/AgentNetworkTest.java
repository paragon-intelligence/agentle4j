package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.Text;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for AgentNetwork.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Builder validation
 *   <li>Peer management
 *   <li>Discussion rounds
 *   <li>Broadcast functionality
 *   <li>Contribution tracking
 *   <li>NetworkResult methods
 * </ul>
 */
@DisplayName("AgentNetwork")
class AgentNetworkTest {

  private MockWebServer mockWebServer;
  private Responder responder;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    responder =
        Responder.builder().baseUrl(mockWebServer.url("/v1/responses")).apiKey("test-key").build();
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Nested
  @DisplayName("Builder")
  class BuilderTests {

    @Test
    @DisplayName("builder() returns non-null builder")
    void builder_returnsBuilder() {
      AgentNetwork.Builder builder = AgentNetwork.builder();
      assertNotNull(builder);
    }

    @Test
    @DisplayName("build() with two peers succeeds")
    void build_withTwoPeers_succeeds() {
      Agent peer1 = createTestAgent("Peer1");
      Agent peer2 = createTestAgent("Peer2");

      AgentNetwork network = AgentNetwork.builder().addPeer(peer1).addPeer(peer2).build();

      assertNotNull(network);
      assertEquals(2, network.peers().size());
    }

    @Test
    @DisplayName("build() with one peer throws exception")
    void build_withOnePeer_throws() {
      Agent peer = createTestAgent("Peer");

      assertThrows(
          IllegalArgumentException.class, () -> AgentNetwork.builder().addPeer(peer).build());
    }

    @Test
    @DisplayName("build() with no peers throws exception")
    void build_withNoPeers_throws() {
      assertThrows(IllegalArgumentException.class, () -> AgentNetwork.builder().build());
    }

    @Test
    @DisplayName("addPeer() validates null peer")
    void addPeer_nullPeer_throws() {
      assertThrows(NullPointerException.class, () -> AgentNetwork.builder().addPeer(null));
    }

    @Test
    @DisplayName("addPeers() adds multiple peers")
    void addPeers_addsMultiplePeers() {
      Agent peer1 = createTestAgent("Peer1");
      Agent peer2 = createTestAgent("Peer2");
      Agent peer3 = createTestAgent("Peer3");

      AgentNetwork network = AgentNetwork.builder().addPeers(peer1, peer2, peer3).build();

      assertEquals(3, network.peers().size());
    }

    @Test
    @DisplayName("maxRounds() rejects invalid values")
    void maxRounds_invalidValue_throws() {
      assertThrows(IllegalArgumentException.class, () -> AgentNetwork.builder().maxRounds(0));
      assertThrows(IllegalArgumentException.class, () -> AgentNetwork.builder().maxRounds(-1));
    }

    @Test
    @DisplayName("maxRounds() accepts valid values")
    void maxRounds_validValue_succeeds() {
      Agent peer1 = createTestAgent("Peer1");
      Agent peer2 = createTestAgent("Peer2");

      AgentNetwork network =
          AgentNetwork.builder().addPeer(peer1).addPeer(peer2).maxRounds(5).build();

      assertEquals(5, network.maxRounds());
    }

    @Test
    @DisplayName("synthesizer() sets synthesizer agent")
    void synthesizer_setsSynthesizer() {
      Agent peer1 = createTestAgent("Peer1");
      Agent peer2 = createTestAgent("Peer2");
      Agent synth = createTestAgent("Synthesizer");

      AgentNetwork network =
          AgentNetwork.builder().addPeer(peer1).addPeer(peer2).synthesizer(synth).build();

      assertNotNull(network);
    }
  }

  @Nested
  @DisplayName("Peers")
  class PeerTests {

    @Test
    @DisplayName("peers() returns unmodifiable list")
    void peers_returnsUnmodifiableList() {
      Agent peer1 = createTestAgent("Peer1");
      Agent peer2 = createTestAgent("Peer2");

      AgentNetwork network = AgentNetwork.builder().addPeer(peer1).addPeer(peer2).build();

      assertThrows(UnsupportedOperationException.class, () -> network.peers().clear());
    }

    @Test
    @DisplayName("peers() contains added peers")
    void peers_containsAddedPeers() {
      Agent peer1 = createTestAgent("Peer1");
      Agent peer2 = createTestAgent("Peer2");

      AgentNetwork network = AgentNetwork.builder().addPeer(peer1).addPeer(peer2).build();

      assertTrue(network.peers().contains(peer1));
      assertTrue(network.peers().contains(peer2));
    }
  }

  @Nested
  @DisplayName("Discussion")
  class DiscussionTests {

    @Test
    @DisplayName("discuss(String) returns NetworkResult")
    void discuss_string_returnsNetworkResult() {
      AgentNetwork network = createTestNetwork();
      // 2 peers x 2 rounds = 4 responses needed
      enqueueSuccessResponse("Response 1");
      enqueueSuccessResponse("Response 2");
      enqueueSuccessResponse("Response 3");
      enqueueSuccessResponse("Response 4");

      AgentNetwork.NetworkResult result = network.discuss("Topic");

      assertNotNull(result);
      assertInstanceOf(AgentNetwork.NetworkResult.class, result);
    }

    @Test
    @DisplayName("discuss(String) validates null topic")
    void discuss_nullTopic_throws() {
      AgentNetwork network = createTestNetwork();

      assertThrows(NullPointerException.class, () -> network.discuss((String) null));
    }

    @Test
    @DisplayName("discuss(Text) returns NetworkResult")
    void discuss_text_returnsNetworkResult() {
      AgentNetwork network = createTestNetwork();
      // 2 peers x 2 rounds = 4 responses needed
      enqueueSuccessResponse("Response 1");
      enqueueSuccessResponse("Response 2");
      enqueueSuccessResponse("Response 3");
      enqueueSuccessResponse("Response 4");

      AgentNetwork.NetworkResult result =
          network.discuss(Text.valueOf("Topic"));

      assertNotNull(result);
    }

    @Test
    @DisplayName("discuss(Message) returns NetworkResult")
    void discuss_message_returnsNetworkResult() {
      AgentNetwork network = createTestNetwork();
      // 2 peers x 2 rounds = 4 responses needed
      enqueueSuccessResponse("Response 1");
      enqueueSuccessResponse("Response 2");
      enqueueSuccessResponse("Response 3");
      enqueueSuccessResponse("Response 4");

      AgentNetwork.NetworkResult result =
          network.discuss(Message.user("Topic"));

      assertNotNull(result);
    }

    @Test
    @DisplayName("discuss(AgentContext) returns NetworkResult")
    void discuss_context_returnsNetworkResult() {
      AgentNetwork network = createTestNetwork();
      AgentContext context = AgentContext.create();
      context.addInput(Message.user("Topic"));
      // 2 peers x 2 rounds = 4 responses needed
      enqueueSuccessResponse("Response 1");
      enqueueSuccessResponse("Response 2");
      enqueueSuccessResponse("Response 3");
      enqueueSuccessResponse("Response 4");

      AgentNetwork.NetworkResult result = network.discuss(context);

      assertNotNull(result);
    }
  }

  @Nested
  @DisplayName("Broadcast")
  class BroadcastTests {

    @Test
    @DisplayName("broadcast() returns List of contributions")
    void broadcast_returnsContributions() {
      AgentNetwork network = createTestNetwork();
      enqueueSuccessResponse("Response 1");
      enqueueSuccessResponse("Response 2");

      List<AgentNetwork.Contribution> contributions = network.broadcast("Message");

      assertNotNull(contributions);
    }

    @Test
    @DisplayName("broadcast() validates null message")
    void broadcast_nullMessage_throws() {
      AgentNetwork network = createTestNetwork();

      assertThrows(NullPointerException.class, () -> network.broadcast(null));
    }
  }

  @Nested
  @DisplayName("Contribution")
  class ContributionTests {

    @Test
    @DisplayName("Contribution validates null agent")
    void contribution_nullAgent_throws() {
      assertThrows(
          NullPointerException.class, () -> new AgentNetwork.Contribution(null, 1, "output", false));
    }

    @Test
    @DisplayName("Contribution validates invalid round")
    void contribution_invalidRound_throws() {
      Agent agent = createTestAgent("Test");
      assertThrows(
          IllegalArgumentException.class,
          () -> new AgentNetwork.Contribution(agent, 0, "output", false));
    }

    @Test
    @DisplayName("Contribution allows null output")
    void contribution_nullOutput_succeeds() {
      Agent agent = createTestAgent("Test");
      AgentNetwork.Contribution contribution = new AgentNetwork.Contribution(agent, 1, null, true);
      assertNull(contribution.output());
    }
  }

  @Nested
  @DisplayName("NetworkResult")
  class NetworkResultTests {

    @Test
    @DisplayName("NetworkResult validates null contributions")
    void networkResult_nullContributions_throws() {
      assertThrows(NullPointerException.class, () -> new AgentNetwork.NetworkResult(null, null));
    }

    @Test
    @DisplayName("contributionsFrom() returns contributions for specific agent")
    void contributionsFrom_returnsAgentContributions() {
      Agent agent1 = createTestAgent("Agent1");
      Agent agent2 = createTestAgent("Agent2");

      List<AgentNetwork.Contribution> contributions =
          List.of(
              new AgentNetwork.Contribution(agent1, 1, "First", false),
              new AgentNetwork.Contribution(agent2, 1, "Second", false),
              new AgentNetwork.Contribution(agent1, 2, "Third", false));

      AgentNetwork.NetworkResult result = new AgentNetwork.NetworkResult(contributions, null);

      assertEquals(2, result.contributionsFrom(agent1).size());
      assertEquals(1, result.contributionsFrom(agent2).size());
    }

    @Test
    @DisplayName("contributionsFromRound() returns contributions for specific round")
    void contributionsFromRound_returnsRoundContributions() {
      Agent agent1 = createTestAgent("Agent1");
      Agent agent2 = createTestAgent("Agent2");

      List<AgentNetwork.Contribution> contributions =
          List.of(
              new AgentNetwork.Contribution(agent1, 1, "First", false),
              new AgentNetwork.Contribution(agent2, 1, "Second", false),
              new AgentNetwork.Contribution(agent1, 2, "Third", false));

      AgentNetwork.NetworkResult result = new AgentNetwork.NetworkResult(contributions, null);

      assertEquals(2, result.contributionsFromRound(1).size());
      assertEquals(1, result.contributionsFromRound(2).size());
    }

    @Test
    @DisplayName("lastContribution() returns last contribution")
    void lastContribution_returnsLast() {
      Agent agent = createTestAgent("Agent");

      List<AgentNetwork.Contribution> contributions =
          List.of(
              new AgentNetwork.Contribution(agent, 1, "First", false),
              new AgentNetwork.Contribution(agent, 2, "Last", false));

      AgentNetwork.NetworkResult result = new AgentNetwork.NetworkResult(contributions, null);

      assertNotNull(result.lastContribution());
      assertEquals("Last", result.lastContribution().output());
    }

    @Test
    @DisplayName("lastContribution() returns null for empty contributions")
    void lastContribution_emptyContributions_returnsNull() {
      AgentNetwork.NetworkResult result = new AgentNetwork.NetworkResult(List.of(), null);

      assertNull(result.lastContribution());
    }

    @Test
    @DisplayName("synthesis() returns synthesis when available")
    void synthesis_returnsWhenAvailable() {
      AgentNetwork.NetworkResult result = new AgentNetwork.NetworkResult(List.of(), "Synthesized");

      assertEquals("Synthesized", result.synthesis());
    }
  }

  // Helper methods

  private Agent createTestAgent(String name) {
    return Agent.builder()
        .name(name)
        .model("test-model")
        .instructions("Test instructions for " + name)
        .responder(responder)
        .build();
  }

  private AgentNetwork createTestNetwork() {
    Agent peer1 = createTestAgent("Peer1");
    Agent peer2 = createTestAgent("Peer2");

    return AgentNetwork.builder().addPeer(peer1).addPeer(peer2).build();
  }

  private void enqueueSuccessResponse(String text) {
    String json =
        """
        {
          "id": "resp_001",
          "object": "response",
          "created_at": 1234567890,
          "status": "completed",
          "model": "test-model",
          "output": [
            {
              "type": "message",
              "id": "msg_001",
              "role": "assistant",
              "content": [
                {
                  "type": "output_text",
                  "text": "%s"
                }
              ]
            }
          ],
          "usage": {
            "input_tokens": 10,
            "output_tokens": 5,
            "total_tokens": 15
          }
        }
        """
            .formatted(text);

    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody(json)
            .addHeader("Content-Type", "application/json"));
  }
}
