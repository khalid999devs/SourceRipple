package io.sourceripple.workspace.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.sourceripple.identity.application.access.AccessPolicy;
import io.sourceripple.workspace.domain.Organization;
import io.sourceripple.workspace.domain.OrganizationRepository;
import io.sourceripple.workspace.domain.Project;
import io.sourceripple.workspace.domain.ProjectRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-20T09:00:00Z");

    @Mock
    private ProjectRepository projects;

    @Mock
    private OrganizationRepository organizations;

    @Mock
    private AccessPolicy access;

    @Mock
    private ProjectArchiveListener archiveListener;

    private ProjectService service;

    @BeforeEach
    void setUp() {
        service = new ProjectService(
                projects,
                organizations,
                access,
                List.of(archiveListener),
                List.of(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void firstProjectGetsThePlainSlugAndTheNextOneASuffix() {
        when(projects.findSlugsLike(ORGANIZATION_ID, "shop")).thenReturn(List.of());
        assertThat(service.uniqueSlug(ORGANIZATION_ID, "Shop")).isEqualTo("shop");

        when(projects.findSlugsLike(ORGANIZATION_ID, "shop")).thenReturn(List.of("shop"));
        assertThat(service.uniqueSlug(ORGANIZATION_ID, "Shop")).isEqualTo("shop-2");

        when(projects.findSlugsLike(ORGANIZATION_ID, "shop")).thenReturn(List.of("shop", "shop-2", "shop-10"));
        assertThat(service.uniqueSlug(ORGANIZATION_ID, "Shop")).isEqualTo("shop-3");
    }

    @Test
    void slugsAreAsciiKebabCase() {
        assertThat(ProjectService.slugify("  Café Checkout / Mobile!  ")).isEqualTo("cafe-checkout-mobile");
        assertThat(ProjectService.slugify("***")).isEqualTo("project");
    }

    @Test
    void archiveSetsArchivedAtAndNotifiesListenersOnce() {
        UUID projectId = UUID.randomUUID();
        Project project = new Project(mock(Organization.class), "Shop", "shop", null, NOW);
        when(projects.findById(projectId)).thenReturn(Optional.of(project));

        service.archive(projectId);
        service.archive(projectId);

        assertThat(project.getArchivedAt()).isEqualTo(NOW);
        verify(archiveListener).onArchived(projectId);
    }

    @Test
    void summaryHasZerosUntilContributorsReport() {
        UUID projectId = UUID.randomUUID();

        var summary = service.summary(projectId);

        assertThat(summary.watchCount()).isZero();
        assertThat(summary.impactsByStatus()).isEmpty();
        verifyNoInteractions(projects);
    }
}
