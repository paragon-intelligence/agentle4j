package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.jspecify.annotations.NonNull;

/**
 * A scroll action.
 *
 * @param scrollX The horizontal scroll distance.
 * @param scrollY The vertical scroll distance.
 * @param coordinate The x and y coordinate where the scroll occurred.
 */
public record ScrollAction(
    @NonNull Integer scrollX,
    @NonNull Integer scrollY,
    @JsonUnwrapped @NonNull Coordinate coordinate)
    implements ComputerUseAction {
  @Override
  public @NonNull String toString() {
    return String.format(
        """
        <computer_use_action:scroll>
            <scroll_x>%s</scroll_x>
            <scroll_y>%s</scroll_y>
            <coordinate>%s</coordinate>
        </computer_use_action:scroll>
        """,
        scrollX, scrollY, coordinate);
  }
}
