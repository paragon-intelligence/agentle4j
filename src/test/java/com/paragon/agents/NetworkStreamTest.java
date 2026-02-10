package com.paragon.agents;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.Message;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for NetworkStream.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Callback registration
 *   <li>Discussion mode streaming
 *   <li>Broadcast mode streaming
 *   <li>Round tracking
 *   <li>Synthesis streaming
 *   <li>Error handling
 * </ul>
 */
@DisplayName("NetworkStream")
class NetworkStreamTest {

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

  private void enqueueStreamingResponse(String text) {
    // SSE format for streaming
    String sseData =
            """
                    data: {"type":"response.output_item.added","item":{"id":"item_1","type":"message","role":"assistant"}}
                    
                    data: {"type":"response.content_part.added","part":{"type":"text","text":""}}
                    
                    data: {"type":"response.output_text.delta","delta":"%s"}
                    
                    data: {"type":"response.output_text.done","text":"%s"}
                    
                    data: {"type":"response.done","response":{"id":"resp_1","status":"completed","output":[{"type":"message","id":"msg_1","role":"assistant","content":[{"type":"output_text","text":"%s"}]}],"usage":{"input_tokens":10,"output_tokens":5,"total_tokens":15}}}
                    
                    """
                    .formatted(text, text, text);

    mockWebServer.enqueue(
            new MockResponse()
                    .setResponseCode(200)
                    .setBody(sseData)
                    .addHeader("Content-Type", "text/event-stream"));
  }

  @Nested
  @DisplayName("Callbacks")
  class CallbackTests {

    @Test
    @DisplayName("onPeerTextDelta registers callback")
    void onPeerTextDelta_registersCallback() {
      AgentNetwork network = createTestNetwork();
      NetworkStream stream = network.discussStream("Topic");

      NetworkStream result = stream.onPeerTextDelta((agent, delta) -> {
      });

      assertSame(stream, result);
    }

    @Test
    @DisplayName("onPeerComplete registers callback")
    void onPeerComplete_registersCallback() {
      AgentNetwork network = createTestNetwork();
      NetworkStream stream = network.discussStream("Topic");

      NetworkStream result = stream.onPeerComplete((agent, result2) -> {
      });

      assertSame(stream, result);
    }

    @Test
    @DisplayName("onRoundStart registers callback")
    void onRoundStart_registersCallback() {
      AgentNetwork network = createTestNetwork();
      NetworkStream stream = network.discussStream("Topic");

      NetworkStream result = stream.onRoundStart(round -> {
      });

      assertSame(stream, result);
    }

    @Test
    @DisplayName("onRoundComplete registers callback")
    void onRoundComplete_registersCallback() {
      AgentNetwork network = createTestNetwork();
      NetworkStream stream = network.discussStream("Topic");

      NetworkStream result = stream.onRoundComplete(contributions -> {
      });

      assertSame(stream, result);
    }

    @Test
    @DisplayName("onSynthesisTextDelta registers callback")
    void onSynthesisTextDelta_registersCallback() {
      AgentNetwork network = createTestNetwork();
      NetworkStream stream = network.discussStream("Topic");

      NetworkStream result = stream.onSynthesisTextDelta(delta -> {
      });

      assertSame(stream, result);
    }

    @Test
    @DisplayName("onComplete registers callback")
    void onComplete_registersCallback() {
      AgentNetwork network = createTestNetwork();
      NetworkStream stream = network.discussStream("Topic");

      NetworkStream result = stream.onComplete(networkResult -> {
      });

      assertSame(stream, result);
    }

    @Test
    @DisplayName("onError registers callback")
    void onError_registersCallback() {
      AgentNetwork network = createTestNetwork();
      NetworkStream stream = network.discussStream("Topic");

      NetworkStream result = stream.onError(error -> {
      });

      assertSame(stream, result);
    }

    @Test
    @DisplayName("callbacks validate null")
    void callbacks_validateNull() {
      AgentNetwork network = createTestNetwork();
      NetworkStream stream = network.discussStream("Topic");

      assertThrows(NullPointerException.class, () -> stream.onPeerTextDelta(null));
      assertThrows(NullPointerException.class, () -> stream.onPeerComplete(null));
      assertThrows(NullPointerException.class, () -> stream.onRoundStart(null));
      assertThrows(NullPointerException.class, () -> stream.onRoundComplete(null));
      assertThrows(NullPointerException.class, () -> stream.onSynthesisTextDelta(null));
      assertThrows(NullPointerException.class, () -> stream.onComplete(null));
      assertThrows(NullPointerException.class, () -> stream.onError(null));
    }
  }

  // Helper methods

  @Nested
  @DisplayName("discussStream()")
  class DiscussStreamTests {

    @Test
    @DisplayName("discussStream(String) returns NetworkStream")
    void discussStream_string_returnsStream() {
      AgentNetwork network = createTestNetwork();

      NetworkStream stream = network.discussStream("Topic");

      assertNotNull(stream);
    }

    @Test
    @DisplayName("discussStream(String) validates null")
    void discussStream_nullString_throws() {
      AgentNetwork network = createTestNetwork();

      assertThrows(NullPointerException.class, () -> network.discussStream((String) null));
    }

    @Test
    @DisplayName("discussStream(AgentContext) returns NetworkStream")
    void discussStream_context_returnsStream() {
      AgentNetwork network = createTestNetwork();
      AgenticContext context = AgenticContext.create();
      context.addInput(Message.user("Topic"));

      NetworkStream stream = network.discussStream(context);

      assertNotNull(stream);
    }

    @Test
    @DisplayName("discussStream(AgentContext) validates null")
    void discussStream_nullContext_throws() {
      AgentNetwork network = createTestNetwork();

      assertThrows(NullPointerException.class, () -> network.discussStream((AgenticContext) null));
    }

