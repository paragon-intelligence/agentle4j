package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Execute a shell command on the server.
 *
 * <ul>
 *   <li>{@link LocalShellExecAction} - A local shell exec action.
 * </ul>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = LocalShellExecAction.class, name = "exec")})
public sealed interface LocalShellAction permits LocalShellExecAction {}
