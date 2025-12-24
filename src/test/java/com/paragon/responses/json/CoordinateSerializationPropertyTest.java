package com.paragon.responses.json;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.responses.ResponsesApiObjectMapper;
import com.paragon.responses.spec.ClickAction;
import com.paragon.responses.spec.ClickButton;
import com.paragon.responses.spec.Coordinate;
import com.paragon.responses.spec.MoveAction;
import net.jqwik.api.*;

/**
 * Property-based tests for Coordinate serialization.
 *
 * <p>Feature: responses-api-jackson-serialization Property 7: Coordinate field separation
 * Validates: Requirements 4.1
 */
class CoordinateSerializationPropertyTest {

  private final ObjectMapper mapper = ResponsesApiObjectMapper.create();

  /**
   * Property 7: Coordinate field separation
   *
   * <p>For any object containing a Coordinate field with @JsonUnwrapped annotation, when serialized
   * to JSON, the coordinate should appear as separate "x" and "y" numeric fields at the same level
   * as other fields (not as a nested object).
   */
  @Property(tries = 100)
  void coordinateSerializesToSeparateXAndYFields(@ForAll("coordinates") Coordinate coord)
      throws Exception {
    // Use MoveAction which has @JsonUnwrapped Coordinate
    MoveAction action = new MoveAction(coord);

    // Serialize
    String json = mapper.writeValueAsString(action);

    // Parse JSON to verify structure
    JsonNode node = mapper.readTree(json);

    // Verify x and y fields exist at the top level
    assertTrue(node.has("x"), "JSON should have 'x' field");
    assertTrue(node.has("y"), "JSON should have 'y' field");

    // Verify they are numeric
    assertTrue(node.get("x").isNumber(), "'x' field should be numeric");
    assertTrue(node.get("y").isNumber(), "'y' field should be numeric");

    // Verify values match
    assertEquals(coord.x(), node.get("x").asInt(), "x value should match");
    assertEquals(coord.y(), node.get("y").asInt(), "y value should match");

    // Verify there's no nested "coordinate" object
    assertFalse(
        json.contains("\"coordinate\""), "JSON should not contain a nested 'coordinate' field");
  }

  @Property(tries = 100)
  void coordinateRoundTripPreservesValues(@ForAll("coordinates") Coordinate original)
      throws Exception {
    // Use MoveAction for round-trip test
    MoveAction action = new MoveAction(original);

    // Serialize
    String json = mapper.writeValueAsString(action);

    // Deserialize
    MoveAction deserialized = mapper.readValue(json, MoveAction.class);

    // Verify round-trip
    assertNotNull(deserialized.coordinate(), "Deserialized coordinate should not be null");
    assertEquals(original.x(), deserialized.coordinate().x(), "x value should be preserved");
    assertEquals(original.y(), deserialized.coordinate().y(), "y value should be preserved");
  }

  @Property(tries = 100)
  void coordinateWithNegativeValuesSerializesCorrectly(
      @ForAll("negativeInts") int x, @ForAll("negativeInts") int y) throws Exception {

    Coordinate coord = Coordinate.Factory.getCoordinate(x, y);
    MoveAction action = new MoveAction(coord);

    // Serialize
    String json = mapper.writeValueAsString(action);

    // Parse and verify
    JsonNode node = mapper.readTree(json);
    assertEquals(x, node.get("x").asInt(), "Negative x should serialize correctly");
    assertEquals(y, node.get("y").asInt(), "Negative y should serialize correctly");
  }

  @Property(tries = 100)
  void coordinateWithZeroValuesSerializesCorrectly() throws Exception {
    Coordinate coord = Coordinate.Factory.getCoordinate(0, 0);
    MoveAction action = new MoveAction(coord);

    // Serialize
    String json = mapper.writeValueAsString(action);

    // Parse and verify
    JsonNode node = mapper.readTree(json);
    assertEquals(0, node.get("x").asInt(), "Zero x should serialize correctly");
    assertEquals(0, node.get("y").asInt(), "Zero y should serialize correctly");
  }

  @Property(tries = 100)
  void coordinateWithOtherFieldsSerializesCorrectly(@ForAll("coordinates") Coordinate coord)
      throws Exception {
    // Use ClickAction which has both button and coordinate fields
    ClickAction action = new ClickAction(ClickButton.LEFT, coord);

    // Serialize
    String json = mapper.writeValueAsString(action);

    // Parse and verify
    JsonNode node = mapper.readTree(json);

    // Verify button field exists
    assertTrue(node.has("button"), "JSON should have 'button' field");

    // Verify x and y fields exist alongside button
    assertTrue(node.has("x"), "JSON should have 'x' field");
    assertTrue(node.has("y"), "JSON should have 'y' field");

    // Verify coordinate values
    assertEquals(coord.x(), node.get("x").asInt(), "x value should match");
    assertEquals(coord.y(), node.get("y").asInt(), "y value should match");
  }

  // Arbitraries (generators)

  @Provide
  Arbitrary<Coordinate> coordinates() {
    return Arbitraries.integers()
        .between(-10000, 10000)
        .flatMap(
            x ->
                Arbitraries.integers()
                    .between(-10000, 10000)
                    .map(y -> Coordinate.Factory.getCoordinate(x, y)));
  }

  @Provide
  Arbitrary<Integer> negativeInts() {
    return Arbitraries.integers().between(-1000, -1);
  }
}
