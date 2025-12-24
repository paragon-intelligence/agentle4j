package com.paragon.responses.spec;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Filters for the search.
 *
 * @param allowedDomains Allowed domains for the search. If not provided, all domains are allowed.
 *     Subdomains of the provided domains are allowed as well.
 */
public record WebSearchFilters(@Nullable List<String> allowedDomains) {}
