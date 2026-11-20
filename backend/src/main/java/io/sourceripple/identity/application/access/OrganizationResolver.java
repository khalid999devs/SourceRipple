package io.sourceripple.identity.application.access;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationResolver {

    ResourceType resourceType();

    Optional<UUID> organizationOf(UUID resourceId);
}
