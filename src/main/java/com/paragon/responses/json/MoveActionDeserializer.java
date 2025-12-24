package com.paragon.responses.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.paragon.responses.spec.Coordinate;
import com.paragon.responses.spec.MoveAction;
import java.io.IOException;

/**
 * Custom deserializer for MoveAction to handle @JsonUnwrapped Coordinate.
 *
 * <p>Jackson doesn't support @JsonUnwrapped with @JsonCreator (which records implicitly use), so we
 * need a custom deserializer to read the unwrapped x and y fields.
 */
public class MoveActionDeserializer extends JsonDeserializer<MoveAction> {

  @Override
  public MoveAction deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode node = p.getCodec().readTree(p);

    if (!node.has("x") || node.get("x").isNull()) {
      throw MismatchedInputException.from(
          p, MoveAction.class, "Missing required field 'x' for Coordinate");
    }
    if (!node.has("y") || node.get("y").isNull()) {
      throw MismatchedInputException.from(
          p, MoveAction.class, "Missing required field 'y' for Coordinate");
    }

    int x = node.get("x").asInt();
    int y = node.get("y").asInt();

    return new MoveAction(new Coordinate(x, y));
  }
}
