package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A description of the chain of thought used by a reasoning model while generating a response. Be
 * sure to include these items in your {@code input} to the Responses API for subsequent turns of a
 * conversation if you are manually managing context.
 *
 * @param id The unique identifier of the reasoning content.
 * @param summary Reasoning summary content. This is a sealed interface. See {@link
 *     ReasoningSummary}
 * @param content Reasoning text content. This is a sealed interface. See {@link ReasoningContent}
 * @param encryptedContent The encrypted content of the reasoning item - populated when a response
 *     is generated with reasoning.encrypted_content in the include parameter.
 * @param status The status of the item. One of {@code in_progress}, {@code completed}, or {@code
 *     incomplete}. Populated when items are returned via API.
 */
@com.fasterxml.jackson.annotation.JsonTypeName("reasoning")
@JsonIgnoreProperties(ignoreUnknown = true)
public record Reasoning(
    @NonNull String id,
    @NonNull List<ReasoningSummary> summary,
    @Nullable List<ReasoningContent> content,
    @Nullable String encryptedContent,
    @Nullable ReasoningStatus status)
    implements Item, ResponseOutput {

  @Override
  public @NonNull String toString() {
    return String.format(
        """
        <reasoning>
            <id>%s</id>
            <summary>%s</summary>
            <content>%s</content>
            <encrypted_content>%s</encrypted_content>
            <status>%s</status>
        </reasoning>
        """,
        id,
        summary,
        content != null ? content : "null",
        encryptedContent != null ? encryptedContent : "null",
        status != null ? status : "null");
  }
}
