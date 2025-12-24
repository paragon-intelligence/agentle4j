package com.paragon.responses.spec;

import org.jspecify.annotations.Nullable;

/**
 * Optional mask for inpainting. Contains {@code image_url} (string, optional) and {@code file_id}
 * (string, optional).
 *
 * @param fileId File ID for the mask image.
 * @param imageUrl Base64-encoded mask image.
 */
public record InputImageMask(@Nullable String fileId, @Nullable String imageUrl) {}
