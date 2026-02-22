package com.paragon.messaging.core;

/**
 * Sealed sub-interface for interactive messages (buttons, lists, CTA URLs).
 *
 * <p>This interface is part of the {@link OutboundMessage} sealed hierarchy and is implemented by
 * concrete interactive message classes.
 *
 * @since 2.1
 */
public non-sealed interface InteractiveMessageInterface extends OutboundMessage {

  /**
   * @return the message body text
   */
  String body();
}
