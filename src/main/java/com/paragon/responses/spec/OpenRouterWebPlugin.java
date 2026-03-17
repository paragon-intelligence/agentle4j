package com.paragon.responses.spec;

import java.util.List;
import org.jspecify.annotations.Nullable;

public record OpenRouterWebPlugin(
    @Nullable Number maxResults,
    @Nullable String searchPrompt,
    @Nullable OpenRouterWebEngine engine,
    @Nullable List<String> includeDomains,
    @Nullable List<String> excludeDomains)
    implements OpenRouterPlugin {

  public OpenRouterWebPlugin() {
    this(null, null, null, null, null);
  }

  /** Convenience: web search with a result cap and no other filters. */
  public static OpenRouterWebPlugin withMaxResults(int maxResults) {
    return new OpenRouterWebPlugin(maxResults, null, null, null, null);
  }

  /** Convenience: web search restricted to specific domains. */
  public static OpenRouterWebPlugin includingDomains(List<String> domains) {
    return new OpenRouterWebPlugin(null, null, null, domains, null);
  }
}
