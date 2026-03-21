package com.paragon.responses.json;

import com.paragon.responses.spec.FunctionToolCallOutput;
import com.paragon.responses.spec.FunctionToolCallOutputStatus;
import com.paragon.responses.spec.Text;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

/**
 * Custom deserializer for {@link FunctionToolCallOutput}.
 *
 * <p>The wire format written by {@link FunctionToolCallOutputSerializer} encodes {@code output} as
 * a plain string (via {@code Text.toString()}). This deserializer wraps that string back into a
 * {@link Text} instance.
 *
 * <p><b>Limitation:</b> {@link com.paragon.responses.spec.Image} and {@link
 * com.paragon.responses.spec.File} outputs are serialized via their {@code toString()} which is not
 * reversible. Deserializing such outputs produces a {@link Text} wrapping the {@code toString()}
 * representation — structurally safe but semantically lossy.
 */
public class FunctionToolCallOutputDeserializer extends ValueDeserializer<FunctionToolCallOutput> {

  @Override
  public FunctionToolCallOutput deserialize(JsonParser p, DeserializationContext ctxt)
      throws tools.jackson.core.JacksonException {
    JsonNode node = p.readValueAsTree();

    String callId = node.get("call_id").asText();

    // output is written as a plain string; wrap it back as Text
    Text output = new Text(node.get("output").asText());

    String id = node.has("id") && !node.get("id").isNull() ? node.get("id").asText() : null;

    FunctionToolCallOutputStatus status = null;
    if (node.has("status") && !node.get("status").isNull()) {
      status = FunctionToolCallOutputStatus.valueOf(node.get("status").asText().toUpperCase());
    }

    return new FunctionToolCallOutput(callId, output, id, status);
  }
}
