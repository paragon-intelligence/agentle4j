package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;

/**
 * The image output from the code interpreter.
 *
 * @param url The URL of the image output from the code interpreter.
 */
public record CodeInterpreterOutputImage(@NonNull String url) implements CodeInterpreterOutput {}
