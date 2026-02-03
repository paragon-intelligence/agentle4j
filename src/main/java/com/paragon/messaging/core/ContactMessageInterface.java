package com.paragon.messaging.core;

/**
 * Sealed sub-interface for contact messages.
 * 
 * <p>This interface is part of the {@link OutboundMessage} sealed hierarchy
 * and is implemented by concrete contact message classes.</p>
 *
 * @since 2.1
 */
public non-sealed interface ContactMessageInterface extends OutboundMessage {
    // Marker interface - specific contact message types provide their own methods
    
    @Override
    default OutboundMessageType type() {
        return OutboundMessageType.CONTACT;
    }
}
