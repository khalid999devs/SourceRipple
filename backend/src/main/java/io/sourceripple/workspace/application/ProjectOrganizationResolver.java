package io.sourceripple.workspace.application;

import io.sourceripple.identity.application.access.OrganizationResolver;
import io.sourceripple.identity.application.access.ResourceType;
import io.sourceripple.workspace.domain.ProjectRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProjectOrganizationResolver implements OrganizationResolver {

    private final ProjectRepository projects;

    public ProjectOrganizationResolver(ProjectRepository projects) {
        this.projects = projects;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.PROJECT;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> organizationOf(UUID projectId) {
        return projects.findOrganizationIdById(projectId);
    }
}
