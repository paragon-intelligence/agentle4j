package com.paragon.messaging.whatsapp.payload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record Entry(@NotNull String id, @Valid List<Change> changes) {}
