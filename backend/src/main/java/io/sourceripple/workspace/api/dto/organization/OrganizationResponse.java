package io.sourceripple.workspace.api.dto.organization;

import io.sourceripple.workspace.domain.Organization;
import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(UUID id, String name, UUID ownerUserId, boolean owner, Instant createdAt) {

    public static OrganizationResponse from(Organization organization, UUID viewerId) {
        UUID ownerId = organization.getOwner().getId();
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                ownerId,
                ownerId.equals(viewerId),
                organization.getCreatedAt());
    }
}
