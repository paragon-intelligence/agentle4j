package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * An object describing the specific action taken in this web search call. Includes details on how
 * the model used the web (search, open_page, find). This is a sealed abstract class with eighteen
 * permitted implementations:
 *
 * <ul>
 *   <li>{@link SearchAction} - Action type "search" - Performs a web search query.
 *   <li>{@link OpenPageAction} - Action type "open_page" - Opens a specific URL from search
 *       results.
 *   <li>{@link FindAction} - Action type "find": Searches for a pattern within a loaded page.
 * </ul>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = SearchAction.class, name = "search"),
  @JsonSubTypes.Type(value = OpenPageAction.class, name = "open_page"),
  @JsonSubTypes.Type(value = FindAction.class, name = "find")
})
public sealed interface WebAction permits SearchAction, OpenPageAction, FindAction {}
