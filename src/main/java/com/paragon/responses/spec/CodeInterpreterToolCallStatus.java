package com.paragon.responses.spec;

/**
 * The status of the code interpreter tool call. Valid values are {@code in_progress}, {@code
 * completed}, {@code incomplete}, {@code interpreting}, and {@code failed}.
 */
public enum CodeInterpreterToolCallStatus {
  IN_PROGRESS,
  COMPLETED,
  INCOMPLETE,
  INTERPRETING,
  FAILED
}
