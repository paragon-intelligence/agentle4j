package com.paragon.responses.spec;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/**
 * A tool that runs Python code to help generate a response to a prompt.
 *
 * @param container The code interpreter container. Can be a container ID or an object that
 *     specifies uploaded file IDs to make available to your code.
 */
public record CodeInterpreterTool(@NonNull String container) implements Tool {
  @Override
  public @NonNull String toToolChoice(ObjectMapper mapper) throws JacksonException {
    return mapper.writeValueAsString(Map.of("type", "code_interpreter"));
  }
}
