package io.sourceripple.admin.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(@NotNull Boolean enabled) {}
