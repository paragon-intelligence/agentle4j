package com.paragon.messaging.whatsapp;

/**
 * Exceção lançada quando há problemas na validação de webhook.
 */
public class WebhookValidationException extends MessagingException {

  public WebhookValidationException(String message) {
    super(message);
  }

  public WebhookValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
