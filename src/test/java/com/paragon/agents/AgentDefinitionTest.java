package com.paragon.agents;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.paragon.agents.InteractableBlueprint.AgentBlueprint;
import com.paragon.agents.InteractableBlueprint.GuardrailReference;
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
            "You are a test agent.",
            5,
            null, // temperature
            null, // toolNames
            null, // inputGuardrails
            null, // outputGuardrails
            null, // handoffs
            null); // contextManagement

    String json = mapper.writeValueAsString(original);
    AgentDefinition restored = mapper.readValue(json, AgentDefinition.class);

    assertEquals(original.name(), restored.name());
    assertEquals(original.instructions(), restored.instructions());
    assertEquals(original.maxTurns(), restored.maxTurns());
    assertNull(restored.temperature());
    assertNull(restored.toolNames());
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
            "You are a professional support agent.",
            15,
            0.3,
            List.of("search_kb", "create_ticket"),
            List.of("profanity_filter", "max_length"),
            List.of("no_pii"),
            List.of(
                new AgentDefinition.HandoffAgentDef(
                    "escalate_billing",
                    "Transfer billing issues",
                    new AgentDefinition(
                        "BillingAgent",
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
    assertEquals(15, restored.maxTurns());
    assertEquals(0.3, restored.temperature());
    assertEquals(List.of("search_kb", "create_ticket"), restored.toolNames());
    assertEquals(List.of("profanity_filter", "max_length"), restored.inputGuardrails());
    assertEquals(List.of("no_pii"), restored.outputGuardrails());
    assertEquals(1, restored.handoffs().size());
    assertEquals("escalate_billing", restored.handoffs().get(0).name());
    assertEquals("BillingAgent", restored.handoffs().get(0).target().name());
    assertNotNull(restored.contextManagement());
    assertEquals("sliding", restored.contextManagement().strategyType());
    assertEquals(4000, restored.contextManagement().maxTokens());
    assertTrue(restored.contextManagement().preserveDeveloperMessages());
  }

  @Test
  void jsonContainsFieldWithoutModel() throws Exception {
    AgentDefinition def =
        new AgentDefinition("TestAgent", "Instructions.", 5, null, null, null, null, null, null);

    String json = mapper.writeValueAsString(def);

    // Must NOT contain a "model" field — model is infrastructure, not LLM output
    assertFalse(json.contains("\"model\""));
    // Must contain behavioral fields
    assertTrue(json.contains("\"name\""));
    assertTrue(json.contains("\"instructions\""));
    assertTrue(json.contains("\"maxTurns\""));
  }

  @Test
  void jsonContainsPropertyDescriptions() throws Exception {
    var schemaGen =
        new com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator(mapper);
    var schema = schemaGen.generateSchema(AgentDefinition.class);
    String schemaJson = mapper.writeValueAsString(schema);

    // Descriptions from @JsonPropertyDescription should be in schema
    assertTrue(schemaJson.contains("Unique name for this agent"));
    assertTrue(schemaJson.contains("System prompt"));
    assertTrue(schemaJson.contains("Maximum number of LLM turns"));
    assertTrue(schemaJson.contains("Names of tools"));
    assertTrue(schemaJson.contains("Names of input guardrails"));
  }

  // ===== Blueprint Conversion =====

  @Test
  void toBlueprint_producesValidAgentBlueprint() {
    AgentDefinition def =
        new AgentDefinition(
            "TestAgent",
            "Test instructions.",
            10,
            0.7,
            List.of("search_kb"),
            List.of("max_length"),
            null,
            null,
            new AgentDefinition.ContextDef("sliding", 4000, true, null, null, null));

    ResponderBlueprint responderBp =
        new ResponderBlueprint("OPEN_ROUTER", null, "OPENROUTER_API_KEY", null, null);

    // No available tools for resolution in this unit test
    AgentBlueprint blueprint = def.toBlueprint(responderBp, "openai/gpt-4o", List.of());

    assertEquals("TestAgent", blueprint.name());
    assertEquals("openai/gpt-4o", blueprint.model()); // model injected externally
    assertEquals("Test instructions.", blueprint.instructions());
    assertEquals(10, blueprint.maxTurns());
    assertEquals(0.7, blueprint.temperature());
    // Guardrails preserved as registry ID references
    assertEquals(1, blueprint.inputGuardrails().size());
    assertEquals("max_length", blueprint.inputGuardrails().get(0).registryId());
    assertEquals(0, blueprint.outputGuardrails().size());
    assertNotNull(blueprint.contextManagement());
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
            List.of(),
            List.of(),
            List.of(new GuardrailReference(null, "profanity_filter")),
            List.of(),
            null);

    AgentDefinition def = AgentDefinition.fromBlueprint(blueprint);

    assertEquals("OriginalAgent", def.name());
    assertEquals("Original instructions.", def.instructions());
    assertEquals(8, def.maxTurns());
    assertEquals(0.5, def.temperature());
    assertNull(def.toolNames()); // no available tools to reverse-lookup
    assertNotNull(def.inputGuardrails());
    assertEquals(1, def.inputGuardrails().size());
    assertEquals("profanity_filter", def.inputGuardrails().get(0));
    assertNull(def.outputGuardrails()); // empty → null
    assertNull(def.handoffs());
    assertNull(def.contextManagement());
  }

  // ===== Hand-Written JSON =====

  @Test
  void handWrittenJson_deserializesCorrectly() throws Exception {
    String json =
        """
        {
          "name": "SpanishSupport",
          "instructions": "Eres un agente de soporte en español.",
          "maxTurns": 10,
          "temperature": 0.3,
          "toolNames": ["search_kb", "create_ticket"],
          "inputGuardrails": ["profanity_filter", "max_length"],
          "handoffs": [
            {
              "name": "escalate_billing",
              "description": "Billing and payment issues",
              "target": {
                "name": "BillingAgent",
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
    assertEquals(10, def.maxTurns());
    assertEquals(0.3, def.temperature());
    assertEquals(List.of("search_kb", "create_ticket"), def.toolNames());
    assertEquals(List.of("profanity_filter", "max_length"), def.inputGuardrails());
    assertNotNull(def.handoffs());
    assertEquals(1, def.handoffs().size());
    assertEquals("BillingAgent", def.handoffs().get(0).target().name());
    assertEquals("sliding", def.contextManagement().strategyType());
  }
}
