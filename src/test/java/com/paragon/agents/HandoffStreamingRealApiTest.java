package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import tools.jackson.databind.ObjectMapper;
import com.paragon.responses.Responder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;

/**
 * Real end-to-end test: streaming + handoffs + structured output, against OpenRouter (no mocks).
 *
 * <p>Scenario: a TriageAgent routes a billing query to BillingAgent (structured output
 * {@link BillingTicket}) and a technical query to TechnicalAgent (structured output
 * {@link TechTicket}). Both handoffs run on the streaming path.
 *
 * <pre>{@code
 * export OPENROUTER_API_KEY=sk-or-...
 * mvn test -Dtest="HandoffStreamingRealApiTest"
 * }</pre>
 */
@DisplayName("Handoff + Streaming + Structured Output — Real API (OpenRouter)")
@Tag("realapi")
class HandoffStreamingRealApiTest {

  private static final String MODEL = "openai/gpt-4o-mini";

  // ── Structured output types ──────────────────────────────────────────────

  record BillingTicket(String summary, String recommendedAction) {}

  record TechTicket(String issue, String suggestedFix, String priority) {}

  // ── Shared state ─────────────────────────────────────────────────────────

  private Responder responder;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    String apiKey = System.getenv("OPENROUTER_API_KEY");
    assumeTrue(apiKey != null && !apiKey.isBlank(), "OPENROUTER_API_KEY not set — skipping");
    responder = Responder.builder().openRouter().apiKey(apiKey).build();
    objectMapper = new ObjectMapper();
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private Agent buildBillingAgent() {
    return Agent.builder()
        .name("BillingAgent")
        .model(MODEL)
        .responder(responder)
        .instructions(
            "You are a billing specialist. "
                + "Reply ONLY with a JSON object — no markdown fences, no extra text — matching: "
                + "{\"summary\": \"<one-sentence summary>\", "
                + "\"recommendedAction\": \"<what the customer should do>\"}")
        .build();
  }

  private Agent buildTechnicalAgent() {
    return Agent.builder()
        .name("TechnicalAgent")
        .model(MODEL)
        .responder(responder)
        .instructions(
            "You are a technical support specialist. "
                + "Reply ONLY with a JSON object — no markdown fences, no extra text — matching: "
                + "{\"issue\": \"<brief issue title>\", "
                + "\"suggestedFix\": \"<step-by-step fix>\", "
                + "\"priority\": \"low|medium|high\"}")
        .build();
  }

  private Agent buildTriageAgent(Agent billing, Agent technical) {
    return Agent.builder()
        .name("TriageAgent")
        .model(MODEL)
        .responder(responder)
        .instructions(
            "You are a customer-support triage bot. "
                + "Route billing/invoice/payment questions to the billing specialist. "
                + "Route technical/software/connectivity questions to the technical specialist. "
                + "Always call the appropriate transfer tool immediately. Do NOT answer directly.")
        .addHandoff(
            Handoff.to(billing)
                .withDescription("Transfer billing or invoice questions to the billing specialist")
                .build())
        .addHandoff(
            Handoff.to(technical)
                .withDescription("Transfer technical or software questions to the technical specialist")
                .build())
        .build();
  }

