package com.paragon.agents;

/** Describes whether the final output was produced locally or delegated. */
public enum OutputOrigin {
  /** The interactable that was invoked directly produced the final output itself. */
  LOCAL,
  /** The final output came from a delegated downstream interactable. */
  DELEGATED
}
