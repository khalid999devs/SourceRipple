package io.sourceripple.workspace.api.dto.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(@NotBlank @Size(max = 100) String name) {}
