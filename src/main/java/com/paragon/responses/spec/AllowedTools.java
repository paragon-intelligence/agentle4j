package com.paragon.responses.spec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/**
 * Constrains the tools available to the model to a pre-defined set.
 *
 * @param mode Constrains the tools available to the model to a pre-defined set.
 * @param tools A list of tool definitions that the model should be allowed to call.
 */
public record AllowedTools(@NonNull AllowedToolsMode mode, @NonNull List<Tool> tools)
    implements ToolChoice {
  @Override
  public String toToolChoice(ObjectMapper mapper) throws JsonProcessingException {
    return mapper.writeValueAsString(Map.of("mode", mode, "tools", tools));
  }
}
