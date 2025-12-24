package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;

/**
 * The logs output from the code interpreter.
 *
 * @param logs The logs output from the code interpreter.
 */
public record CodeInterpreterOutputLogs(@NonNull String logs) implements CodeInterpreterOutput {}
