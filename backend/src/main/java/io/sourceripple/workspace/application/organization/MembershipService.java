package io.sourceripple.workspace.application.organization;

import io.sourceripple.identity.application.access.AccessPolicy;
import io.sourceripple.identity.application.account.UserAccountService;
import io.sourceripple.identity.domain.User;
import io.sourceripple.shared.api.ErrorCode;
import io.sourceripple.shared.api.PageResponse;
import io.sourceripple.shared.api.error.status.ConflictException;
import io.sourceripple.shared.api.error.status.NotFoundException;
import io.sourceripple.workspace.api.dto.organization.AddMemberRequest;
import io.sourceripple.workspace.api.dto.organization.MemberResponse;
import io.sourceripple.workspace.application.OrganizationNotFoundException;
import io.sourceripple.workspace.domain.Organization;
import io.sourceripple.workspace.domain.OrganizationRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipService {

    private final OrganizationRepository organizations;
    private final UserAccountService accounts;
    private final AccessPolicy access;
    private final EntityManager entityManager;

    public MembershipService(
            OrganizationRepository organizations,
            UserAccountService accounts,
            AccessPolicy access,
            EntityManager entityManager) {
        this.organizations = organizations;
        this.accounts = accounts;
        this.access = access;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public PageResponse<MemberResponse> list(UUID organizationId, int page, int size) {
        access.requireMember(organizationId);
        UUID ownerId = find(organizationId).getOwner().getId();
        Pageable request = PageResponse.request(page, size, Sort.unsorted());
        return PageResponse.from(
                organizations.findMembers(organizationId, request), row -> MemberResponse.from(row, ownerId));
    }

    @Transactional
    public MemberResponse add(UUID organizationId, AddMemberRequest request) {
        access.requireOwner(organizationId);
        UUID userId = accounts.findIdByEmail(request.email())
                .orElseThrow(() -> new NotFoundException("No account exists for this email."));
        if (organizations.existsByIdAndMembersId(organizationId, userId)) {
            throw new ConflictException(ErrorCode.INVALID_TRANSITION, "This user is already a member.");
        }

        Organization organization = find(organizationId);
        organization.addMember(entityManager.getReference(User.class, userId));
        organizations.flush();
        return organizations
                .findMember(organizationId, userId)
                .map(row -> MemberResponse.from(row, organization.getOwner().getId()))
                .orElseThrow();
    }

    @Transactional
    public void remove(UUID organizationId, UUID userId) {
        access.requireOwner(organizationId);
        Organization organization = find(organizationId);
        if (organization.getOwner().getId().equals(userId)) {
            throw new ConflictException(ErrorCode.INVALID_TRANSITION, "The owner cannot be removed.");
        }
        if (!organization.removeMember(userId)) {
            throw new NotFoundException("This user is not a member.");
        }
    }

    private Organization find(UUID organizationId) {
        return organizations.findById(organizationId).orElseThrow(OrganizationNotFoundException::new);
    }
}
