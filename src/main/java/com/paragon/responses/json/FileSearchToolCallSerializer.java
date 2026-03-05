package com.paragon.responses.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.paragon.responses.spec.FileSearchToolCall;
import java.io.IOException;

/** Serializer that enforces the correct wire format for {@link FileSearchToolCall}. */
public class FileSearchToolCallSerializer extends StdSerializer<FileSearchToolCall> {

  public FileSearchToolCallSerializer() {
    super(FileSearchToolCall.class);
  }

  @Override
  public void serializeWithType(
      FileSearchToolCall value,
      JsonGenerator gen,
      SerializerProvider provider,
      TypeSerializer typeSer)
      throws IOException {
    serialize(value, gen, provider);
  }

  @Override
  public void serialize(FileSearchToolCall value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartObject();
    gen.writeStringField("type", "file_search_call");
    gen.writeStringField("id", value.id());
    gen.writeObjectField("queries", value.queries());
    gen.writeStringField("status", value.status().name().toLowerCase());
    if (value.fileSearchToolCallResultList() != null) {
      gen.writeObjectField("results", value.fileSearchToolCallResultList());
    }
    gen.writeEndObject();
  }
}

