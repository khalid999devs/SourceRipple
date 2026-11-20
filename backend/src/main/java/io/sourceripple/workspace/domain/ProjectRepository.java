package io.sourceripple.workspace.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @Query("select p.organization.id from Project p where p.id = :projectId")
    Optional<UUID> findOrganizationIdById(UUID projectId);
}
