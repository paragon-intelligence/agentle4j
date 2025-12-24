package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;

/** A wait action. */
public record WaitAction() implements ComputerUseAction {
  @Override
  public @NonNull String toString() {
    return "<computer_use_action:wait />";
  }
}
