package com.paragon.responses.spec;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NonNull;

public interface ToolChoiceRepresentable {
  String toToolChoice(ObjectMapper mapper) throws JacksonException;

  default @NonNull String toToolChoice() throws JacksonException {
    return toToolChoice(new ObjectMapper());
  }
}
