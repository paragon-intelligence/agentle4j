package com.paragon.messaging.core;

import java.util.Optional;

/**
 * Exceção base para erros de mensageria.
 */
public class MessagingException extends Exception {

  private final Optional<Integer> errorCode;

  public MessagingException(String message) {
    super(message);
    this.errorCode = Optional.empty();
  }

  public MessagingException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = Optional.empty();
  }

  public MessagingException(String message, int errorCode) {
    super(message);
    this.errorCode = Optional.of(errorCode);
  }

  public Optional<Integer> getErrorCode() {
    return errorCode;
  }
}