package com.paragon.responses.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.paragon.responses.spec.WebSearchToolCall;
import java.io.IOException;

/** Serializer that enforces the correct wire format for {@link WebSearchToolCall}. */
public class WebSearchToolCallSerializer extends StdSerializer<WebSearchToolCall> {

  public WebSearchToolCallSerializer() {
    super(WebSearchToolCall.class);
  }

  @Override
  public void serializeWithType(
      WebSearchToolCall value,
      JsonGenerator gen,
      SerializerProvider provider,
      TypeSerializer typeSer)
      throws IOException {
    serialize(value, gen, provider);
  }

  @Override
  public void serialize(WebSearchToolCall value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartObject();
    gen.writeStringField("type", "web_search_call");
    gen.writeStringField("id", value.id());
    gen.writeObjectField("action", value.action());
    gen.writeStringField("status", value.status());
    gen.writeEndObject();
  }
}

