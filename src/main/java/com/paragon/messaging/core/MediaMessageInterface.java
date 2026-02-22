package com.paragon.messaging.core;


/**
 * Sealed sub-interface for media messages (images, videos, audio, documents, stickers).
 *
 * <p>This interface is part of the {@link OutboundMessage} sealed hierarchy and is implemented by
 * concrete media message classes and their subtypes.
 *
 * @since 2.1
 */
public non-sealed interface MediaMessageInterface extends OutboundMessage {
  // Marker interface - specific media types provide their own methods
}
