package com.paragon.responses.spec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NonNull;

public interface ToolChoiceRepresentable {
  String toToolChoice(ObjectMapper mapper) throws JsonProcessingException;

  default @NonNull String toToolChoice() throws JsonProcessingException {
    return toToolChoice(new ObjectMapper());
  }
}
