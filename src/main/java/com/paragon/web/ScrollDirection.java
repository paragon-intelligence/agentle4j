package com.paragon.web;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Direction to scroll in.
 */
public enum ScrollDirection {
  UP("up"),
  DOWN("down"),
  LEFT("left"),
  RIGHT("right");

  private final String value;

  ScrollDirection(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
