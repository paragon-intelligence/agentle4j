package com.paragon.messaging.whatsapp.payload;

import jakarta.validation.constraints.NotNull;

public record Change(@NotNull String field, ChangeValue value) {}
