package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.jspecify.annotations.NonNull;

/**
 * A mouse move action.
 *
 * @param coordinate The x and y coordinate to move to.
 */
public record MoveAction(@JsonUnwrapped @NonNull Coordinate coordinate)
    implements ComputerUseAction {
  @Override
  public @NonNull String toString() {
    return String.format(
        """
        <computer_use_action:move>
            <coordinate>%s</coordinate>
        </computer_use_action:move>
        """,
        coordinate);
  }
}
