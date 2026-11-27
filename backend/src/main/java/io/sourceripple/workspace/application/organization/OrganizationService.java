package io.sourceripple.workspace.application.organization;

import io.sourceripple.identity.application.access.AccessPolicy;
import io.sourceripple.identity.application.account.UserAccountService;
import io.sourceripple.identity.domain.User;
import io.sourceripple.shared.api.ErrorCode;
import io.sourceripple.shared.api.PageResponse;
import io.sourceripple.shared.api.error.status.ConflictException;
import io.sourceripple.shared.infrastructure.config.SourceRippleProperties;
import io.sourceripple.workspace.api.dto.organization.CreateOrganizationRequest;
import io.sourceripple.workspace.api.dto.organization.OrganizationResponse;
import io.sourceripple.workspace.api.dto.organization.RenameOrganizationRequest;
import io.sourceripple.workspace.application.OrganizationNotFoundException;
import io.sourceripple.workspace.domain.Organization;
import io.sourceripple.workspace.domain.OrganizationRepository;
import io.sourceripple.workspace.domain.ProjectRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

    private final OrganizationRepository organizations;
    private final ProjectRepository projects;
    private final AccessPolicy access;
    private final UserAccountService accounts;
    private final Clock clock;
    private final int maxOwnedPerUser;

    public OrganizationService(
            OrganizationRepository organizations,
            ProjectRepository projects,
            AccessPolicy access,
            UserAccountService accounts,
            Clock clock,
            SourceRippleProperties properties) {
        this.organizations = organizations;
        this.projects = projects;
        this.access = access;
        this.accounts = accounts;
        this.clock = clock;
        this.maxOwnedPerUser = properties.workspace().maxOrganizationsPerUser();
    }

    @PreAuthorize("hasRole('MAINTAINER')")
    @Transactional
    public OrganizationResponse create(CreateOrganizationRequest request, UUID creatorId) {
        // Locking the creator's row serializes concurrent creates, so the quota count cannot be raced.
        User creator = accounts.lockForUpdate(creatorId);
        if (organizations.countByOwnerId(creatorId) >= maxOwnedPerUser) {
            throw new ConflictException(
                    ErrorCode.QUOTA_EXCEEDED, "You can own at most " + maxOwnedPerUser + " organizations.");
        }

        Organization organization = new Organization(creator, request.name().trim(), clock.instant());
        organization.addMember(creator);
        return OrganizationResponse.from(organizations.save(organization), creatorId);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrganizationResponse> listMine(UUID userId, int page, int size) {
        return PageResponse.from(
                organizations.findByMembersId(userId, PageResponse.request(page, size, Sort.by("name", "id"))),
                organization -> OrganizationResponse.from(organization, userId));
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> allOf(UUID userId) {
        return organizations.findByMembersIdOrderByNameAsc(userId).stream()
                .map(organization -> OrganizationResponse.from(organization, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse get(UUID organizationId, UUID viewerId) {
        access.requireMember(organizationId);
        return OrganizationResponse.from(find(organizationId), viewerId);
    }

    @Transactional
    public OrganizationResponse rename(UUID organizationId, RenameOrganizationRequest request, UUID viewerId) {
        access.requireOwner(organizationId);
        Organization organization = find(organizationId);
        organization.rename(request.name().trim());
        return OrganizationResponse.from(organization, viewerId);
    }

    @Transactional
    public void delete(UUID organizationId) {
        access.requireOwner(organizationId);
        Organization organization =
                organizations.findByIdForUpdate(organizationId).orElseThrow(OrganizationNotFoundException::new);
        if (projects.existsByOrganizationIdAndArchivedAtIsNull(organizationId)) {
            throw new ConflictException(
                    ErrorCode.ORG_NOT_EMPTY, "Archive every project before deleting the organization.");
        }
        projects.deleteByOrganizationId(organizationId);
        organizations.delete(organization);
    }

    private Organization find(UUID organizationId) {
        return organizations.findById(organizationId).orElseThrow(OrganizationNotFoundException::new);
    }
}
