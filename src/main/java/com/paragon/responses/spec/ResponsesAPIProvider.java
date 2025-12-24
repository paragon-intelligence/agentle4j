package com.paragon.responses.spec;

import okhttp3.HttpUrl;

public enum ResponsesAPIProvider {
  OPENAI,
  OPEN_ROUTER;

  public HttpUrl getBaseUrl() {
    return switch (this) {
      case OPENAI -> HttpUrl.get("https://api.openai.com/v1/responses");
      case OPEN_ROUTER -> HttpUrl.get("https://openrouter.ai/api/v1/responses");
    };
  }

  public String getEnvKey() {
    return switch (this) {
      case OPENAI -> "OPENAI_API_KEY";
      case OPEN_ROUTER -> "OPENROUTER_API_KEY";
    };
  }
}