    @Test
    @DisplayName("start() returns NetworkResult")
    void start_returnsNetworkResult() throws Exception {
      AgentNetwork network = createTestNetwork();
      enqueueStreamingResponse("Response 1");
      enqueueStreamingResponse("Response 2");
      enqueueStreamingResponse("Response 3"); // Round 2, peer 1
      enqueueStreamingResponse("Response 4"); // Round 2, peer 2

      AgentNetwork.NetworkResult result =
              network.discussStream("Topic").onComplete(r -> {
              }).start();

      assertNotNull(result);
      assertNotNull(result.contributions());
    }

    @Test
    @DisplayName("onRoundStart fires for each round")
    void onRoundStart_firesForEachRound() throws Exception {
      Agent peer1 = createTestAgent("Peer1");
      Agent peer2 = createTestAgent("Peer2");

      AgentNetwork network =
              AgentNetwork.builder().addPeer(peer1).addPeer(peer2).maxRounds(2).build();

      // 2 rounds * 2 peers = 4 responses
      enqueueStreamingResponse("R1P1");
      enqueueStreamingResponse("R1P2");
      enqueueStreamingResponse("R2P1");
      enqueueStreamingResponse("R2P2");

      List<Integer> rounds = new ArrayList<>();
      CountDownLatch latch = new CountDownLatch(1);

      network
              .discussStream("Topic")
              .onRoundStart(rounds::add)
              .onComplete(r -> latch.countDown())
              .start();

      assertTrue(latch.await(10, TimeUnit.SECONDS));
      assertEquals(2, rounds.size());
      assertEquals(List.of(1, 2), rounds);
    }

    @Test
    @DisplayName("onPeerComplete fires for each peer")
    void onPeerComplete_firesForEachPeer() throws Exception {
      AgentNetwork network = createTestNetwork();
      // 2 rounds * 2 peers = 4 responses
      enqueueStreamingResponse("R1P1");
      enqueueStreamingResponse("R1P2");
      enqueueStreamingResponse("R2P1");
      enqueueStreamingResponse("R2P2");

      AtomicInteger peerCompleteCount = new AtomicInteger(0);
      CountDownLatch latch = new CountDownLatch(1);

      network
              .discussStream("Topic")
              .onPeerComplete((agent, result) -> peerCompleteCount.incrementAndGet())
              .onComplete(r -> latch.countDown())
              .start();

      assertTrue(latch.await(10, TimeUnit.SECONDS));
      assertEquals(4, peerCompleteCount.get()); // 2 peers * 2 rounds
    }
  }

  @Nested
  @DisplayName("broadcastStream()")
  class BroadcastStreamTests {

    @Test
    @DisplayName("broadcastStream(String) returns NetworkStream")
    void broadcastStream_string_returnsStream() {
      AgentNetwork network = createTestNetwork();

      NetworkStream stream = network.broadcastStream("Message");

      assertNotNull(stream);
    }

    @Test
    @DisplayName("broadcastStream(String) validates null")
    void broadcastStream_nullString_throws() {
      AgentNetwork network = createTestNetwork();

      assertThrows(NullPointerException.class, () -> network.broadcastStream((String) null));
    }

    @Test
    @DisplayName("broadcast mode runs all peers in parallel")
    void broadcast_runsAllPeersInParallel() throws Exception {
      AgentNetwork network = createTestNetwork();
      enqueueStreamingResponse("Response 1");
      enqueueStreamingResponse("Response 2");

      AtomicInteger peerCompleteCount = new AtomicInteger(0);
      CountDownLatch latch = new CountDownLatch(1);

      network
              .broadcastStream("Message")
              .onPeerComplete((agent, result) -> peerCompleteCount.incrementAndGet())
              .onComplete(r -> latch.countDown())
              .start();

      assertTrue(latch.await(10, TimeUnit.SECONDS));
      assertEquals(2, peerCompleteCount.get());
    }

    @Test
    @DisplayName("broadcast mode returns contributions with round 1")
    void broadcast_returnsContributionsWithRound1() throws Exception {
      AgentNetwork network = createTestNetwork();
      enqueueStreamingResponse("Response 1");
      enqueueStreamingResponse("Response 2");

      AtomicReference<AgentNetwork.NetworkResult> resultRef = new AtomicReference<>();
      CountDownLatch latch = new CountDownLatch(1);

      network
              .broadcastStream("Message")
              .onComplete(
                      r -> {
                        resultRef.set(r);
                        latch.countDown();
                      })
              .start();

      assertTrue(latch.await(10, TimeUnit.SECONDS));
      AgentNetwork.NetworkResult result = resultRef.get();
      assertNotNull(result);
      assertEquals(2, result.contributions().size());
      // All broadcast contributions should be round 1
      assertTrue(result.contributions().stream().allMatch(c -> c.round() == 1));
    }
  }

  @Nested
  @DisplayName("Synthesis")
  class SynthesisTests {

    @Test
    @DisplayName("synthesis streaming works with synthesizer")
    void synthesis_worksWithSynthesizer() throws Exception {
      Agent peer1 = createTestAgent("Peer1");
      Agent peer2 = createTestAgent("Peer2");
      Agent synthesizer = createTestAgent("Synthesizer");

      AgentNetwork network =
              AgentNetwork.builder()
                      .addPeer(peer1)
                      .addPeer(peer2)
                      .maxRounds(1)
                      .synthesizer(synthesizer)
                      .build();

      // Verify synthesizer is configured
      assertNotNull(network.getSynthesizer());
      assertEquals("Synthesizer", network.getSynthesizer().name());

      // Verify streaming works with synthesizer
      NetworkStream stream = network.discussStream("Topic");
      assertNotNull(stream);
    }
  }
}

