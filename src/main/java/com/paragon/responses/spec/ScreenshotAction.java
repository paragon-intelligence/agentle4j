package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;

/** A screenshot action. */
public record ScreenshotAction() implements ComputerUseAction {
  @Override
  public @NonNull String toString() {
    return "<computer_use_action:screenshot />";
  }
}
