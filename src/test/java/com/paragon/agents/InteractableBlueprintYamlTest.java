package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.paragon.agents.InteractableBlueprint.*;
import java.util.*;
import org.junit.jupiter.api.*;

/** Tests for YAML serialization/deserialization of {@link InteractableBlueprint}. */
class InteractableBlueprintYamlTest {

  private ObjectMapper jsonMapper;
  private YAMLMapper yamlMapper;

  @BeforeEach
  void setUp() {
    jsonMapper =
        new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    yamlMapper = new YAMLMapper();
    yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    GuardrailRegistry.clear();
  }

  // ===== Agent Blueprint =====

  @Test
  void agentBlueprintYamlRoundtrip() {
    var responder = new ResponderBlueprint("OPEN_ROUTER", null, "OPENROUTER_API_KEY", null, null);
    var blueprint =
        new AgentBlueprint(
            "SupportAgent",
            "gpt-4o",
            new InstructionSource.Inline("You are a support agent."),
            10,
            0.7,
            null,
            null,
            responder,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            null,
            null);

    String yaml = blueprint.toYaml();
    assertNotNull(yaml);
    assertTrue(yaml.contains("SupportAgent"));
    assertTrue(yaml.contains("gpt-4o"));

    InteractableBlueprint restored = InteractableBlueprint.fromYaml(yaml);
    assertInstanceOf(AgentBlueprint.class, restored);
    AgentBlueprint agentBp = (AgentBlueprint) restored;
    assertEquals("SupportAgent", agentBp.name());
    assertEquals("gpt-4o", agentBp.model());
    assertEquals("You are a support agent.", agentBp.instructions().resolve());
    assertEquals(10, agentBp.maxTurns());
  }

  @Test
  void agentBlueprintWithStructuredOutput() {
    var responder = new ResponderBlueprint("OPEN_ROUTER", null, null, null, null);
    var blueprint =
        new AgentBlueprint(
            "ExtractorAgent",
            "gpt-4o",
            new InstructionSource.Inline("Extract data from text."),
            5,
            null,
            "java.lang.String",
            null,
            responder,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            null,
            null);

    String yaml = blueprint.toYaml();
    assertTrue(yaml.contains("java.lang.String"));

    InteractableBlueprint restored = InteractableBlueprint.fromYaml(yaml);
    AgentBlueprint bp = (AgentBlueprint) restored;
    assertEquals("java.lang.String", bp.outputType());
  }

  // ===== Router Blueprint =====

  @Test
  void routerBlueprintYamlRoundtrip() {
    var responder = new ResponderBlueprint("OPEN_ROUTER", null, null, null, null);
    var sales =
        new AgentBlueprint(
            "Sales", "gpt-4o",
            new InstructionSource.Inline("Sales agent"),
            5, null, null, null, responder,
            List.of(), List.of(), List.of(), List.of(), null, null);
    var support =
        new AgentBlueprint(
            "Support", "gpt-4o",
            new InstructionSource.Inline("Support agent"),
            5, null, null, null, responder,
            List.of(), List.of(), List.of(), List.of(), null, null);

    var router =
        new RouterAgentBlueprint(
            "Router", "gpt-4o",
            List.of(
                new RouteBlueprint(sales, "Sales inquiries"),
                new RouteBlueprint(support, "Support tickets")),
            null, responder, null);

    String yaml = router.toYaml();
    assertTrue(yaml.contains("Router"));
    assertTrue(yaml.contains("Sales inquiries"));

    InteractableBlueprint restored = InteractableBlueprint.fromYaml(yaml);
    assertInstanceOf(RouterAgentBlueprint.class, restored);
    RouterAgentBlueprint routerBp = (RouterAgentBlueprint) restored;
    assertEquals(2, routerBp.routes().size());
  }

  // ===== Supervisor Blueprint =====

  @Test
  void supervisorBlueprintYamlRoundtrip() {
    var responder = new ResponderBlueprint("OPEN_ROUTER", null, null, null, null);
    var worker =
        new AgentBlueprint(
            "Researcher", "gpt-4o",
            new InstructionSource.Inline("Do research"),
            5, null, null, null, responder,
            List.of(), List.of(), List.of(), List.of(), null, null);

    var supervisor =
        new SupervisorAgentBlueprint(
            "Manager", "gpt-4o", "Manage the team", 10,
            List.of(new WorkerBlueprint(worker, "Research specialist")),
            responder, null);

    String yaml = supervisor.toYaml();
    assertTrue(yaml.contains("Manager"));

    InteractableBlueprint restored = InteractableBlueprint.fromYaml(yaml);
    assertInstanceOf(SupervisorAgentBlueprint.class, restored);
    SupervisorAgentBlueprint supBp = (SupervisorAgentBlueprint) restored;
    assertEquals(1, supBp.workers().size());
    assertEquals("Research specialist", supBp.workers().get(0).description());
  }

