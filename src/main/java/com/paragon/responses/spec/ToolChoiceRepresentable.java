package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public interface ToolChoiceRepresentable {
  String toToolChoice(ObjectMapper mapper) throws JacksonException;

  default @NonNull String toToolChoice() throws JacksonException {
    return toToolChoice(new ObjectMapper());
  }
}
