package com.paragon.responses.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.paragon.responses.spec.ClickAction;
import com.paragon.responses.spec.ClickButton;
import com.paragon.responses.spec.Coordinate;
import java.io.IOException;

/**
 * Custom deserializer for ClickAction to handle @JsonUnwrapped Coordinate.
 *
 * <p>Jackson doesn't support @JsonUnwrapped with @JsonCreator (which records implicitly use), so we
 * need a custom deserializer to read the unwrapped x and y fields.
 */
public class ClickActionDeserializer extends JsonDeserializer<ClickAction> {

  @Override
  public ClickAction deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode node = p.getCodec().readTree(p);

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
