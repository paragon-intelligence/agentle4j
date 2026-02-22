package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.http.RetryPolicy;
import com.paragon.responses.TraceMetadata;
import com.paragon.agents.InteractableBlueprint.*;
import java.util.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the InteractableBlueprint sealed hierarchy:
 * - Jackson serialization/deserialization roundtrip
 * - Polymorphic type discrimination
 * - Guardrail registry integration
 * - Helper records (ResponderBlueprint, RetryPolicyBlueprint, GuardrailReference)
 */
class InteractableBlueprintTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
    mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    GuardrailRegistry.clear();
  }

  // ===== ResponderBlueprint =====

  @Test
  void responderBlueprintRoundtrip() throws Exception {
    var retryPolicy = new RetryPolicyBlueprint(3, 1000, 30000, 2.0, Set.of(429, 500));
    var blueprint = new ResponderBlueprint("OPEN_ROUTER", null, "OPENROUTER_API_KEY", retryPolicy, null);

    String json = mapper.writeValueAsString(blueprint);
    var restored = mapper.readValue(json, ResponderBlueprint.class);

    assertEquals("OPEN_ROUTER", restored.provider());
    assertNull(restored.baseUrl());
    assertEquals("OPENROUTER_API_KEY", restored.apiKeyEnvVar());
    assertEquals(3, restored.retryPolicy().maxRetries());
    assertEquals(1000, restored.retryPolicy().initialDelayMs());
    assertEquals(2.0, restored.retryPolicy().multiplier());
  }

  @Test
  void responderBlueprintCustomUrl() throws Exception {
    var blueprint = new ResponderBlueprint(null, "https://custom.api.com/v1", "MY_KEY", null, null);

    String json = mapper.writeValueAsString(blueprint);
    var restored = mapper.readValue(json, ResponderBlueprint.class);

    assertNull(restored.provider());
    assertEquals("https://custom.api.com/v1", restored.baseUrl());
    assertEquals("MY_KEY", restored.apiKeyEnvVar());
  }

  // ===== RetryPolicyBlueprint =====

  @Test
  void retryPolicyBlueprintConversion() {
    RetryPolicy original = RetryPolicy.defaults();
    RetryPolicyBlueprint blueprint = RetryPolicyBlueprint.from(original);

    assertEquals(3, blueprint.maxRetries());
    assertEquals(1000, blueprint.initialDelayMs());
    assertEquals(30000, blueprint.maxDelayMs());
    assertEquals(2.0, blueprint.multiplier());

    RetryPolicy restored = blueprint.toRetryPolicy();
    assertEquals(original.maxRetries(), restored.maxRetries());
    assertEquals(original.initialDelay(), restored.initialDelay());
    assertEquals(original.maxDelay(), restored.maxDelay());
    assertEquals(original.multiplier(), restored.multiplier());
  }

  // ===== AgentBlueprint Roundtrip =====

  @Test
  void agentBlueprintRoundtrip() throws Exception {
    var responder = new ResponderBlueprint("OPEN_ROUTER", null, "OPENROUTER_API_KEY", null, null);
    var blueprint = new AgentBlueprint(
        "SupportAgent", "gpt-4o", "You are a helpful support agent.",
        10, 0.7, null, null, responder,
        List.of(), List.of(), List.of(), List.of(), null);

    String json = mapper.writeValueAsString(blueprint);
    assertNotNull(json);
    assertTrue(json.contains("\"type\":\"agent\""));
    assertTrue(json.contains("\"name\":\"SupportAgent\""));
    assertTrue(json.contains("\"model\":\"gpt-4o\""));
    assertTrue(json.contains("\"maxTurns\":10"));
    assertTrue(json.contains("\"temperature\":0.7"));

    InteractableBlueprint restored = mapper.readValue(json, InteractableBlueprint.class);
    assertInstanceOf(AgentBlueprint.class, restored);
    AgentBlueprint agentBp = (AgentBlueprint) restored;

    assertEquals("SupportAgent", agentBp.name());
    assertEquals("gpt-4o", agentBp.model());
    assertEquals("You are a helpful support agent.", agentBp.instructions());
    assertEquals(10, agentBp.maxTurns());
    assertEquals(0.7, agentBp.temperature());
    assertNotNull(agentBp.responder());
    assertEquals("OPEN_ROUTER", agentBp.responder().provider());
  }

  @Test
  void agentBlueprintWithTraceMetadata() throws Exception {
    var trace = new TraceMetadata("trace-1", "myTrace", "span-1", "gen-1", null, "prod", Map.of("key", "value"));
    var responder = new ResponderBlueprint("OPENAI", null, "OPENAI_API_KEY", null, trace);
    var blueprint = new AgentBlueprint(
        "TracedAgent", "gpt-4", "Instructions", 5, null, null,
        trace, responder, List.of(), List.of(), List.of(), List.of(), null);

    String json = mapper.writeValueAsString(blueprint);
    InteractableBlueprint restored = mapper.readValue(json, InteractableBlueprint.class);
    AgentBlueprint agentBp = (AgentBlueprint) restored;

    assertNotNull(agentBp.traceMetadata());
    assertEquals("trace-1", agentBp.traceMetadata().traceId());
    assertEquals("prod", agentBp.traceMetadata().environment());
  }

  @Test
  void agentBlueprintWithTools() throws Exception {
    var responder = new ResponderBlueprint("OPEN_ROUTER", null, "OPENROUTER_API_KEY", null, null);
    var blueprint = new AgentBlueprint(
        "ToolAgent", "gpt-4o", "Help the user",
        10, null, null, null, responder,
        List.of("com.example.GetWeatherTool", "com.example.SearchTool"),
        List.of(), List.of(), List.of(), null);

    String json = mapper.writeValueAsString(blueprint);
    InteractableBlueprint restored = mapper.readValue(json, InteractableBlueprint.class);
    AgentBlueprint agentBp = (AgentBlueprint) restored;

    assertEquals(2, agentBp.toolClassNames().size());
    assertEquals("com.example.GetWeatherTool", agentBp.toolClassNames().get(0));
    assertEquals("com.example.SearchTool", agentBp.toolClassNames().get(1));
  }

  @Test
  void agentBlueprintWithOutputType() throws Exception {
    var responder = new ResponderBlueprint("OPEN_ROUTER", null, null, null, null);
    var blueprint = new AgentBlueprint(
        "StructuredAgent", "gpt-4o", "Extract data",
        10, null, "java.lang.String", null, responder,
        List.of(), List.of(), List.of(), List.of(), null);

    String json = mapper.writeValueAsString(blueprint);
    AgentBlueprint restored = (AgentBlueprint) mapper.readValue(json, InteractableBlueprint.class);
    assertEquals("java.lang.String", restored.outputType());
  }

  // ===== AgentNetworkBlueprint Roundtrip =====

  @Test
  void agentNetworkBlueprintRoundtrip() throws Exception {
    var responder = new ResponderBlueprint("OPEN_ROUTER", null, null, null, null);
    var peer1 = new AgentBlueprint("Peer1", "gpt-4o", "Peer 1 instructions", 5, null, null, null,
        responder, List.of(), List.of(), List.of(), List.of(), null);
    var peer2 = new AgentBlueprint("Peer2", "gpt-4o", "Peer 2 instructions", 5, null, null, null,
        responder, List.of(), List.of(), List.of(), List.of(), null);

    var network = new AgentNetworkBlueprint("TestNetwork", List.of(peer1, peer2), 3, null, null);

    String json = mapper.writeValueAsString(network);
    assertTrue(json.contains("\"type\":\"network\""));

    InteractableBlueprint restored = mapper.readValue(json, InteractableBlueprint.class);
    assertInstanceOf(AgentNetworkBlueprint.class, restored);
    AgentNetworkBlueprint networkBp = (AgentNetworkBlueprint) restored;

    assertEquals("TestNetwork", networkBp.name());
    assertEquals(3, networkBp.maxRounds());
    assertEquals(2, networkBp.peers().size());
    assertNull(networkBp.synthesizer());
  }

  @Test
  void agentNetworkWithSynthesizer() throws Exception {
    var responder = new ResponderBlueprint("OPEN_ROUTER", null, null, null, null);
    var peer = new AgentBlueprint("Peer", "gpt-4o", "Inst", 5, null, null, null,
        responder, List.of(), List.of(), List.of(), List.of(), null);
    var synth = new AgentBlueprint("Synth", "gpt-4o", "Synthesize", 5, null, null, null,
        responder, List.of(), List.of(), List.of(), List.of(), null);

    var network = new AgentNetworkBlueprint("Net", List.of(peer, peer), 2, synth, null);

    String json = mapper.writeValueAsString(network);
    AgentNetworkBlueprint restored = (AgentNetworkBlueprint) mapper.readValue(json, InteractableBlueprint.class);

    assertNotNull(restored.synthesizer());
    assertInstanceOf(AgentBlueprint.class, restored.synthesizer());
    assertEquals("Synth", restored.synthesizer().name());
  }

  // ===== SupervisorAgentBlueprint Roundtrip =====

  @Test
  void supervisorAgentBlueprintRoundtrip() throws Exception {
    var responder = new ResponderBlueprint("OPEN_ROUTER", null, null, null, null);
    var worker = new AgentBlueprint("Worker1", "gpt-4o", "Do research", 5, null, null, null,
        responder, List.of(), List.of(), List.of(), List.of(), null);
    var workerBp = new WorkerBlueprint(worker, "Research assistant");

    var supervisor = new SupervisorAgentBlueprint(
        "Supervisor", "gpt-4o", "Manage workers", 10,
        List.of(workerBp), responder, null);

    String json = mapper.writeValueAsString(supervisor);
    assertTrue(json.contains("\"type\":\"supervisor\""));

    InteractableBlueprint restored = mapper.readValue(json, InteractableBlueprint.class);
    assertInstanceOf(SupervisorAgentBlueprint.class, restored);
    SupervisorAgentBlueprint supBp = (SupervisorAgentBlueprint) restored;

    assertEquals("Supervisor", supBp.name());
    assertEquals(1, supBp.workers().size());
    assertEquals("Research assistant", supBp.workers().get(0).description());
    assertInstanceOf(AgentBlueprint.class, supBp.workers().get(0).worker());
  }

  // ===== ParallelAgentsBlueprint Roundtrip =====

  @Test
  void parallelAgentsBlueprintRoundtrip() throws Exception {
    var responder = new ResponderBlueprint("OPEN_ROUTER", null, null, null, null);
    var member1 = new AgentBlueprint("Fast", "gpt-4o-mini", "Quick answers", 3, null, null, null,
        responder, List.of(), List.of(), List.of(), List.of(), null);
    var member2 = new AgentBlueprint("Deep", "gpt-4o", "Detailed answers", 5, null, null, null,
        responder, List.of(), List.of(), List.of(), List.of(), null);

    var parallel = new ParallelAgentsBlueprint("ParallelTeam", List.of(member1, member2), null);

    String json = mapper.writeValueAsString(parallel);
    assertTrue(json.contains("\"type\":\"parallel\""));

    InteractableBlueprint restored = mapper.readValue(json, InteractableBlueprint.class);
    assertInstanceOf(ParallelAgentsBlueprint.class, restored);
    ParallelAgentsBlueprint parBp = (ParallelAgentsBlueprint) restored;

    assertEquals("ParallelTeam", parBp.name());
    assertEquals(2, parBp.members().size());
  }

  // ===== RouterAgentBlueprint Roundtrip =====

  @Test
  void routerAgentBlueprintRoundtrip() throws Exception {
    var responder = new ResponderBlueprint("OPEN_ROUTER", null, null, null, null);
    var target1 = new AgentBlueprint("Sales", "gpt-4o", "Sales agent", 5, null, null, null,
        responder, List.of(), List.of(), List.of(), List.of(), null);
    var target2 = new AgentBlueprint("Support", "gpt-4o", "Support agent", 5, null, null, null,
        responder, List.of(), List.of(), List.of(), List.of(), null);
    var fallback = new AgentBlueprint("General", "gpt-4o", "General agent", 5, null, null, null,
        responder, List.of(), List.of(), List.of(), List.of(), null);

    var router = new RouterAgentBlueprint(
        "Router", "gpt-4o",
        List.of(
            new RouteBlueprint(target1, "Sales inquiries"),
            new RouteBlueprint(target2, "Support tickets")),
        fallback, responder, null);

    String json = mapper.writeValueAsString(router);
    assertTrue(json.contains("\"type\":\"router\""));

    InteractableBlueprint restored = mapper.readValue(json, InteractableBlueprint.class);
    assertInstanceOf(RouterAgentBlueprint.class, restored);
    RouterAgentBlueprint routerBp = (RouterAgentBlueprint) restored;

    assertEquals("Router", routerBp.name());
    assertEquals(2, routerBp.routes().size());
    assertEquals("Sales inquiries", routerBp.routes().get(0).description());
    assertNotNull(routerBp.fallback());
    assertEquals("General", routerBp.fallback().name());
  }

  // ===== HierarchicalAgentsBlueprint Roundtrip =====

  @Test
  void hierarchicalAgentsBlueprintRoundtrip() throws Exception {
    var responder = new ResponderBlueprint("OPEN_ROUTER", null, null, null, null);
    var exec = new AgentBlueprint("CEO", "gpt-4o", "Lead the company", 10, null, null, null,
        responder, List.of(), List.of(), List.of(), List.of(), null);
    var manager = new AgentBlueprint("EngineeringMgr", "gpt-4o", "Manage engineering", 5, null, null, null,
        responder, List.of(), List.of(), List.of(), List.of(), null);
    var worker = new AgentBlueprint("Developer", "gpt-4o-mini", "Write code", 3, null, null, null,
        responder, List.of(), List.of(), List.of(), List.of(), null);

    var dept = new DepartmentBlueprint(manager, List.of(worker));
    var hierarchical = new HierarchicalAgentsBlueprint(
        exec, Map.of("Engineering", dept), 10, null);

    String json = mapper.writeValueAsString(hierarchical);
    assertTrue(json.contains("\"type\":\"hierarchical\""));

    InteractableBlueprint restored = mapper.readValue(json, InteractableBlueprint.class);
    assertInstanceOf(HierarchicalAgentsBlueprint.class, restored);
    HierarchicalAgentsBlueprint hierBp = (HierarchicalAgentsBlueprint) restored;

    assertEquals("CEO_Hierarchy", hierBp.name());
    assertEquals(1, hierBp.departments().size());
    assertTrue(hierBp.departments().containsKey("Engineering"));
    assertEquals("EngineeringMgr", hierBp.departments().get("Engineering").manager().name());
    assertEquals(1, hierBp.departments().get("Engineering").workers().size());
  }

  // ===== Guardrail Registry & References =====

  @Test
  void guardrailRegistryInputRoundtrip() {
    InputGuardrail guard = InputGuardrail.named("no_spam", (input, ctx) ->
        input.contains("spam") ? GuardrailResult.failed("Spam detected") : GuardrailResult.passed());

    assertInstanceOf(NamedInputGuardrail.class, guard);
    assertEquals("no_spam", ((NamedInputGuardrail) guard).id());

    // Registry should have the guardrail
    assertNotNull(GuardrailRegistry.getInput("no_spam"));

    // Create reference and restore
    GuardrailReference ref = GuardrailReference.fromInput(guard);
    assertNull(ref.className());
    assertEquals("no_spam", ref.registryId());

    InputGuardrail restored = ref.toInputGuardrail();
    assertNotNull(restored);
  }

  @Test
  void guardrailRegistryOutputRoundtrip() {
    OutputGuardrail guard = OutputGuardrail.named("max_length", (output, ctx) ->
        output.length() > 1000 ? GuardrailResult.failed("Too long") : GuardrailResult.passed());

    assertInstanceOf(NamedOutputGuardrail.class, guard);

    GuardrailReference ref = GuardrailReference.fromOutput(guard);
    assertEquals("max_length", ref.registryId());

    OutputGuardrail restored = ref.toOutputGuardrail();
    assertNotNull(restored);
  }

  @Test
  void guardrailReferenceSerializationRoundtrip() throws Exception {
    // Register guardrail
    InputGuardrail.named("validate_input", (input, ctx) -> GuardrailResult.passed());

    var ref = new GuardrailReference(null, "validate_input");
    String json = mapper.writeValueAsString(ref);
    GuardrailReference restored = mapper.readValue(json, GuardrailReference.class);

    assertEquals("validate_input", restored.registryId());
    assertNull(restored.className());
    assertNotNull(restored.toInputGuardrail());
  }

  @Test
  void guardrailReferenceUnregisteredThrows() {
    var ref = new GuardrailReference(null, "nonexistent");
    assertThrows(IllegalStateException.class, ref::toInputGuardrail);
  }

  @Test
  void agentBlueprintWithGuardrailsRoundtrip() throws Exception {
    // Register named guardrails
    InputGuardrail.named("profanity_filter", (input, ctx) -> GuardrailResult.passed());
    OutputGuardrail.named("length_check", (output, ctx) -> GuardrailResult.passed());

    var responder = new ResponderBlueprint("OPEN_ROUTER", null, null, null, null);
    var blueprint = new AgentBlueprint(
        "GuardedAgent", "gpt-4o", "Be safe",
        10, null, null, null, responder,
        List.of(), List.of(),
        List.of(new GuardrailReference(null, "profanity_filter")),
        List.of(new GuardrailReference(null, "length_check")),
        null);

    String json = mapper.writeValueAsString(blueprint);
    AgentBlueprint restored = (AgentBlueprint) mapper.readValue(json, InteractableBlueprint.class);

    assertEquals(1, restored.inputGuardrails().size());
    assertEquals("profanity_filter", restored.inputGuardrails().get(0).registryId());
    assertEquals(1, restored.outputGuardrails().size());
    assertEquals("length_check", restored.outputGuardrails().get(0).registryId());
  }

  // ===== Handoff Descriptor =====

  @Test
  void handoffDescriptorRoundtrip() throws Exception {
    var responder = new ResponderBlueprint("OPEN_ROUTER", null, null, null, null);
    var targetAgent = new AgentBlueprint(
        "EscalationAgent", "gpt-4o", "Handle escalation", 5, null, null, null,
        responder, List.of(), List.of(), List.of(), List.of(), null);

    var handoff = new HandoffDescriptor("escalate", "Escalate to specialist", targetAgent);

    String json = mapper.writeValueAsString(handoff);
    HandoffDescriptor restored = mapper.readValue(json, HandoffDescriptor.class);

    assertEquals("escalate", restored.name());
    assertEquals("Escalate to specialist", restored.description());
    assertInstanceOf(AgentBlueprint.class, restored.target());
    assertEquals("EscalationAgent", restored.target().name());
  }

  @Test
  void agentBlueprintWithHandoffsRoundtrip() throws Exception {
    var responder = new ResponderBlueprint("OPEN_ROUTER", null, null, null, null);
    var target = new AgentBlueprint("Target", "gpt-4o", "Target agent", 5, null, null, null,
        responder, List.of(), List.of(), List.of(), List.of(), null);

    var blueprint = new AgentBlueprint(
        "SourceAgent", "gpt-4o", "Source agent",
        10, null, null, null, responder,
        List.of(),
        List.of(new HandoffDescriptor("handoff_to_target", "Transfer to target agent", target)),
        List.of(), List.of(), null);

    String json = mapper.writeValueAsString(blueprint);
    AgentBlueprint restored = (AgentBlueprint) mapper.readValue(json, InteractableBlueprint.class);

    assertEquals(1, restored.handoffs().size());
    assertEquals("handoff_to_target", restored.handoffs().get(0).name());
    assertEquals("Target", restored.handoffs().get(0).target().name());
  }

  // ===== Context Blueprint =====

  @Test
  void contextBlueprintSlidingWindow() throws Exception {
    var ctx = new ContextBlueprint("sliding", true, null, null, null, 4000, null);

    String json = mapper.writeValueAsString(ctx);
    ContextBlueprint restored = mapper.readValue(json, ContextBlueprint.class);

    assertEquals("sliding", restored.strategyType());
    assertTrue(restored.preserveDeveloperMessages());
    assertEquals(4000, restored.maxTokens());
  }

  @Test
  void contextBlueprintSummarization() throws Exception {
    var ctx = new ContextBlueprint("summarization", null, "gpt-4o-mini", 5, "Summarize this", 8000, null);

    String json = mapper.writeValueAsString(ctx);
    ContextBlueprint restored = mapper.readValue(json, ContextBlueprint.class);

    assertEquals("summarization", restored.strategyType());
    assertEquals("gpt-4o-mini", restored.summarizationModel());
    assertEquals(5, restored.keepRecentMessages());
    assertEquals("Summarize this", restored.summarizationPrompt());
    assertEquals(8000, restored.maxTokens());
  }

  @Test
  void agentBlueprintWithContextManagement() throws Exception {
    var responder = new ResponderBlueprint("OPEN_ROUTER", null, null, null, null);
    var ctx = new ContextBlueprint("sliding", true, null, null, null, 4000, null);
    var blueprint = new AgentBlueprint(
        "CtxAgent", "gpt-4o", "Managed context",
        10, null, null, null, responder,
        List.of(), List.of(), List.of(), List.of(), ctx);

    String json = mapper.writeValueAsString(blueprint);
    AgentBlueprint restored = (AgentBlueprint) mapper.readValue(json, InteractableBlueprint.class);

    assertNotNull(restored.contextManagement());
    assertEquals("sliding", restored.contextManagement().strategyType());
    assertTrue(restored.contextManagement().preserveDeveloperMessages());
  }

  // ===== Polymorphic Discrimination =====

  @Test
  void polymorphicDeserializationAllTypes() throws Exception {
    var responder = new ResponderBlueprint("OPEN_ROUTER", null, null, null, null);
    var agent = new AgentBlueprint("A", "m", "i", 5, null, null, null,
        responder, List.of(), List.of(), List.of(), List.of(), null);

    // Each type serializes with the correct "type" discriminator
    Map<String, InteractableBlueprint> blueprints = Map.of(
        "agent", agent,
        "network", new AgentNetworkBlueprint("N", List.of(agent, agent), 2, null, null),
        "supervisor", new SupervisorAgentBlueprint("S", "m", "i", 5,
            List.of(new WorkerBlueprint(agent, "d")), responder, null),
        "parallel", new ParallelAgentsBlueprint("P", List.of(agent), null),
        "router", new RouterAgentBlueprint("R", "m",
            List.of(new RouteBlueprint(agent, "d")), null, responder, null),
        "hierarchical", new HierarchicalAgentsBlueprint(agent, Map.of("d",
            new DepartmentBlueprint(agent, List.of(agent))), 5, null));

    for (var entry : blueprints.entrySet()) {
      String json = mapper.writeValueAsString(entry.getValue());
      assertTrue(json.contains("\"type\":\"" + entry.getKey() + "\""),
          "Expected type discriminator '" + entry.getKey() + "' in JSON: " + json);

      InteractableBlueprint restored = mapper.readValue(json, InteractableBlueprint.class);
      assertEquals(entry.getValue().name(), restored.name(),
          "Name mismatch for type: " + entry.getKey());
    }
  }

  // ===== Complex Nested Blueprint =====

  @Test
  void deeplyNestedBlueprintRoundtrip() throws Exception {
    var responder = new ResponderBlueprint("OPEN_ROUTER", null, "OPENROUTER_API_KEY", null, null);

    // Build a complex constellation: Router → Supervisor → Agent + Network
    var codeAgent = new AgentBlueprint("CodeAgent", "gpt-4o", "Write code", 10, 0.2, null, null,
        responder, List.of(), List.of(), List.of(), List.of(), null);
    var reviewAgent = new AgentBlueprint("ReviewAgent", "gpt-4o", "Review code", 5, null, null, null,
        responder, List.of(), List.of(), List.of(), List.of(), null);

    var devSupervisor = new SupervisorAgentBlueprint("DevSupervisor", "gpt-4o", "Manage dev",
        10, List.of(
            new WorkerBlueprint(codeAgent, "Code writer"),
            new WorkerBlueprint(reviewAgent, "Code reviewer")),
        responder, null);

    var salesAgent = new AgentBlueprint("Sales", "gpt-4o", "Handle sales", 5, null, null, null,
        responder, List.of(), List.of(), List.of(), List.of(), null);

    var router = new RouterAgentBlueprint("MainRouter", "gpt-4o",
        List.of(
            new RouteBlueprint(devSupervisor, "Development tasks"),
            new RouteBlueprint(salesAgent, "Sales inquiries")),
        null, responder, null);

    // Full roundtrip
    String json = mapper.writeValueAsString(router);
    InteractableBlueprint restored = mapper.readValue(json, InteractableBlueprint.class);

    assertInstanceOf(RouterAgentBlueprint.class, restored);
    RouterAgentBlueprint routerBp = (RouterAgentBlueprint) restored;
    assertEquals("MainRouter", routerBp.name());
    assertEquals(2, routerBp.routes().size());

    // First route is the supervisor
    InteractableBlueprint devRoute = routerBp.routes().get(0).target();
    assertInstanceOf(SupervisorAgentBlueprint.class, devRoute);
    SupervisorAgentBlueprint supBp = (SupervisorAgentBlueprint) devRoute;
    assertEquals("DevSupervisor", supBp.name());
    assertEquals(2, supBp.workers().size());
    assertEquals("CodeAgent", supBp.workers().get(0).worker().name());

    // Second route is a plain agent
    InteractableBlueprint salesRoute = routerBp.routes().get(1).target();
    assertInstanceOf(AgentBlueprint.class, salesRoute);
    assertEquals("Sales", salesRoute.name());
  }
}
