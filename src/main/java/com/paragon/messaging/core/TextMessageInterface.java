package com.paragon.messaging.core;

import org.jspecify.annotations.Nullable;

/**
 * Sealed sub-interface for text messages.
 * 
 * <p>This interface is part of the {@link OutboundMessage} sealed hierarchy
 * and is implemented by concrete text message classes.</p>
 *
 * @since 2.1
 */
public non-sealed interface TextMessageInterface extends OutboundMessage {
    
    /**
     * @return the message body text
     */
    String body();
    
    /**
     * @return whether to generate URL previews
     */
    boolean previewUrl();
    
    @Override
    default OutboundMessageType type() {
        return OutboundMessageType.TEXT;
    }
}
