package com.paragon.responses.spec;

/** A filter to apply. */
public sealed interface FileSearchFilter permits ComparisonFilter, CompoundFilter {}
