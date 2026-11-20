package io.sourceripple.workspace.application.project;

import java.util.UUID;

public interface ProjectSummaryContributor {

    void contribute(UUID projectId, ProjectSummary summary);
}
