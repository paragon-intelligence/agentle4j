package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;

/**
 * Instruction for creating a new file via the apply_patch tool.
 *
 * @param diff Unified diff content to apply when creating the file.
 * @param path Path of the file to create relative to the workspace root.
 */
public record ApplyPatchCreateFileOperation(@NonNull String diff, @NonNull String path)
    implements ApplyPatchOperation {}
