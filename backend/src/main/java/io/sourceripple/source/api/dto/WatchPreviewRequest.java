package io.sourceripple.source.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WatchPreviewRequest(@NotBlank @Size(max = 2048) String url) {}
