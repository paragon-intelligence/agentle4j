package com.paragon.messaging.whatsapp;

/**
 * Exceção lançada quando há problemas na autenticação.
 */
public class AuthenticationException extends MessagingException {

  public AuthenticationException(String message) {
    super(message);
  }

  public AuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }

  public AuthenticationException(String message, int errorCode, String providerErrorMessage) {
    super(message, errorCode, providerErrorMessage);
  }
}
