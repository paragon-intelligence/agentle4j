package com.paragon.responses.spec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A tool that generates images using a model like {@code gpt-image-1}.
 *
 * @param background Background type for the generated image. One of {@code transparent}, {@code
 *     opaque}, or {@code auto}. Default: {@code auto}.
 * @param inputFidelity Control how much effort the model will exert to match the style and
 *     features, especially facial features, of input images. This parameter is only supported for
 *     gpt-image-1. Unsupported for {@code gpt-image-1-mini}. Supports {@code high} and {@code low}.
 *     Defaults to {@code low}.
 * @param inputImageMask Optional mask for inpainting. Contains {@code image_url} (string, optional)
 *     and {@code file_id} (string, optional).
 * @param model The image generation model to use. Default: {@code gpt-image-1}.
 * @param moderation Moderation level for the generated image. Default: {@code auto}. See {@link
 *     ImageGenerationModeration}
 * @param outputCompression Compression level for the output image. Default: 100.
 * @param outputFormat The output format of the generated image. One of {@code png}, {@code webp},
 *     or {@code jpeg}. Default: {@code png}.
 * @param partialImages Number of partial images to generate in streaming mode, from 0 (default
 *     value) to 3.
 * @param quality The quality of the generated image. One of {@code low}, {@code medium}, {@code
 *     high}, or {@code auto}. Default: {@code auto}.
 * @param size The size of the generated image. One of {@code 1024x1024}, {@code 1024x1536}, {@code
 *     1536x1024}, or {@code auto}. Default: {@code auto}.
 */
public record ImageGenerationTool(
    @Nullable Background background,
    @Nullable InputFidelity inputFidelity,
    @Nullable InputImageMask inputImageMask,
    @Nullable String model,
    @Nullable ImageGenerationModeration moderation,
    @Nullable Integer outputCompression,
    @Nullable OutputFormat outputFormat,
    @Nullable Integer partialImages,
    @Nullable Quality quality,
    @Nullable ImageSize size)
    implements Tool {
  @Override
  public @NonNull String toToolChoice(ObjectMapper mapper) throws JsonProcessingException {
    return mapper.writeValueAsString(Map.of("type", "image_generation"));
  }
}
