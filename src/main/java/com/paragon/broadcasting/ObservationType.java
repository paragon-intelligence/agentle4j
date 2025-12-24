package com.paragon.broadcasting;

public enum ObservationType {
  SPAN, // Generic operation/span
  GENERATION, // LLM model call
  EVENT, // Lightweight event/log
  RETRIEVAL, // Vector DB / embedding retrieval
  TOOL_CALL // Tool/function invocation
}
