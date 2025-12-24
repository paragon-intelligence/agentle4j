package com.paragon.responses.spec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  public @NonNull String toToolChoice(ObjectMapper mapper) throws JsonProcessingException {
    return mapper.writeValueAsString(Map.of("type", "code_interpreter"));
  }
}
