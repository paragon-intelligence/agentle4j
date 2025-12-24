package com.paragon.responses.spec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Search the Internet for sources related to the prompt. Learn more about the <a>web search
 * tool</a>.
 *
 * @param filters Filters for the search.
 * @param searchContextSize High level guidance for the amount of context window space to use for
 *     the search. One of {@code low}, {@code medium}, or {@code high}. {@code medium} is the
 *     default.
 * @param userLocation The approximate location of the user. See {@link UserLocation}
 */
public record WebSearchTool(
    @Nullable WebSearchFilters filters,
    @Nullable WebSearchSearchContextSize searchContextSize,
    @Nullable UserLocation userLocation)
    implements Tool {
  @Override
  public @NonNull String toToolChoice(ObjectMapper mapper) throws JsonProcessingException {
    return mapper.writeValueAsString(Map.of("type", "web_search"));
  }
}
