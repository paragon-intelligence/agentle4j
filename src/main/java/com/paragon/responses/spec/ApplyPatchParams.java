package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Params for the ApplyPatch Tool
 *
 * @param path the path of the file.
 * @param operation The specific create, delete, or update instruction for the {@code apply_patch}
 *     tool call. This is a sealed interface. Allowed implementations are:
 *     <ul>
 *       <li>{@link ApplyPatchCreateFileOperation} - Instruction for creating a new file via the
 *           apply_patch tool.
 *       <li>{@link ApplyPatchDeleteFileOperation} - Instruction for deleting an existing file via
 *           the apply_patch tool .
 *       <li>{@link ApplyPatchUpdateFileOperation} - Instruction for updating an existing file via
 *           the apply_patch tool .
 *     </ul>
 */
public record ApplyPatchParams(
    @NonNull String path, @NonNull ApplyPatchOperation operation, @Nullable String diff) {}
