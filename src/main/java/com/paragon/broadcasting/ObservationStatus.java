package com.paragon.broadcasting;

public enum ObservationStatus {
  RUNNING, // Still executing
  SUCCESS, // Completed successfully
  ERROR, // Completed with error
  CANCELLED // Cancelled/interrupted
}
