package com.paragon.responses.spec;

import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * A collection of keypresses the model would like to perform.
 *
 * @param keys The combination of keys the model is requesting to be pressed. This is an array of
 *     strings, each representing a key.
 */
public record KeyPressAction(@NonNull List<String> keys) implements ComputerUseAction {
  @Override
  public @NonNull String toString() {
    return String.format(
        """
        <computer_use_action:keypress>
            <keys>%s</keys>
        </computer_use_action:keypress>
        """,
        keys);
  }
}
