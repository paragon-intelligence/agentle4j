package com.paragon.responses.spec;

import org.jspecify.annotations.Nullable;

/**
 * A computer screenshot image used with the computer use tool.
 *
 * @param fileId
 * @param imageUrl
 */
public record ComputerUseOutput(@Nullable String fileId, @Nullable String imageUrl) {}
