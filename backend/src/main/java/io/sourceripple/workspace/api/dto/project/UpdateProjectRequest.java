package io.sourceripple.workspace.api.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(@NotBlank @Size(max = 100) String name, @Size(max = 1000) String description) {}
