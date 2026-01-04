package com.paragon.http;

import java.util.function.Consumer;

/** Consumer for SSE streaming responses. */
public interface StreamConsumer<T> {

  static <T> StreamConsumer<T> of(
      Class<T> type, Consumer<T> onEvent, Consumer<Throwable> onError, Runnable onComplete) {
    return new StreamConsumer<>() {
      @Override
      public Class<T> eventType() {
        return type;
      }

      @Override
      public void onEvent(T event) {
        onEvent.accept(event);
      }

      @Override
      public void onError(Throwable error) {
        onError.accept(error);
      }

      @Override
      public void onComplete() {
        onComplete.run();
      }
    };
  }

  Class<T> eventType();

  void onEvent(T event);

  void onError(Throwable error);

  void onComplete();

  default void onRawLine(String line) {}

  default boolean shouldCancel() {
    return false;
  }
}
