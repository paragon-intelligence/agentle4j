package com.paragon.messaging.whatsapp.config;

/**
 * Configuration for WhatsApp Cloud API provider.
 */
public record WhatsAppConfig(
        String accessToken,
        String phoneNumberId,
        String apiVersion,
        String apiBaseUrl,
        int maxConcurrentRequests,
        long requestTimeout
) {

  public WhatsAppConfig {
    if (accessToken == null || accessToken.isBlank()) {
      throw new IllegalArgumentException("accessToken is required");
    }
    if (phoneNumberId == null || phoneNumberId.isBlank()) {
      throw new IllegalArgumentException("phoneNumberId is required");
    }
    if (apiVersion == null) {
      apiVersion = "v22.0";
    }
    if (apiBaseUrl == null) {
      apiBaseUrl = "https://graph.facebook.com";
    }
    if (maxConcurrentRequests <= 0) {
      maxConcurrentRequests = 80;
    }
    if (requestTimeout <= 0) {
      requestTimeout = 30000;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String accessToken;
    private String phoneNumberId;
    private String apiVersion = "v22.0";
    private String apiBaseUrl = "https://graph.facebook.com";
    private int maxConcurrentRequests = 80;
    private long requestTimeout = 30000;

    public Builder accessToken(String v) { this.accessToken = v; return this; }
    public Builder phoneNumberId(String v) { this.phoneNumberId = v; return this; }
    public Builder apiVersion(String v) { this.apiVersion = v; return this; }
    public Builder apiBaseUrl(String v) { this.apiBaseUrl = v; return this; }
    public Builder maxConcurrentRequests(int v) { this.maxConcurrentRequests = v; return this; }
    public Builder requestTimeout(long v) { this.requestTimeout = v; return this; }

    public WhatsAppConfig build() {
      return new WhatsAppConfig(accessToken, phoneNumberId, apiVersion, apiBaseUrl, maxConcurrentRequests, requestTimeout);
    }
  }
}
