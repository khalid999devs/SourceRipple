package io.sourceripple.workspace.application;

import io.sourceripple.identity.application.access.MembershipLookup;
import io.sourceripple.workspace.domain.OrganizationRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WorkspaceMembershipLookup implements MembershipLookup {

    private final OrganizationRepository organizations;

    public WorkspaceMembershipLookup(OrganizationRepository organizations) {
        this.organizations = organizations;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMember(UUID organizationId, UUID userId) {
        return organizations.existsByIdAndMembersId(organizationId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOwner(UUID organizationId, UUID userId) {
        return organizations.existsByIdAndOwnerId(organizationId, userId);
    }
}
