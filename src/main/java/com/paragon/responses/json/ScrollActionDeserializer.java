package com.paragon.responses.json;

import com.paragon.responses.spec.Coordinate;
import com.paragon.responses.spec.ScrollAction;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Custom deserializer for ScrollAction to handle @JsonUnwrapped Coordinate.
 *
 * <p>Jackson doesn't support @JsonUnwrapped with @JsonCreator (which records implicitly use), so we
 * need a custom deserializer to read the unwrapped x and y fields.
 */
public class ScrollActionDeserializer extends ValueDeserializer<ScrollAction> {

  @Override
  public ScrollAction deserialize(JsonParser p, DeserializationContext ctxt)
      throws tools.jackson.core.JacksonException {
    JsonNode node = p.readValueAsTree();

    int scrollX = node.has("scroll_x") ? node.get("scroll_x").asInt() : 0;
    int scrollY = node.has("scroll_y") ? node.get("scroll_y").asInt() : 0;

    if (!node.has("x") || node.get("x").isNull()) {
      throw MismatchedInputException.from(
          p, ScrollAction.class, "Missing required field 'x' for Coordinate");
    }
    if (!node.has("y") || node.get("y").isNull()) {
      throw MismatchedInputException.from(
          p, ScrollAction.class, "Missing required field 'y' for Coordinate");
    }

    int x = node.get("x").asInt();
    int y = node.get("y").asInt();

    return new ScrollAction(scrollX, scrollY, new Coordinate(x, y));
  }
}
