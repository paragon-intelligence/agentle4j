package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Reasoning summary content. This is a sealed interface. Allowed implementations are:
 *
 * <ul>
 *   <li>{@link ReasoningSummaryText} - A summary of the reasoning output from the model so far.
 * </ul>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = ReasoningSummaryText.class, name = "summary_text")})
public sealed interface ReasoningSummary permits ReasoningSummaryText {}
