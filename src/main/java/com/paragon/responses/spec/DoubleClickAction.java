package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.jspecify.annotations.NonNull;

/**
 * A double click action.
 *
 * @param coordinate The x and y coordinate where the double click occurred.
 */
public record DoubleClickAction(@JsonUnwrapped @NonNull Coordinate coordinate)
    implements ComputerUseAction {
  @Override
  public @NonNull String toString() {
    return String.format(
        """
        <computer_use_action:double_click>
            <coordinate>%s</coordinate>
        </computer_use_action:double_click>
        """,
        coordinate);
  }
}
