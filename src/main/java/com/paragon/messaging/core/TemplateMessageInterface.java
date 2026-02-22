package com.paragon.messaging.core;

/**
 * Sealed sub-interface for template messages.
 *
 * <p>This interface is part of the {@link OutboundMessage} sealed hierarchy and is implemented by
 * concrete template message classes.
 *
 * @since 2.1
 */
public non-sealed interface TemplateMessageInterface extends OutboundMessage {

  /**
   * @return the template name
   */
  String name();

  @Override
  default OutboundMessageType type() {
    return OutboundMessageType.TEMPLATE;
  }
}
