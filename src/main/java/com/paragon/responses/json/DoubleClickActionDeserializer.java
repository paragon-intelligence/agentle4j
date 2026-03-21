package com.paragon.responses.json;

import com.paragon.responses.spec.Coordinate;
import com.paragon.responses.spec.DoubleClickAction;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Custom deserializer for DoubleClickAction to handle @JsonUnwrapped Coordinate.
 *
 * <p>Jackson doesn't support @JsonUnwrapped with @JsonCreator (which records implicitly use), so we
 * need a custom deserializer to read the unwrapped x and y fields.
 */
public class DoubleClickActionDeserializer extends ValueDeserializer<DoubleClickAction> {

  @Override
  public DoubleClickAction deserialize(JsonParser p, DeserializationContext ctxt)
      throws tools.jackson.core.JacksonException {
    JsonNode node = p.readValueAsTree();

    if (!node.has("x") || node.get("x").isNull()) {
      throw MismatchedInputException.from(
          p, DoubleClickAction.class, "Missing required field 'x' for Coordinate");
    }
    if (!node.has("y") || node.get("y").isNull()) {
      throw MismatchedInputException.from(
          p, DoubleClickAction.class, "Missing required field 'y' for Coordinate");
    }

    int x = node.get("x").asInt();
    int y = node.get("y").asInt();

    return new DoubleClickAction(new Coordinate(x, y));
  }
}
