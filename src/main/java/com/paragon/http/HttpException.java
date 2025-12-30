package com.paragon.http;

import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when an HTTP request fails.
 */
public final class HttpException extends RuntimeException {

  private final int statusCode;
  @Nullable
  private final String responseBody;
  @Nullable
  private final String requestMethod;
  @Nullable
  private final String requestUrl;

  private HttpException(
          String message,
          int statusCode,
          @Nullable String responseBody,
          @Nullable String requestMethod,
          @Nullable String requestUrl,
          @Nullable Throwable cause
  ) {
    super(message, cause);
    this.statusCode = statusCode;
    this.responseBody = responseBody;
    this.requestMethod = requestMethod;
    this.requestUrl = requestUrl;
  }

  public static HttpException networkFailure(String message, Throwable cause) {
    return new HttpException(message, -1, null, null, null, cause);
  }

  public static HttpException fromResponse(int status, @Nullable String body, String method, String url) {
    var message = "HTTP %d: %s %s".formatted(status, method, url);
    return new HttpException(message, status, body, method, url, null);
  }

  public static HttpException deserializationFailed(int status, @Nullable String body, Throwable cause) {
    return new HttpException("Failed to deserialize response", status, body, null, null, cause);
  }

  public int statusCode() {
    return statusCode;
  }

  @Nullable
  public String responseBody() {
    return responseBody;
  }

  @Nullable
  public String requestMethod() {
    return requestMethod;
  }

  @Nullable
  public String requestUrl() {
    return requestUrl;
  }

  public boolean isClientError() {
    return statusCode >= 400 && statusCode < 500;
  }

  public boolean isServerError() {
    return statusCode >= 500 && statusCode < 600;
  }

  public boolean isRetryable() {
    return statusCode == -1
            || statusCode == 408
            || statusCode == 429
            || isServerError();
  }

  @Override
  public String toString() {
    var sb = new StringBuilder("HttpException{");
    sb.append("status=").append(statusCode);
    if (requestMethod != null) sb.append(", method=").append(requestMethod);
    if (requestUrl != null) sb.append(", url=").append(requestUrl);
    if (responseBody != null && !responseBody.isEmpty()) {
      var truncated = responseBody.length() > 100
              ? responseBody.substring(0, 100) + "..."
              : responseBody;
      sb.append(", body=").append(truncated);
    }
    sb.append('}');
    return sb.toString();
  }
}
