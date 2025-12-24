package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * The specific create, delete, or update instruction for the {@code apply_patch} tool call. This is
 * a sealed interface. Allowed implementations are:
 *
 * <ul>
 *   <li>{@link ApplyPatchCreateFileOperation} - Instruction for creating a new file via the
 *       apply_patch tool.
 *   <li>{@link ApplyPatchDeleteFileOperation} - Instruction for deleting an existing file via the
 *       apply_patch tool .
 *   <li>{@link ApplyPatchUpdateFileOperation} - Instruction for updating an existing file via the
 *       apply_patch tool .
 * </ul>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = ApplyPatchCreateFileOperation.class, name = "create_file"),
  @JsonSubTypes.Type(value = ApplyPatchDeleteFileOperation.class, name = "delete_file"),
  @JsonSubTypes.Type(value = ApplyPatchUpdateFileOperation.class, name = "update_file")
})
public sealed interface ApplyPatchOperation
    permits ApplyPatchCreateFileOperation,
        ApplyPatchDeleteFileOperation,
        ApplyPatchUpdateFileOperation {}
