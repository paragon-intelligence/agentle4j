package com.paragon.responses.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.paragon.responses.spec.FunctionToolCallOutput;
import java.io.IOException;

/**
 * Custom Jackson serializer for {@link FunctionToolCallOutput}.
 *
 * <p>The OpenAI Responses API requires:
 * <ul>
 *   <li>{@code type}: always {@code "function_call_output"}
 *   <li>{@code call_id}: the matching function call ID
 *   <li>{@code output}: a plain <b>string</b> (not an object)
 * </ul>
 *
 * <p>Without this serializer, Jackson would serialize {@code output} as a nested JSON object
 * (e.g., {@code {"text": "..."}}) because {@code FunctionToolCallOutputKind} has no type
 * information. That causes OpenRouter (and OpenAI) to reject the payload with
 * {@code "expected string, received object"}.
 */
public class FunctionToolCallOutputSerializer extends JsonSerializer<FunctionToolCallOutput> {

  @Override
  public void serialize(
      FunctionToolCallOutput value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartObject();
    gen.writeStringField("type", "function_call_output");
    gen.writeStringField("call_id", value.callId());
    // output() is FunctionToolCallOutputKind — Text.toString() returns the plain text
    gen.writeStringField("output", value.output().toString());
    if (value.id() != null) {
      gen.writeStringField("id", value.id());
    }
    if (value.status() != null) {
      gen.writeStringField("status", value.status().name().toLowerCase());
    }
    gen.writeEndObject();
  }
}
