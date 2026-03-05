package com.paragon.responses.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.paragon.responses.spec.McpToolCall;
import java.io.IOException;

/** Serializer that enforces the correct wire format for {@link McpToolCall}. */
public class McpToolCallSerializer extends StdSerializer<McpToolCall> {

  public McpToolCallSerializer() {
    super(McpToolCall.class);
  }

  @Override
  public void serializeWithType(
      McpToolCall value,
      JsonGenerator gen,
      SerializerProvider provider,
      TypeSerializer typeSer)
      throws IOException {
    serialize(value, gen, provider);
  }

  @Override
  public void serialize(McpToolCall value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartObject();
    gen.writeStringField("type", "mcp_call");
    gen.writeStringField("id", value.id());
    gen.writeStringField("arguments", value.arguments());
    gen.writeStringField("name", value.name());
    gen.writeStringField("server_label", value.serverLabel());
    if (value.approvalRequestId() != null) {
      gen.writeStringField("approval_request_id", value.approvalRequestId());
    }
    if (value.error() != null) {
      gen.writeStringField("error", value.error());
    }
    if (value.output() != null) {
      gen.writeStringField("output", value.output());
    }
    if (value.status() != null) {
      gen.writeStringField("status", value.status().name().toLowerCase());
    }
    gen.writeEndObject();
  }
}

