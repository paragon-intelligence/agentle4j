package com.paragon.responses.json;

import com.paragon.responses.spec.McpToolCall;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

/** Serializer that enforces the correct wire format for {@link McpToolCall}. */
public class McpToolCallSerializer extends StdSerializer<McpToolCall> {

  public McpToolCallSerializer() {
    super(McpToolCall.class);
  }

  @Override
  public void serializeWithType(
      McpToolCall value, JsonGenerator gen, SerializationContext provider, TypeSerializer typeSer)
      throws tools.jackson.core.JacksonException {
    serialize(value, gen, provider);
  }

  @Override
  public void serialize(McpToolCall value, JsonGenerator gen, SerializationContext provider)
      throws tools.jackson.core.JacksonException {
    gen.writeStartObject();
    gen.writeStringProperty("type", "mcp_call");
    gen.writeStringProperty("id", value.id());
    gen.writeStringProperty("arguments", value.arguments());
    gen.writeStringProperty("name", value.name());
    gen.writeStringProperty("server_label", value.serverLabel());
    if (value.approvalRequestId() != null) {
      gen.writeStringProperty("approval_request_id", value.approvalRequestId());
    }
    if (value.error() != null) {
      gen.writeStringProperty("error", value.error());
    }
    if (value.output() != null) {
      gen.writeStringProperty("output", value.output());
    }
    if (value.status() != null) {
      gen.writeStringProperty("status", value.status().name().toLowerCase());
    }
    gen.writeEndObject();
  }
}
