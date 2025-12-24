package com.paragon.responses.spec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A custom tool that processes input using a specified format. Learn more about custom tools
 *
 * @param name The name of the custom tool, used to identify it in tool calls.
 * @param description Optional description of the custom tool, used to provide more context.
 * @param format The input format for the custom tool. Default is unconstrained text.
 */
public record CustomTool(
    @NonNull String name, @Nullable String description, @Nullable CustomToolInputFormat format)
    implements Tool {
  @Override
  public @NonNull String toToolChoice(ObjectMapper mapper) throws JsonProcessingException {
    return mapper.writeValueAsString(Map.of("name", name, "type", "function"));
  }
}
