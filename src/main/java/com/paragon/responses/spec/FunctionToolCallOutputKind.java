package com.paragon.responses.spec;

/**
 * Text, image, or file output of the function tool call. This is a sealed interface. Allowed values
 * are:
 *
 * <ul>
 *   <li>{@link Text} - A text input to the model.
 *   <li>{@link Image} - An image input to the model. Learn about image inputs.
 *   <li>{@link File} - A file input to the model.
 * </ul>
 */
public sealed interface FunctionToolCallOutputKind permits Text, Image, File {}
