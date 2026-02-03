package com.paragon.messaging.core;

import java.util.Optional;

/**
 * Sealed sub-interface for reaction messages (emoji reactions).
 * 
 * <p>This interface is part of the {@link OutboundMessage} sealed hierarchy
 * and is implemented by concrete reaction message classes.</p>
 *
 * @since 2.1
 */
public non-sealed interface ReactionMessageInterface extends OutboundMessage {
    
    /**
     * @return the message ID to react to
     */
    String messageId();
    
    /**
     * @return optional emoji (empty means removal)
     */
    Optional<String> emoji();
    
    /**
     * @return true if this is a reaction removal
     */
    default boolean isRemoval() {
        return emoji().isEmpty();
    }
    
    @Override
    default OutboundMessageType type() {
        return OutboundMessageType.REACTION;
    }
}
