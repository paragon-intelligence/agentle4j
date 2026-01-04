package com.paragon.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/** Immutable HTTP request with fluent builder. */
public final class HttpRequest {

  private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();

  private final String method;
  private final String url;
  private final Map<String, List<String>> headers;
  private final byte[] body;
  private final String contentType;
  private final Duration timeout;

  private HttpRequest(Builder builder) {
    this.method = builder.method;
    this.url = buildUrl(builder.url, builder.queryParams);
    this.headers = Map.copyOf(builder.headers);
    this.body = builder.body;
    this.contentType = builder.contentType;
    this.timeout = builder.timeout;
  }

  private static String buildUrl(String base, Map<String, List<String>> params) {
    if (params.isEmpty()) return base;

    var sb = new StringBuilder(base);
    sb.append(base.contains("?") ? "&" : "?");

    var first = true;
    for (var entry : params.entrySet()) {
      for (var value : entry.getValue()) {
        if (!first) sb.append("&");
        sb.append(encode(entry.getKey())).append("=").append(encode(value));
        first = false;
      }
    }
    return sb.toString();
  }

  private static String encode(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }

  public static Builder get(String url) {
    return new Builder("GET", url);
  }

  public static Builder post(String url) {
    return new Builder("POST", url);
  }

  public static Builder put(String url) {
    return new Builder("PUT", url);
  }

  public static Builder patch(String url) {
    return new Builder("PATCH", url);
  }

  public static Builder delete(String url) {
    return new Builder("DELETE", url);
  }

  public String method() {
    return method;
  }

  public String url() {
    return url;
  }

  public Map<String, List<String>> headers() {
    return headers;
  }

  public byte[] body() {
    return body;
  }

  public String contentType() {
    return contentType;
  }

  public Duration timeout() {
    return timeout;
  }

  @Override
  public String toString() {
    return "HttpRequest{%s %s, body=%s}"
        .formatted(method, url, body != null ? body.length + "B" : "null");
  }

  public static final class Builder {
    private final String method;
    private final String url;
    private final Map<String, List<String>> headers = new LinkedHashMap<>();
    private final Map<String, List<String>> queryParams = new LinkedHashMap<>();
    private byte[] body;
    private String contentType;
    private Duration timeout;

    private Builder(String method, String url) {
      this.method = Objects.requireNonNull(method);
      this.url = Objects.requireNonNull(url);
    }

    public Builder header(String name, String value) {
      headers.computeIfAbsent(name.toLowerCase(), k -> new ArrayList<>()).add(value);
      return this;
    }

    public Builder setHeader(String name, String value) {
      headers.put(name.toLowerCase(), new ArrayList<>(List.of(value)));
      return this;
    }

    public Builder queryParam(String name, String value) {
      queryParams.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
      return this;
    }

    public Builder body(byte[] body, String contentType) {
      this.body = body.clone();
      this.contentType = contentType;
      return this;
    }

    public Builder body(String body, String contentType) {
      this.body = body.getBytes(StandardCharsets.UTF_8);
      this.contentType = contentType;
      return this;
    }

    public Builder jsonBody(Object body, ObjectMapper mapper) {
      try {
        this.body = mapper.writeValueAsBytes(body);
        this.contentType = "application/json";
        return this;
      } catch (JsonProcessingException e) {
        throw new IllegalArgumentException("Failed to serialize JSON", e);
      }
    }

    public Builder jsonBody(Object body) {
      return jsonBody(body, DEFAULT_MAPPER);
    }

    public Builder jsonBody(String json) {
      this.body = json.getBytes(StandardCharsets.UTF_8);
      this.contentType = "application/json";
      return this;
    }

    public Builder timeout(Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    public Builder bearerAuth(String token) {
      return setHeader("Authorization", "Bearer " + token);
    }

    public Builder basicAuth(String user, String pass) {
      var encoded =
          Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
      return setHeader("Authorization", "Basic " + encoded);
    }

    public Builder accept(String mediaType) {
      return setHeader("Accept", mediaType);
    }

    public Builder acceptJson() {
      return accept("application/json");
    }

    public HttpRequest build() {
      return new HttpRequest(this);
    }
  }
}
