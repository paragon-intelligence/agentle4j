package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Contents of a reasoning. This is a sealed interface. Allowed implementations are:
 *
 * <ul>
 *   <li>{@link ReasoningTextContent} - Reasoning text content.
 * </ul>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = ReasoningTextContent.class, name = "reasoning_text")})
public sealed interface ReasoningContent permits ReasoningTextContent {}
