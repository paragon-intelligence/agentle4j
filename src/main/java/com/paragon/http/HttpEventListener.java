package com.paragon.http;

import org.jspecify.annotations.Nullable;

/** Listener for HTTP client events (logging, metrics). */
public interface HttpEventListener {

  static HttpEventListener noop() {
    return new HttpEventListener() {};
  }

  static HttpEventListener composite(HttpEventListener... listeners) {
    if (listeners.length == 0) return noop();
    if (listeners.length == 1) return listeners[0];

    return new HttpEventListener() {
      @Override
      public void onRequestStart(HttpRequest r) {
        for (var l : listeners) l.onRequestStart(r);
      }

      @Override
      public void onResponse(HttpRequest r, HttpResponse res) {
        for (var l : listeners) l.onResponse(r, res);
      }

      @Override
      public void onError(HttpRequest r, Throwable e) {
        for (var l : listeners) l.onError(r, e);
      }

      @Override
      public void onRetry(HttpRequest r, int a, long d, Throwable e) {
        for (var l : listeners) l.onRetry(r, a, d, e);
      }

      @Override
      public void onStreamStart(HttpRequest r) {
        for (var l : listeners) l.onStreamStart(r);
      }

      @Override
      public void onStreamEvent(HttpRequest r, String d) {
        for (var l : listeners) l.onStreamEvent(r, d);
      }

      @Override
      public void onStreamEnd(HttpRequest r, int c, Throwable e) {
        for (var l : listeners) l.onStreamEnd(r, c, e);
      }
    };
  }

  default void onRequestStart(HttpRequest request) {}

  default void onResponse(HttpRequest request, HttpResponse response) {}

  default void onError(HttpRequest request, Throwable error) {}

  default void onRetry(
      HttpRequest request, int attempt, long delayMs, @Nullable Throwable lastError) {}

  default void onStreamStart(HttpRequest request) {}

  default void onStreamEvent(HttpRequest request, String data) {}

  default void onStreamEnd(HttpRequest request, int eventCount, @Nullable Throwable error) {}
}
