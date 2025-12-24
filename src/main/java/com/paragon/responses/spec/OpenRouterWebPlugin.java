package com.paragon.responses.spec;

import org.jspecify.annotations.Nullable;

public record OpenRouterWebPlugin(
    @Nullable Number maxResults,
    @Nullable String searchPrompt,
    @Nullable OpenRouterWebEngine engine)
    implements OpenRouterPlugin {

  public OpenRouterWebPlugin() {
    this(null, null, null);
  }
}
