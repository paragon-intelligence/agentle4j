package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.paragon.agents.InteractableBlueprint.AgentBlueprint;
import com.paragon.agents.InteractableBlueprint.ResponderBlueprint;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AgentDefinition} — JSON round-trip, blueprint conversion, and structural
 * integrity.
 */
class AgentDefinitionTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper =
        new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.INDENT_OUTPUT);
  }

  // ===== Serialization / Deserialization =====

  @Test
  void minimalDefinition_roundTripsViaJson() throws Exception {
    AgentDefinition original =
        new AgentDefinition(
            "TestAgent",
            "openai/gpt-4o-mini",
            "You are a test agent.",
            5,
            null, // temperature
            null, // tools
            null, // inputGuardrails
            null, // outputGuardrails
            null, // handoffs
            null); // contextManagement

    String json = mapper.writeValueAsString(original);
    AgentDefinition restored = mapper.readValue(json, AgentDefinition.class);

    assertEquals(original.name(), restored.name());
    assertEquals(original.model(), restored.model());
    assertEquals(original.instructions(), restored.instructions());
    assertEquals(original.maxTurns(), restored.maxTurns());
    assertNull(restored.temperature());
    assertNull(restored.toolClassNames());
    assertNull(restored.inputGuardrails());
    assertNull(restored.outputGuardrails());
    assertNull(restored.handoffs());
    assertNull(restored.contextManagement());
  }

  @Test
  void fullDefinition_roundTripsViaJson() throws Exception {
    AgentDefinition original =
        new AgentDefinition(
            "SupportAgent",
            "openai/gpt-4o",
            "You are a professional support agent.",
            15,
            0.3,
            List.of("com.example.tools.SearchTool"),
            List.of(new AgentDefinition.GuardrailDef("max_length", null)),
            List.of(new AgentDefinition.GuardrailDef(null, "com.example.guards.PiiFilter")),
            List.of(
                new AgentDefinition.HandoffAgentDef(
                    "escalate_billing",
                    "Transfer billing issues",
                    new AgentDefinition(
                        "BillingAgent",
                        "openai/gpt-4o",
                        "Handle billing.",
                        10,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null))),
            new AgentDefinition.ContextDef("sliding", 4000, true, null, null, null));

    String json = mapper.writeValueAsString(original);
    AgentDefinition restored = mapper.readValue(json, AgentDefinition.class);

    assertEquals("SupportAgent", restored.name());
    assertEquals("openai/gpt-4o", restored.model());
    assertEquals(15, restored.maxTurns());
    assertEquals(0.3, restored.temperature());
    assertEquals(1, restored.toolClassNames().size());
    assertEquals("com.example.tools.SearchTool", restored.toolClassNames().get(0));
    assertEquals(1, restored.inputGuardrails().size());
    assertEquals("max_length", restored.inputGuardrails().get(0).registryId());
    assertEquals(1, restored.outputGuardrails().size());
    assertEquals("com.example.guards.PiiFilter", restored.outputGuardrails().get(0).className());
    assertEquals(1, restored.handoffs().size());
    assertEquals("escalate_billing", restored.handoffs().get(0).name());
    assertEquals("BillingAgent", restored.handoffs().get(0).target().name());
    assertNotNull(restored.contextManagement());
    assertEquals("sliding", restored.contextManagement().strategyType());
    assertEquals(4000, restored.contextManagement().maxTokens());
    assertTrue(restored.contextManagement().preserveDeveloperMessages());
  }

  @Test
  void jsonContainsPropertyDescriptions() throws Exception {
    // Verify that the schema generator can extract descriptions
    var schemaGen =
        new com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator(mapper);
    var schema = schemaGen.generateSchema(AgentDefinition.class);
    String schemaJson = mapper.writeValueAsString(schema);

    // Key descriptions should be present in the schema
    assertTrue(schemaJson.contains("Unique name for this agent"));
    assertTrue(schemaJson.contains("LLM model identifier"));
    assertTrue(schemaJson.contains("System prompt"));
    assertTrue(schemaJson.contains("Maximum number of LLM turns"));
  }

  // ===== Blueprint Conversion =====

  @Test
  void toBlueprint_producesValidAgentBlueprint() {
    AgentDefinition def =
        new AgentDefinition(
            "TestAgent",
            "openai/gpt-4o",
            "Test instructions.",
            10,
            0.7,
            List.of("com.example.tools.SearchTool"),
            List.of(new AgentDefinition.GuardrailDef("max_length", null)),
            null,
            null,
            new AgentDefinition.ContextDef("sliding", 4000, true, null, null, null));

    ResponderBlueprint responderBp =
        new ResponderBlueprint("OPEN_ROUTER", null, "OPENROUTER_API_KEY", null, null);
    AgentBlueprint blueprint = def.toBlueprint(responderBp);

    assertEquals("TestAgent", blueprint.name());
    assertEquals("openai/gpt-4o", blueprint.model());
    assertEquals("Test instructions.", blueprint.instructions());
    assertEquals(10, blueprint.maxTurns());
    assertEquals(0.7, blueprint.temperature());
    assertEquals(1, blueprint.toolClassNames().size());
    assertEquals(1, blueprint.inputGuardrails().size());
    assertEquals(0, blueprint.outputGuardrails().size());
    assertEquals(0, blueprint.handoffs().size());
    assertNotNull(blueprint.contextManagement());
    assertEquals("sliding", blueprint.contextManagement().strategyType());
  }

  // ===== fromBlueprint Conversion =====

  @Test
  void fromBlueprint_extractsBehavioralFields() {
    ResponderBlueprint responderBp =
        new ResponderBlueprint("OPEN_ROUTER", null, "OPENROUTER_API_KEY", null, null);

    AgentBlueprint blueprint =
        new AgentBlueprint(
            "OriginalAgent",
            "openai/gpt-4o",
            "Original instructions.",
            8,
            0.5,
            null, // outputType
            null, // traceMetadata
            responderBp,
            List.of("com.example.SomeTool"),
            List.of(),
            List.of(new InteractableBlueprint.GuardrailReference(null, "profanity_filter")),
            List.of(),
            null);

    AgentDefinition def = AgentDefinition.fromBlueprint(blueprint);

    assertEquals("OriginalAgent", def.name());
    assertEquals("openai/gpt-4o", def.model());
    assertEquals("Original instructions.", def.instructions());
    assertEquals(8, def.maxTurns());
    assertEquals(0.5, def.temperature());
    assertNotNull(def.toolClassNames());
    assertEquals(1, def.toolClassNames().size());
    assertNotNull(def.inputGuardrails());
    assertEquals(1, def.inputGuardrails().size());
    assertEquals("profanity_filter", def.inputGuardrails().get(0).registryId());
    assertNull(def.outputGuardrails()); // empty list becomes null
    assertNull(def.handoffs()); // empty list becomes null
    assertNull(def.contextManagement()); // null stays null
  }

  @Test
  void fromBlueprint_andBackToBlueprint_preservesData() {
    ResponderBlueprint responderBp =
        new ResponderBlueprint("OPENAI", null, "OPENAI_API_KEY", null, null);

    AgentBlueprint original =
        new AgentBlueprint(
            "RoundTrip",
            "openai/gpt-4o-mini",
            "Round-trip test.",
            5,
            null,
            null,
            null,
            responderBp,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            null);

    AgentDefinition def = AgentDefinition.fromBlueprint(original);
    AgentBlueprint restored = def.toBlueprint(responderBp);

    assertEquals(original.name(), restored.name());
    assertEquals(original.model(), restored.model());
    assertEquals(original.instructions(), restored.instructions());
    assertEquals(original.maxTurns(), restored.maxTurns());
    assertEquals(original.temperature(), restored.temperature());
  }

  // ===== Hand-Written JSON =====

  @Test
  void handWrittenJson_deserializesCorrectly() throws Exception {
    String json =
        """
        {
          "name": "SpanishSupport",
          "model": "openai/gpt-4o",
          "instructions": "Eres un agente de soporte en español.",
          "maxTurns": 10,
          "temperature": 0.3,
          "inputGuardrails": [
            { "registryId": "max_length" }
          ],
          "handoffs": [
            {
              "name": "escalate_billing",
              "description": "Billing and payment issues",
              "target": {
                "name": "BillingAgent",
                "model": "openai/gpt-4o",
                "instructions": "Handle billing in Spanish.",
                "maxTurns": 5
              }
            }
          ],
          "contextManagement": {
            "strategyType": "sliding",
            "maxTokens": 4000,
            "preserveDeveloperMessages": true
          }
        }
        """;

    AgentDefinition def = mapper.readValue(json, AgentDefinition.class);

    assertEquals("SpanishSupport", def.name());
    assertEquals("openai/gpt-4o", def.model());
    assertEquals(10, def.maxTurns());
    assertEquals(0.3, def.temperature());
    assertNull(def.toolClassNames());
    assertNotNull(def.inputGuardrails());
    assertEquals(1, def.inputGuardrails().size());
    assertNotNull(def.handoffs());
    assertEquals(1, def.handoffs().size());
    assertEquals("BillingAgent", def.handoffs().get(0).target().name());
    assertNotNull(def.contextManagement());
    assertEquals("sliding", def.contextManagement().strategyType());
  }
}
