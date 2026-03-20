package com.paragon.responses.spec;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
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
  public String toToolChoice(ObjectMapper mapper) throws JacksonException {
    return mapper.writeValueAsString(Map.of("mode", mode, "tools", tools));
  }
}
