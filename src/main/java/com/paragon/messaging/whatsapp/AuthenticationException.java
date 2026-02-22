package com.paragon.messaging.whatsapp;

import com.paragon.messaging.core.MessagingException;

/** Exceção lançada quando há problemas na autenticação. */
public class AuthenticationException extends MessagingException {

  public AuthenticationException(String message) {
    super(message);
  }

  public AuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }
}
