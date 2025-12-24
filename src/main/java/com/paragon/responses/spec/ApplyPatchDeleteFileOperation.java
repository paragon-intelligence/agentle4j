package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;

/**
 * Instruction for deleting an existing file via the apply_patch tool.
 *
 * @param path Path of the file to delete relative to the workspace root.
 */
public record ApplyPatchDeleteFileOperation(@NonNull String path) implements ApplyPatchOperation {}
