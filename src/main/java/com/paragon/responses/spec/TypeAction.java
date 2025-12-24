package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;

/**
 * An action to type in text.
 *
 * @param text The text to type.
 */
public record TypeAction(@NonNull String text) implements ComputerUseAction {
  @Override
  public @NonNull String toString() {
    return String.format(
        """
        <computer_use_action:type>
            <text>%s</text>
        </computer_use_action:type>
        """,
        text);
  }
}
