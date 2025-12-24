package com.paragon.responses.spec;

import org.jspecify.annotations.Nullable;

public record OpenRouterFileParserPlugin(
    @Nullable Number maxFiles, @Nullable OpenRouterFileParserPdfConfig pdf)
    implements OpenRouterPlugin {

  public OpenRouterFileParserPlugin() {
    this(null, null);
  }
}
