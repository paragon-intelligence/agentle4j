package com.paragon.parsing;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.spec.File;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.ParsedResponse;
import org.jspecify.annotations.NonNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class AgenticFileParser {
  @NonNull
  private final Responder responder;

  public AgenticFileParser(@NonNull Responder responder) {
    this.responder = responder;
  }

  public @NonNull CompletableFuture<MarkdownResult> parse(@NonNull Path path) throws IOException {
    boolean fileDoesNotExist = Files.notExists(path);

    if (fileDoesNotExist) {
      throw new FileNotFoundException(path.toString());
    }

    byte[] fileBytes = Files.readAllBytes(path);
    String base64 = Base64.getEncoder().encodeToString(fileBytes);
    return parse(base64);
  }

  public @NonNull CompletableFuture<MarkdownResult> parse(@NonNull URI uri) throws IOException {
    if (!seemsLikeFile(uri)) {
      throw new IllegalArgumentException(String.format("URI %s is not a file", uri));
    }

    File file = File.fromUrl(uri.toString());
    return parse(file);
  }

  public @NonNull CompletableFuture<MarkdownResult> parse(@NonNull String base64)
          throws IOException {
    File file = File.fromBase64(base64);
    return parse(file);
  }

  public @NonNull CompletableFuture<MarkdownResult> parse(@NonNull File file) throws IOException {
    return responder.respond(
            CreateResponsePayload.builder()
                    .addUserMessage(Message.builder().addContent(file).asUser())
                    .withStructuredOutput(MarkdownResult.class)
                    .build()).thenApply(ParsedResponse::outputParsed);

  }

  private boolean seemsLikeFile(URI uri) {
    String path = uri.getPath();
    if (path == null || path.isEmpty()) return false;
    int lastSlash = path.lastIndexOf('/');
    String lastSegment = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    return !lastSegment.isEmpty() && !path.endsWith("/") && lastSegment.contains(".");
  }
}
