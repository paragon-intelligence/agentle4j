package com.paragon.parsing;

import com.paragon.responses.Responder;
import com.paragon.responses.spec.CreateResponsePayload;
import com.paragon.responses.spec.File;
import com.paragon.responses.spec.Message;
import com.paragon.responses.spec.ParsedResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.jspecify.annotations.NonNull;

/**
 * Agentic file parser that uses LLM to convert documents to markdown.
 *
 * <p><b>Virtual Thread Design:</b> Uses synchronous API optimized for Java 21+ virtual threads.
 */
@SuppressWarnings("ClassCanBeRecord")
public class AgenticFileParser {
  @NonNull
  private static final String INSTRUCTIONS =
      "Convert the document to markdown format, with each page as a separate markdown string."
          + " Preserve all structure (headings, lists, tables, formatting) and ensure clean,"
          + " readable output. For images, replace with a descriptive paragraph explaining the"
          + " image content in detail.";

  @NonNull private final Responder responder;

  public AgenticFileParser(@NonNull Responder responder) {
    this.responder = responder;
  }

  /**
   * Parses a file from the given path into markdown.
   *
   * @param path the path to the file
   * @return the markdown result
   * @throws IOException if the file cannot be read
   */
  public @NonNull MarkdownResult parse(@NonNull Path path) throws IOException {
    boolean fileDoesNotExist = Files.notExists(path);

    if (fileDoesNotExist) {
      throw new FileNotFoundException(path.toString());
    }

    byte[] fileBytes = Files.readAllBytes(path);
    String base64 = Base64.getEncoder().encodeToString(fileBytes);
    return parse(base64);
  }

  /**
   * Parses a file from a URI into markdown.
   *
   * @param uri the URI of the file
   * @return the markdown result
   * @throws IOException if the file cannot be read
   */
  public @NonNull MarkdownResult parse(@NonNull URI uri) throws IOException {
    if (!seemsLikeFile(uri)) {
      throw new IllegalArgumentException(String.format("URI %s is not a file", uri));
    }

    File file = File.fromUrl(uri.toString());
    return parse(file);
  }

  /**
   * Parses a base64-encoded file into markdown.
   *
   * @param base64 the base64-encoded file content
   * @return the markdown result
   * @throws IOException if processing fails
   */
  public @NonNull MarkdownResult parse(@NonNull String base64) throws IOException {
    File file = File.fromBase64(base64);
    return parse(file);
  }

  /**
   * Parses a File object into markdown.
   *
   * @param file the file to parse
   * @return the markdown result
   * @throws IOException if processing fails
   */
  public @NonNull MarkdownResult parse(@NonNull File file) throws IOException {
    ParsedResponse<MarkdownResult> response =
        responder.respond(
            CreateResponsePayload.builder()
                .addDeveloperMessage(Message.developer(INSTRUCTIONS))
                .addUserMessage(Message.builder().addContent(file).asUser())
                .withStructuredOutput(MarkdownResult.class)
                .build());
    return response.outputParsed();
  }

  private boolean seemsLikeFile(URI uri) {
    String path = uri.getPath();
    if (path == null || path.isEmpty()) return false;
    int lastSlash = path.lastIndexOf('/');
    String lastSegment = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    return !lastSegment.isEmpty() && !path.endsWith("/") && lastSegment.contains(".");
  }
}
