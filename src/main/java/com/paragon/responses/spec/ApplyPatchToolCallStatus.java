package com.paragon.responses.spec;

/** The status of the apply patch tool call output. One of {@code completed} or {@code failed}. */
public enum ApplyPatchToolCallStatus {

  /** Apply patch succeeded. */
  COMPLETED,

  /** Apply patch failed. */
  FAILED
}