  // ── Tests ─────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("billing query: streaming handoff fires onTextDelta and child returns BillingTicket")
  void billingQuery_streamingHandoff_returnsStructuredBillingTicket() throws Exception {
    Agent billing = buildBillingAgent();
    Agent technical = buildTechnicalAgent();
    Agent triage = buildTriageAgent(billing, technical);

    List<String> chunks = new ArrayList<>();
    AtomicReference<Handoff> handoffRef = new AtomicReference<>();
    AtomicReference<AgentResult> resultRef = new AtomicReference<>();

    triage
        .asStreaming()
        .interact(
            "My invoice from last month shows an unexpected charge of $49. "
                + "This is a billing problem — please help me resolve it.")
        .onTextDelta(chunks::add)
        .onHandoff(handoffRef::set)
        .onComplete(resultRef::set)
        .startBlocking();

    AgentResult result = resultRef.get();
    assertNotNull(result, "onComplete did not fire");
    assertFalse(
        result.isError(),
        "Result is an error: " + errorDetails(result.error()));

    // onHandoff callback should have fired
    assertNotNull(handoffRef.get(), "onHandoff never fired — triage did not route");
    assertEquals("BillingAgent", handoffRef.get().targetAgent().name());

    // Text deltas should have come from the child agent (streaming path)
    assertFalse(chunks.isEmpty(), "onTextDelta never fired — child ran on blocking path");

    // The final output (from child agent) must be parseable as BillingTicket
    String output = result.output();
    assertNotNull(output, "result output is null");
    assertFalse(output.isBlank(), "result output is blank");

    BillingTicket ticket = parseJson(output, BillingTicket.class);
    assertNotNull(ticket.summary(), "BillingTicket.summary is null");
    assertFalse(ticket.summary().isBlank(), "BillingTicket.summary is blank");
    assertNotNull(ticket.recommendedAction(), "BillingTicket.recommendedAction is null");
  }

  @Test
  @DisplayName("technical query: streaming handoff fires onTextDelta and child returns TechTicket")
  void technicalQuery_streamingHandoff_returnsStructuredTechTicket() throws Exception {
    Agent billing = buildBillingAgent();
    Agent technical = buildTechnicalAgent();
    Agent triage = buildTriageAgent(billing, technical);

    List<String> chunks = new ArrayList<>();
    AtomicReference<Handoff> handoffRef = new AtomicReference<>();
    AtomicReference<AgentResult> resultRef = new AtomicReference<>();

    triage
        .asStreaming()
        .interact(
            "My app keeps crashing whenever I try to upload a file. "
                + "This is a technical software bug — please help me fix it.")
        .onTextDelta(chunks::add)
        .onHandoff(handoffRef::set)
        .onComplete(resultRef::set)
        .startBlocking();

    AgentResult result = resultRef.get();
    assertNotNull(result, "onComplete did not fire");
    assertFalse(
        result.isError(),
        "Result is an error: " + errorDetails(result.error()));

    // onHandoff callback should have fired
    assertNotNull(handoffRef.get(), "onHandoff never fired — triage did not route");
    assertEquals("TechnicalAgent", handoffRef.get().targetAgent().name());

    // Text deltas from the child streaming path
    assertFalse(chunks.isEmpty(), "onTextDelta never fired — child ran on blocking path");

    // The final output (from child agent) must be parseable as TechTicket
    String output = result.output();
    assertNotNull(output, "result output is null");
    assertFalse(output.isBlank(), "result output is blank");

    TechTicket ticket = parseJson(output, TechTicket.class);
    assertNotNull(ticket.issue(), "TechTicket.issue is null");
    assertFalse(ticket.issue().isBlank(), "TechTicket.issue is blank");
    assertNotNull(ticket.priority(), "TechTicket.priority is null");
  }

  // ── Private helpers ───────────────────────────────────────────────────────

  private static String errorDetails(Throwable t) {
    if (t == null) return "null";
    StringBuilder sb = new StringBuilder(t.getMessage());
    Throwable cause = t.getCause();
    while (cause != null) {
      sb.append(" | caused by: ").append(cause.getMessage());
      cause = cause.getCause();
    }
    return sb.toString();
  }

  private <T> T parseJson(String json, Class<T> type) throws Exception {
    String cleaned = json.strip();
    // Strip optional markdown code fences
    if (cleaned.startsWith("```")) {
      int firstNewline = cleaned.indexOf('\n');
      int lastFence = cleaned.lastIndexOf("```");
      if (firstNewline > 0 && lastFence > firstNewline) {
        cleaned = cleaned.substring(firstNewline + 1, lastFence).strip();
      }
    }
    return objectMapper.readValue(cleaned, type);
  }
}
