package com.paragon.embeddings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("ClassCanBeRecord")
public class OpenRouterEmbeddingProvider implements EmbeddingProvider {
  private static final String URL = "https://openrouter.ai/api/v1/embeddings";
  private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final OkHttpClient client;

  public OpenRouterEmbeddingProvider(
      @NonNull ObjectMapper objectMapper, @NonNull String apiKey, @NonNull OkHttpClient client) {
    this.objectMapper = objectMapper;
    this.apiKey = apiKey;
    this.client = client;
  }

  @Override
  public @NonNull CompletableFuture<List<Embedding>> createEmbeddings(
      @NonNull List<String> input, @NonNull String model) {
    Request request = buildRequest(input, model);

    CompletableFuture<List<Embedding>> future = new CompletableFuture<>();

    client
        .newCall(request)
        .enqueue(
            new Callback() {
              @Override
              public void onFailure(@NotNull Call call, @NotNull IOException e) {
                future.completeExceptionally(e);
              }

              @Override
              public void onResponse(@NotNull Call call, @NotNull Response response)
                  throws IOException {
                String responseBody = response.body().string();
                try {
                  List<Embedding> embeddings =
                      objectMapper.readValue(responseBody, new TypeReference<List<Embedding>>() {});
                  future.complete(embeddings);
                } catch (JsonMappingException e) {
                  future.completeExceptionally(e);
                }
              }
            });

    return future;
  }

  private @NonNull Request buildRequest(@NonNull List<String> input, @NonNull String model) {
    try {
      return new Request.Builder()
          .addHeader("Content-Type", "application/json")
          .addHeader("Authorization", String.format("Bearer %s", apiKey))
          .url(URL)
          .post(
              RequestBody.create(
                  objectMapper.writeValueAsString(Map.of("input", input, "model", model)), JSON))
          .build();
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
