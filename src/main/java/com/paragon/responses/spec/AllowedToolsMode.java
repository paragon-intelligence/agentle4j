package com.paragon.responses.spec;

/** Constrains the tools available to the model to a pre-defined set. */
public enum AllowedToolsMode {

  /** Allows the model to pick from among the allowed tools and generate a message. */
  AUTO,

  /** Requires the model to call one or more of the allowed tools. */
  REQUIRED
}
