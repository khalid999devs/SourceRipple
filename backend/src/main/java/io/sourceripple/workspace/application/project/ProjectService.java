package io.sourceripple.workspace.application.project;

import io.sourceripple.identity.application.access.AccessPolicy;
import io.sourceripple.shared.api.PageResponse;
import io.sourceripple.shared.api.error.status.NotFoundException;
import io.sourceripple.workspace.api.dto.project.CreateProjectRequest;
import io.sourceripple.workspace.api.dto.project.ProjectResponse;
import io.sourceripple.workspace.api.dto.project.ProjectSummaryResponse;
import io.sourceripple.workspace.api.dto.project.UpdateProjectRequest;
import io.sourceripple.workspace.application.OrganizationNotFoundException;
import io.sourceripple.workspace.domain.Organization;
import io.sourceripple.workspace.domain.OrganizationRepository;
import io.sourceripple.workspace.domain.Project;
import io.sourceripple.workspace.domain.ProjectRepository;
import java.text.Normalizer;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private static final int MAX_SLUG_BASE = 100;

    private final ProjectRepository projects;
    private final OrganizationRepository organizations;
    private final AccessPolicy access;
    private final List<ProjectArchiveListener> archiveListeners;
    private final List<ProjectSummaryContributor> summaryContributors;
    private final Clock clock;

    public ProjectService(
            ProjectRepository projects,
            OrganizationRepository organizations,
            AccessPolicy access,
            List<ProjectArchiveListener> archiveListeners,
            List<ProjectSummaryContributor> summaryContributors,
            Clock clock) {
        this.projects = projects;
        this.organizations = organizations;
        this.access = access;
        this.archiveListeners = archiveListeners;
        this.summaryContributors = summaryContributors;
        this.clock = clock;
    }

    @PreAuthorize("hasRole('MAINTAINER')")
    @Transactional
    public ProjectResponse create(UUID organizationId, CreateProjectRequest request) {
        access.requireMember(organizationId);

        // Locking the organization serializes creates, so two projects never race for the same slug.
        Organization organization =
                organizations.findByIdForUpdate(organizationId).orElseThrow(OrganizationNotFoundException::new);
        String name = request.name().trim();
        String slug = uniqueSlug(organizationId, name);
        Project project = new Project(organization, name, slug, trimToNull(request.description()), clock.instant());
        return ProjectResponse.from(projects.save(project));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> list(UUID organizationId, int page, int size) {
        access.requireMember(organizationId);
        return PageResponse.from(
                projects.findByOrganizationId(organizationId, PageResponse.request(page, size, Sort.by("name", "id"))),
                ProjectResponse::from);
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID projectId) {
        access.requireProjectMember(projectId);
        return ProjectResponse.from(find(projectId));
    }

    @PreAuthorize("hasRole('MAINTAINER')")
    @Transactional
    public ProjectResponse update(UUID projectId, UpdateProjectRequest request) {
        access.requireProjectMember(projectId);
        Project project = find(projectId);
        project.update(request.name().trim(), trimToNull(request.description()));
        return ProjectResponse.from(project);
    }

    @PreAuthorize("hasRole('MAINTAINER')")
    @Transactional
    public void archive(UUID projectId) {
        access.requireProjectMember(projectId);
        Project project = find(projectId);
        if (project.archive(clock.instant())) {
            archiveListeners.forEach(listener -> listener.onArchived(projectId));
        }
    }

    @Transactional(readOnly = true)
    public ProjectSummaryResponse summary(UUID projectId) {
        access.requireProjectMember(projectId);
        ProjectSummary summary = new ProjectSummary();
        summaryContributors.forEach(contributor -> contributor.contribute(projectId, summary));
        return ProjectSummaryResponse.from(projectId, summary);
    }

    String uniqueSlug(UUID organizationId, String name) {
        String base = slugify(name);
        Set<String> taken = new HashSet<>(projects.findSlugsLike(organizationId, base));
        if (!taken.contains(base)) {
            return base;
        }
        int suffix = 2;
        while (taken.contains(base + "-" + suffix)) {
            suffix++;
        }
        return base + "-" + suffix;
    }

    static String slugify(String name) {
        String ascii = Normalizer.normalize(name, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
        String slug = ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        if (slug.length() > MAX_SLUG_BASE) {
            slug = slug.substring(0, MAX_SLUG_BASE).replaceAll("-+$", "");
        }
        return slug.isEmpty() ? "project" : slug;
    }

    private Project find(UUID projectId) {
        return projects.findById(projectId).orElseThrow(() -> new NotFoundException("The project was not found."));
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
