package io.sourceripple.workspace.application.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.sourceripple.identity.application.access.AccessPolicy;
import io.sourceripple.identity.application.account.UserAccountService;
import io.sourceripple.identity.domain.User;
import io.sourceripple.shared.api.ErrorCode;
import io.sourceripple.shared.api.error.ApiException;
import io.sourceripple.shared.infrastructure.config.SourceRippleProperties;
import io.sourceripple.support.TestProperties;
import io.sourceripple.workspace.api.dto.organization.CreateOrganizationRequest;
import io.sourceripple.workspace.api.dto.organization.OrganizationResponse;
import io.sourceripple.workspace.domain.Organization;
import io.sourceripple.workspace.domain.OrganizationRepository;
import io.sourceripple.workspace.domain.ProjectRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    private static final UUID CREATOR_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-20T09:00:00Z");
    private static final int MAX_OWNED = 5;

    @Mock
    private OrganizationRepository organizations;

    @Mock
    private ProjectRepository projects;

    @Mock
    private AccessPolicy access;

    @Mock
    private UserAccountService accounts;

    private OrganizationService service;

    @BeforeEach
    void setUp() {
        SourceRippleProperties properties =
                TestProperties.withWorkspace(new SourceRippleProperties.Workspace(MAX_OWNED));
        service = new OrganizationService(
                organizations, projects, access, accounts, Clock.fixed(NOW, ZoneOffset.UTC), properties);
    }

    @Test
    void createMakesTheCreatorOwnerAndMember() {
        User creator = mock(User.class);
        when(creator.getId()).thenReturn(CREATOR_ID);
        when(accounts.lockForUpdate(CREATOR_ID)).thenReturn(creator);
        when(organizations.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationResponse response = service.create(new CreateOrganizationRequest(" Acme "), CREATOR_ID);

        ArgumentCaptor<Organization> saved = ArgumentCaptor.forClass(Organization.class);
        verify(organizations).save(saved.capture());
        assertThat(saved.getValue().getOwner()).isSameAs(creator);
        assertThat(saved.getValue().getMembers()).containsExactly(creator);
        assertThat(saved.getValue().getName()).isEqualTo("Acme");
        assertThat(response.owner()).isTrue();
        assertThat(response.createdAt()).isEqualTo(NOW);
    }

    @Test
    void createRejectsASixthOwnedOrganization() {
        when(organizations.countByOwnerId(CREATOR_ID)).thenReturn((long) MAX_OWNED);

        assertThatThrownBy(() -> service.create(new CreateOrganizationRequest("Sixth"), CREATOR_ID))
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.QUOTA_EXCEEDED);
        verify(organizations, never()).save(any());
    }

    @Test
    void deleteRefusesWhileAProjectIsStillActive() {
        UUID organizationId = UUID.randomUUID();
        when(organizations.findByIdForUpdate(organizationId)).thenReturn(Optional.of(mock(Organization.class)));
        when(projects.existsByOrganizationIdAndArchivedAtIsNull(organizationId)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(organizationId))
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.ORG_NOT_EMPTY);
        verify(organizations, never()).delete(any());
    }
}
