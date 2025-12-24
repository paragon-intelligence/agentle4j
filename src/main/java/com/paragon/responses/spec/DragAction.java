package com.paragon.responses.spec;

import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * A drag action.
 *
 * @param path An array of coordinates representing the path of the drag action. Coordinates will
 *     appear as an array of objects, e.g:
 *     <pre>
 *                                                                                                              [
 *                                                                                                                  { x: 100, y: 200 },
 *                                                                                                                  { x: 200, y: 300 }
 *                                                                                                              ]
 *
 *     </pre>
 */
public record DragAction(@NonNull List<Coordinate> path) implements ComputerUseAction {
  @Override
  public @NonNull String toString() {
    return String.format(
        """
        <computer_use_action:drag>
            <path>%s</path>
        </computer_use_action:drag>
        """,
        path);
  }
}
