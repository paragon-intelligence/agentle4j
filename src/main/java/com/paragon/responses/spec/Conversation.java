package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;

/**
 * The conversation that this response belongs to. Input items and output items from this response
 * are automatically added to this conversation.
 *
 * @param id The unique ID of the conversation.
 */
public record Conversation(@NonNull String id) {}
