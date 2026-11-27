package io.sourceripple.workspace.api.dto.project;

import io.sourceripple.workspace.application.project.ProjectSummary;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ProjectSummaryResponse(
        UUID projectId,
        long watchCount,
        long failingSources,
        Instant lastCheckAt,
        Map<String, Long> impactsByStatus) {

    public static ProjectSummaryResponse from(UUID projectId, ProjectSummary summary) {
        return new ProjectSummaryResponse(
                projectId,
                summary.getWatchCount(),
                summary.getFailingSources(),
                summary.getLastCheckAt(),
                summary.getImpactsByStatus());
    }
}
