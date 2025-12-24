package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;

/**
 * Instruction for updating an existing file via the apply_patch tool.
 *
 * @param diff Unified diff content to apply to the existing file.
 * @param path Path of the file to update relative to the workspace root.
 */
public record ApplyPatchUpdateFileOperation(@NonNull String diff, @NonNull String path)
    implements ApplyPatchOperation {}
