package io.sourceripple.workspace.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    boolean existsByIdAndMembersId(UUID organizationId, UUID userId);

    boolean existsByIdAndOwnerId(UUID organizationId, UUID userId);
}
