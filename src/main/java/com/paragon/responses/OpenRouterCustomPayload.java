package com.paragon.responses;

import com.paragon.responses.spec.OpenRouterPlugin;
import com.paragon.responses.spec.OpenRouterProviderConfig;
import com.paragon.responses.spec.OpenRouterRouteStrategy;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record OpenRouterCustomPayload(
    @Nullable List<OpenRouterPlugin> plugins,
    @Nullable OpenRouterProviderConfig providerConfig,
    @Nullable OpenRouterRouteStrategy route,
    @Nullable String user,
    @Nullable String sessionId,
    @Nullable TraceMetadata trace) {

  /**
   * Checks if all fields in this payload are null.
   *
   * @return true if all fields are null, false otherwise
   */
  public boolean isEmpty() {
    return plugins == null
        && providerConfig == null
        && route == null
        && user == null
        && sessionId == null
        && trace == null;
  }

  /**
   * Returns this payload or null if all fields are null.
   *
   * @return this payload if not empty, null otherwise
   */
  public @Nullable OpenRouterCustomPayload orNullIfEmpty() {
    return isEmpty() ? null : this;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    @Nullable List<OpenRouterPlugin> plugins = null;
    @Nullable OpenRouterProviderConfig providerConfig = null;
    @Nullable OpenRouterRouteStrategy route = null;
    @Nullable String user = null;
    @Nullable String sessionId = null;
    @Nullable TraceMetadata trace = null;

    public Builder plugins(List<OpenRouterPlugin> plugins) {
      this.plugins = plugins;
      return this;
    }

    public Builder providerConfig(OpenRouterProviderConfig providerConfig) {
      this.providerConfig = providerConfig;
      return this;
    }

    public Builder route(OpenRouterRouteStrategy route) {
      this.route = route;
      return this;
    }

    public Builder user(String user) {
      this.user = user;
      return this;
    }

    public Builder sessionId(String sessionId) {
      this.sessionId = sessionId;
      return this;
    }

    public Builder trace(TraceMetadata trace) {
      this.trace = trace;
      return this;
    }

    public OpenRouterCustomPayload build() {
      return new OpenRouterCustomPayload(
          this.plugins, this.providerConfig, this.route, this.user, this.sessionId, this.trace);
    }
  }
}
