package com.paragon.responses.json;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
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
      SerializationContext provider,
      TypeSerializer typeSer)
      throws tools.jackson.core.JacksonException {
    serialize(value, gen, provider);
  }

  @Override
  public void serialize(FileSearchToolCall value, JsonGenerator gen, SerializationContext provider)
      throws tools.jackson.core.JacksonException {
    gen.writeStartObject();
    gen.writeStringProperty("type", "file_search_call");
    gen.writeStringProperty("id", value.id());
    gen.writePOJOProperty("queries", value.queries());
    gen.writeStringProperty("status", value.status().name().toLowerCase());
    if (value.fileSearchToolCallResultList() != null) {
      gen.writePOJOProperty("results", value.fileSearchToolCallResultList());
    }
    gen.writeEndObject();
  }
}

