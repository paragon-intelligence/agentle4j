package com.paragon.parsing;

public record ParsingOptions() {
  public static ParsingOptions withDefaultOptions() {
    return new ParsingOptions();
  }
}
