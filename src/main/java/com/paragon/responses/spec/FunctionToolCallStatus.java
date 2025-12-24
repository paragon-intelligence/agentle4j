package com.paragon.responses.spec;

/**
 * The status of the item. One of {@code in_progress}, {@code completed}, or {@code incomplete}.
 * Populated when items are returned via API.
 */
public enum FunctionToolCallStatus {
  IN_PROGRESS,
  COMPLETED,
  INCOMPLETE
}
