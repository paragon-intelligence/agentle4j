package com.paragon.http;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link HttpEventListener}.
 *
 * <p>Tests cover: noop factory, composite factory, and default method behavior.
 */
@DisplayName("HttpEventListener Tests")
class HttpEventListenerTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // NOOP FACTORY
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Noop Factory")
  class NoopFactoryTests {

    @Test
    @DisplayName("noop() returns non-null listener")
    void noopReturnsNonNullListener() {
      HttpEventListener listener = HttpEventListener.noop();
      assertNotNull(listener);
    }

    @Test
    @DisplayName("noop listener methods do nothing")
    void noopListenerMethodsDoNothing() {
      HttpEventListener listener = HttpEventListener.noop();
      HttpRequest request = HttpRequest.get("/test").build();

      // All methods should execute without throwing
      assertDoesNotThrow(() -> listener.onRequestStart(request));
      assertDoesNotThrow(() -> listener.onResponse(request, null));
      assertDoesNotThrow(() -> listener.onError(request, new RuntimeException("test")));
      assertDoesNotThrow(() -> listener.onRetry(request, 1, 1000L, null));
      assertDoesNotThrow(() -> listener.onStreamStart(request));
      assertDoesNotThrow(() -> listener.onStreamEvent(request, "data: test"));
      assertDoesNotThrow(() -> listener.onStreamEnd(request, 5, null));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // COMPOSITE FACTORY
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Composite Factory")
  class CompositeFactoryTests {

    @Test
    @DisplayName("composite with no listeners returns noop")
    void compositeWithNoListenersReturnsNoop() {
      HttpEventListener composite = HttpEventListener.composite();
      assertNotNull(composite);
    }

    @Test
    @DisplayName("composite with single listener returns that listener")
    void compositeWithSingleListenerReturnsThatListener() {
      TrackingListener listener = new TrackingListener();
      HttpEventListener composite = HttpEventListener.composite(listener);

      HttpRequest request = HttpRequest.get("/test").build();
      composite.onRequestStart(request);

      assertEquals(1, listener.requestStartCalls);
    }

    @Test
    @DisplayName("composite calls all listeners on onRequestStart")
    void compositeCallsAllListenersOnRequestStart() {
      TrackingListener listener1 = new TrackingListener();
      TrackingListener listener2 = new TrackingListener();
      TrackingListener listener3 = new TrackingListener();

      HttpEventListener composite = HttpEventListener.composite(listener1, listener2, listener3);

      HttpRequest request = HttpRequest.get("/test").build();
      composite.onRequestStart(request);

      assertEquals(1, listener1.requestStartCalls);
      assertEquals(1, listener2.requestStartCalls);
      assertEquals(1, listener3.requestStartCalls);
    }

    @Test
    @DisplayName("composite calls all listeners on onResponse")
    void compositeCallsAllListenersOnResponse() {
      TrackingListener listener1 = new TrackingListener();
      TrackingListener listener2 = new TrackingListener();

      HttpEventListener composite = HttpEventListener.composite(listener1, listener2);

      HttpRequest request = HttpRequest.get("/test").build();
      HttpResponse response = HttpResponse.of(200, "OK", java.util.Map.of(), "".getBytes(), 100L);
      composite.onResponse(request, response);

      assertEquals(1, listener1.responseCalls);
      assertEquals(1, listener2.responseCalls);
    }

    @Test
    @DisplayName("composite calls all listeners on onError")
    void compositeCallsAllListenersOnError() {
      TrackingListener listener1 = new TrackingListener();
      TrackingListener listener2 = new TrackingListener();

      HttpEventListener composite = HttpEventListener.composite(listener1, listener2);

      HttpRequest request = HttpRequest.get("/test").build();
      composite.onError(request, new RuntimeException("error"));

      assertEquals(1, listener1.errorCalls);
      assertEquals(1, listener2.errorCalls);
    }

    @Test
    @DisplayName("composite calls all listeners on onRetry")
    void compositeCallsAllListenersOnRetry() {
      TrackingListener listener1 = new TrackingListener();
      TrackingListener listener2 = new TrackingListener();

      HttpEventListener composite = HttpEventListener.composite(listener1, listener2);

      HttpRequest request = HttpRequest.get("/test").build();
      composite.onRetry(request, 1, 1000L, new RuntimeException("retry"));

      assertEquals(1, listener1.retryCalls);
      assertEquals(1, listener2.retryCalls);
    }

    @Test
    @DisplayName("composite calls all listeners on onStreamStart")
    void compositeCallsAllListenersOnStreamStart() {
      TrackingListener listener1 = new TrackingListener();
      TrackingListener listener2 = new TrackingListener();

      HttpEventListener composite = HttpEventListener.composite(listener1, listener2);

      HttpRequest request = HttpRequest.get("/test").build();
      composite.onStreamStart(request);

      assertEquals(1, listener1.streamStartCalls);
      assertEquals(1, listener2.streamStartCalls);
    }

    @Test
    @DisplayName("composite calls all listeners on onStreamEvent")
    void compositeCallsAllListenersOnStreamEvent() {
      TrackingListener listener1 = new TrackingListener();
      TrackingListener listener2 = new TrackingListener();

      HttpEventListener composite = HttpEventListener.composite(listener1, listener2);

      HttpRequest request = HttpRequest.get("/test").build();
      composite.onStreamEvent(request, "data: test");

      assertEquals(1, listener1.streamEventCalls);
      assertEquals(1, listener2.streamEventCalls);
    }

    @Test
    @DisplayName("composite calls all listeners on onStreamEnd")
    void compositeCallsAllListenersOnStreamEnd() {
      TrackingListener listener1 = new TrackingListener();
      TrackingListener listener2 = new TrackingListener();

      HttpEventListener composite = HttpEventListener.composite(listener1, listener2);

      HttpRequest request = HttpRequest.get("/test").build();
      composite.onStreamEnd(request, 10, null);

      assertEquals(1, listener1.streamEndCalls);
      assertEquals(1, listener2.streamEndCalls);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // DEFAULT METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("Default Methods")
  class DefaultMethodTests {

    @Test
    @DisplayName("custom listener can override onRequestStart")
    void customListenerCanOverrideOnRequestStart() {
      List<String> calls = new ArrayList<>();

      HttpEventListener listener =
          new HttpEventListener() {
            @Override
            public void onRequestStart(HttpRequest request) {
              calls.add("onRequestStart");
            }
          };

      listener.onRequestStart(HttpRequest.get("/test").build());
      assertEquals(1, calls.size());
      assertEquals("onRequestStart", calls.get(0));
    }

    @Test
    @DisplayName("custom listener can override onError")
    void customListenerCanOverrideOnError() {
      List<Throwable> errors = new ArrayList<>();

      HttpEventListener listener =
          new HttpEventListener() {
            @Override
            public void onError(HttpRequest request, Throwable error) {
              errors.add(error);
            }
          };

      RuntimeException error = new RuntimeException("test error");
      listener.onError(HttpRequest.get("/test").build(), error);

      assertEquals(1, errors.size());
      assertEquals(error, errors.get(0));
    }

    @Test
    @DisplayName("custom listener can override onRetry with null lastError")
    void customListenerCanHandleNullLastError() {
      List<Integer> attempts = new ArrayList<>();

      HttpEventListener listener =
          new HttpEventListener() {
            @Override
            public void onRetry(
                HttpRequest request, int attempt, long delayMs, Throwable lastError) {
              attempts.add(attempt);
            }
          };

      listener.onRetry(HttpRequest.get("/test").build(), 2, 1000L, null);

      assertEquals(1, attempts.size());
      assertEquals(2, attempts.get(0));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPER CLASS
  // ═══════════════════════════════════════════════════════════════════════════

  private static class TrackingListener implements HttpEventListener {
    int requestStartCalls = 0;
    int responseCalls = 0;
    int errorCalls = 0;
    int retryCalls = 0;
    int streamStartCalls = 0;
    int streamEventCalls = 0;
    int streamEndCalls = 0;

    @Override
    public void onRequestStart(HttpRequest request) {
      requestStartCalls++;
    }

    @Override
    public void onResponse(HttpRequest request, HttpResponse response) {
      responseCalls++;
    }

    @Override
    public void onError(HttpRequest request, Throwable error) {
      errorCalls++;
    }

    @Override
    public void onRetry(HttpRequest request, int attempt, long delayMs, Throwable lastError) {
      retryCalls++;
    }

    @Override
    public void onStreamStart(HttpRequest request) {
      streamStartCalls++;
    }

    @Override
    public void onStreamEvent(HttpRequest request, String data) {
      streamEventCalls++;
    }

    @Override
    public void onStreamEnd(HttpRequest request, int eventCount, Throwable error) {
      streamEndCalls++;
    }
  }
}
