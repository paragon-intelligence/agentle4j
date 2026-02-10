package com.paragon.responses.spec;

public enum MessageRole {
  DEVELOPER,
  USER,
  ASSISTANT;

  @Override
  public String toString() {
    return name().toLowerCase();
  }
}
