package com.paragon.responses.spec;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * A tool that searches for relevant content from uploaded files. Learn more about the file search
 * tool at https://platform.openai.com/docs/guides/tools-file-search
 *
 * @param vectorStoreIds The IDs of the vector stores to search.
 * @param filters A filter to apply.
 * @param maxNumResults The maximum number of results to return. This number should be between 1 and
 *     50 inclusive.
 * @param rankingOptions Ranking options for search. See {@link FileSearchRankingOptions}
 */
public record FileSearchTool(
    @NonNull List<String> vectorStoreIds,
    @Nullable FileSearchFilter filters,
    @Nullable Integer maxNumResults,
    @Nullable FileSearchRankingOptions rankingOptions)
    implements Tool {
  @Override
  public @NonNull String toToolChoice(ObjectMapper mapper) throws JacksonException {
    return mapper.writeValueAsString(Map.of("type", "file_search"));
  }
}
