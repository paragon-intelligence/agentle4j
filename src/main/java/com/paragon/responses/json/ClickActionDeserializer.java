package com.paragon.responses.json;

import com.paragon.responses.spec.ClickAction;
import com.paragon.responses.spec.ClickButton;
import com.paragon.responses.spec.Coordinate;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Custom deserializer for ClickAction to handle @JsonUnwrapped Coordinate.
 *
 * <p>Jackson doesn't support @JsonUnwrapped with @JsonCreator (which records implicitly use), so we
 * need a custom deserializer to read the unwrapped x and y fields.
 */
public class ClickActionDeserializer extends ValueDeserializer<ClickAction> {

  @Override
  public ClickAction deserialize(JsonParser p, DeserializationContext ctxt)
      throws tools.jackson.core.JacksonException {
    JsonNode node = p.readValueAsTree();

    ClickButton button =
        node.has("button")
            ? ClickButton.valueOf(node.get("button").asText().toUpperCase())
            : ClickButton.LEFT;

    if (!node.has("x") || node.get("x").isNull()) {
      throw MismatchedInputException.from(
          p, ClickAction.class, "Missing required field 'x' for Coordinate");
    }
    if (!node.has("y") || node.get("y").isNull()) {
      throw MismatchedInputException.from(
          p, ClickAction.class, "Missing required field 'y' for Coordinate");
    }

    int x = node.get("x").asInt();
    int y = node.get("y").asInt();

    return new ClickAction(button, new Coordinate(x, y));
  }
}
