package com.paragon.prompts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response from the Langfuse prompts list API (v2).
 *
 * <p>Represents the paginated response from {@code GET /api/public/v2/prompts}.
 *
 * @author Agentle Framework
 * @since 1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class LangfusePromptListResponse {

  @JsonProperty("data")
  private List<PromptMeta> data;

  @JsonProperty("meta")
  private PageMeta meta;

  /** Default constructor for Jackson deserialization. */
  public LangfusePromptListResponse() {}

  /**
   * Returns the list of prompt metadata entries.
   *
   * @return list of prompt metadata
   */
  public List<PromptMeta> getData() {
    return data != null ? data : List.of();
  }

  /**
   * Returns pagination metadata.
   *
   * @return page metadata
   */
  public PageMeta getMeta() {
    return meta;
  }

  /** Metadata for a single prompt in the list. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class PromptMeta {
    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @JsonProperty("versions")
    private List<Integer> versions;

    @JsonProperty("labels")
    private List<String> labels;

    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("lastUpdatedAt")
    private String lastUpdatedAt;

    /** Default constructor for Jackson deserialization. */
    public PromptMeta() {}

    /**
     * Returns the prompt name (used as promptId).
     *
     * @return prompt name
     */
    public String getName() {
      return name;
    }

    /**
     * Returns the prompt type ("text" or "chat").
     *
     * @return prompt type
     */
    public String getType() {
      return type;
    }

    /**
     * Returns available versions.
     *
     * @return list of version numbers
     */
    public List<Integer> getVersions() {
      return versions != null ? versions : List.of();
    }

    /**
     * Returns labels assigned to this prompt.
     *
     * @return list of labels
     */
    public List<String> getLabels() {
      return labels != null ? labels : List.of();
    }

    /**
     * Returns tags assigned to this prompt.
     *
     * @return list of tags
     */
    public List<String> getTags() {
      return tags != null ? tags : List.of();
    }

    /**
     * Returns the last updated timestamp.
     *
     * @return ISO 8601 timestamp string
     */
    public String getLastUpdatedAt() {
      return lastUpdatedAt;
    }
  }

  /** Pagination metadata. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class PageMeta {
    @JsonProperty("page")
    private int page;

    @JsonProperty("limit")
    private int limit;

    @JsonProperty("totalItems")
    private int totalItems;

    @JsonProperty("totalPages")
    private int totalPages;

    /** Default constructor for Jackson deserialization. */
    public PageMeta() {}

    public int getPage() {
      return page;
    }

    public int getLimit() {
      return limit;
    }

    public int getTotalItems() {
      return totalItems;
    }

    public int getTotalPages() {
      return totalPages;
    }
  }
}
