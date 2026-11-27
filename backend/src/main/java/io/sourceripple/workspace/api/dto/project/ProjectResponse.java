package io.sourceripple.workspace.api.dto.project;

import io.sourceripple.workspace.domain.Project;
import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID organizationId,
        String name,
        String slug,
        String description,
        Instant archivedAt,
        Instant createdAt) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getOrganization().getId(),
                project.getName(),
                project.getSlug(),
                project.getDescription(),
                project.getArchivedAt(),
                project.getCreatedAt());
    }
}
