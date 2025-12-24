package com.paragon.responses.spec;

/**
 * The status of the item. One of {@code in_progress}, {@code completed}, or {@code incomplete}.
 * Populated when input items are returned via API.
 */
public enum ComputerToolCallOutputStatus {
  IN_PROGRESS,
  COMPLETED,
  INCOMPLETE
}
