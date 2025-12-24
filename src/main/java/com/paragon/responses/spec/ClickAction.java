package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.jspecify.annotations.NonNull;

/**
 * A click action.
 *
 * @param button Indicates which mouse button was pressed during the click. One of left, right,
 *     wheel, back, or forward.
 * @param coordinate The x and y coordinate where the click occurred.
 */
public record ClickAction(
    @NonNull ClickButton button, @JsonUnwrapped @NonNull Coordinate coordinate)
    implements ComputerUseAction {
  @Override
  public @NonNull String toString() {
    return String.format(
        """
        <computer_use_action:click>
            <button>%s</button>
            <coordinate>%s</coordinate>
        </computer_use_action:click>
        """,
        button, coordinate);
  }
}