  // ===== JSON ↔ YAML equivalence =====

  @Test
  void jsonAndYamlAreEquivalent() {
    var responder = new ResponderBlueprint("OPEN_ROUTER", null, "KEY", null, null);
    var blueprint =
        new AgentBlueprint(
            "Agent", "gpt-4o",
            new InstructionSource.Inline("Instructions"),
            10, 0.5, null, null, responder,
            List.of(), List.of(), List.of(), List.of(), null, null);

    String json = blueprint.toJson();
    String yaml = blueprint.toYaml();

    InteractableBlueprint fromJson = InteractableBlueprint.fromJson(json);
    InteractableBlueprint fromYaml = InteractableBlueprint.fromYaml(yaml);

    assertEquals(fromJson.name(), fromYaml.name());
    AgentBlueprint jsonBp = (AgentBlueprint) fromJson;
    AgentBlueprint yamlBp = (AgentBlueprint) fromYaml;
    assertEquals(jsonBp.model(), yamlBp.model());
    assertEquals(jsonBp.instructions().resolve(), yamlBp.instructions().resolve());
    assertEquals(jsonBp.maxTurns(), yamlBp.maxTurns());
    assertEquals(jsonBp.temperature(), yamlBp.temperature());
  }

  // ===== Hand-written YAML =====

  @Test
  void handWrittenYamlDeserialization() {
    String yaml = """
        type: agent
        name: CustomerService
        model: gpt-4o
        instructions: You are a customer service specialist.
        maxTurns: 15
        temperature: 0.3
        responder:
          provider: OPEN_ROUTER
          apiKeyEnvVar: OPENROUTER_API_KEY
        toolClassNames: []
        handoffs: []
        inputGuardrails: []
        outputGuardrails: []
        """;

    InteractableBlueprint bp = InteractableBlueprint.fromYaml(yaml);
    assertInstanceOf(AgentBlueprint.class, bp);
    AgentBlueprint agent = (AgentBlueprint) bp;

    assertEquals("CustomerService", agent.name());
    assertEquals("gpt-4o", agent.model());
    assertEquals("You are a customer service specialist.", agent.instructions().resolve());
    assertEquals(15, agent.maxTurns());
    assertEquals(0.3, agent.temperature());
  }

  @Test
  void handWrittenYamlWithFileInstructions() {
    String yaml = """
        type: agent
        name: FileAgent
        model: gpt-4o
        instructions:
          source: file
          path: /tmp/nonexistent-prompt.txt
        maxTurns: 5
        responder:
          provider: OPEN_ROUTER
          apiKeyEnvVar: OPENROUTER_API_KEY
        toolClassNames: []
        handoffs: []
        inputGuardrails: []
        outputGuardrails: []
        """;

    InteractableBlueprint bp = InteractableBlueprint.fromYaml(yaml);
    AgentBlueprint agent = (AgentBlueprint) bp;
    assertInstanceOf(InstructionSource.FileRef.class, agent.instructions());
    assertEquals("/tmp/nonexistent-prompt.txt",
        ((InstructionSource.FileRef) agent.instructions()).path());
  }

  @Test
  void handWrittenYamlWithProviderInstructions() {
    String yaml = """
        type: agent
        name: LangfuseAgent
        model: gpt-4o
        instructions:
          source: provider
          providerId: langfuse
          promptId: support-v2
          filters:
            label: production
        maxTurns: 10
        responder:
          provider: OPEN_ROUTER
          apiKeyEnvVar: OPENROUTER_API_KEY
        toolClassNames: []
        handoffs: []
        inputGuardrails: []
        outputGuardrails: []
        """;

    InteractableBlueprint bp = InteractableBlueprint.fromYaml(yaml);
    AgentBlueprint agent = (AgentBlueprint) bp;
    assertInstanceOf(InstructionSource.ProviderRef.class, agent.instructions());
    InstructionSource.ProviderRef ref = (InstructionSource.ProviderRef) agent.instructions();
    assertEquals("langfuse", ref.providerId());
    assertEquals("support-v2", ref.promptId());
    assertEquals("production", ref.filters().get("label"));
  }
}
