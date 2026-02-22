package com.paragon.agents.toolplan;

import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when a tool plan fails validation or execution.
 *
 * <p>Contains an optional {@link #stepId()} identifying which step caused the failure.
 */
public class ToolPlanException extends RuntimeException {

  private final @Nullable String stepId;

  public ToolPlanException(String message) {
    super(message);
    this.stepId = null;
  }

  public ToolPlanException(@Nullable String stepId, String message) {
    super(message);
    this.stepId = stepId;
  }

  public ToolPlanException(@Nullable String stepId, String message, Throwable cause) {
    super(message, cause);
    this.stepId = stepId;
  }

  /** Returns the step ID that caused this exception, or null if not step-specific. */
  public @Nullable String stepId() {
    return stepId;
  }
}
