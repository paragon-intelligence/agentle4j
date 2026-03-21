package com.paragon.responses.spec;

import java.util.Map;
import org.jspecify.annotations.NonNull;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** A tool that allows the model to execute shell commands in a local environment. */
public record LocalShellTool() implements Tool {
  @Override
  public @NonNull String toToolChoice(ObjectMapper mapper) throws JacksonException {
    return mapper.writeValueAsString(Map.of("type", "local_shell"));
  }
}
