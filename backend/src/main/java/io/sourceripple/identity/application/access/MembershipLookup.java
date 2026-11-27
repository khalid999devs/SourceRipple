package io.sourceripple.identity.application.access;

import java.util.UUID;

public interface MembershipLookup {

    boolean isMember(UUID organizationId, UUID userId);

    boolean isOwner(UUID organizationId, UUID userId);
}
