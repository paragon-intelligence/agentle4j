package com.paragon.responses.spec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/**
 * A tool that controls a virtual computer. Learn more about the computer tool.
 *
 * @param displayHeight The height of the computer display.
 * @param displayWidth The width of the computer display.
 * @param environment The type of computer environment to control. See {@link
 *     ComputerUseEnvironment}
 */
public record ComputerUseTool(
    @NonNull Integer displayHeight,
    @NonNull Integer displayWidth,
    @NonNull ComputerUseEnvironment environment)
    implements Tool {
  @Override
  public @NonNull String toToolChoice(ObjectMapper mapper) throws JsonProcessingException {
    return mapper.writeValueAsString(Map.of("type", "computer_use"));
  }
}
