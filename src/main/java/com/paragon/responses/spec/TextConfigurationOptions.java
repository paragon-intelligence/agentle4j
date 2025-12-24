package com.paragon.responses.spec;

import org.jspecify.annotations.Nullable;

public record TextConfigurationOptions(
    @Nullable TextConfigurationOptionsFormat format, @Nullable ModelVerbosityConfig verbosity) {
  public boolean hasFormat() {
    return format != null;
  }

  public boolean hasJsonSchemaTextFormat() {
    return hasFormat() && format instanceof TextConfigurationOptionsJsonSchemaFormat;
  }
}
