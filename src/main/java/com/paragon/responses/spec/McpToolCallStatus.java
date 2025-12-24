package com.paragon.responses.spec;

/**
 * The status of the tool call. One of {@code in_progress}, {@code completed}, {@code incomplete},
 * {@code calling}, or {@code failed}.
 */
public enum McpToolCallStatus {
  IN_PROGRESS,
  COMPLETED,
  INCOMPLETE,
  CALLING,
  FAILED
}
