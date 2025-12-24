package com.paragon.responses.spec;

import org.jspecify.annotations.NonNull;

/**
 * A grammar defined by the user.
 *
 * @param definition The grammar definition.
 * @param syntax The syntax of the grammar definition. One of {@code lark} or {@code regex}.
 */
public record CustomToolInputFormatGrammar(
    @NonNull String definition, @NonNull CustomToolInputFormatGrammarSyntax syntax)
    implements CustomToolInputFormat {}
