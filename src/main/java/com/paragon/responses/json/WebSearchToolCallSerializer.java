package com.paragon.responses.json;

import com.paragon.responses.spec.WebSearchToolCall;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

/** Serializer that enforces the correct wire format for {@link WebSearchToolCall}. */
public class WebSearchToolCallSerializer extends StdSerializer<WebSearchToolCall> {

  public WebSearchToolCallSerializer() {
    super(WebSearchToolCall.class);
  }

  @Override
  public void serializeWithType(
      WebSearchToolCall value,
      JsonGenerator gen,
      SerializationContext provider,
      TypeSerializer typeSer)
      throws tools.jackson.core.JacksonException {
    serialize(value, gen, provider);
  }

  @Override
  public void serialize(WebSearchToolCall value, JsonGenerator gen, SerializationContext provider)
      throws tools.jackson.core.JacksonException {
    gen.writeStartObject();
    gen.writeStringProperty("type", "web_search_call");
    gen.writeStringProperty("id", value.id());
    gen.writePOJOProperty("action", value.action());
    gen.writeStringProperty("status", value.status());
    gen.writeEndObject();
  }
}
