package com.paragon.responses.spec;

import com.fasterxml.jackson.databind.ObjectMapper;

public enum ToolChoiceMode implements ToolChoice {
  NONE,
  AUTO,
  REQUIRED;

  @Override
  public String toToolChoice(ObjectMapper mapper) {
    return this.name().toLowerCase();
  }
}
