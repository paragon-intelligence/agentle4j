package com.paragon.broadcasting;

import org.jspecify.annotations.NonNull;

public record CostDetails(
    @NonNull Number inputCost, @NonNull Number outputCost, @NonNull Number totalCost) {}
