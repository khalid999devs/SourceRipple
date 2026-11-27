package io.sourceripple.workspace.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @Query("select p.organization.id from Project p where p.id = :projectId")
    Optional<UUID> findOrganizationIdById(UUID projectId);

    boolean existsByOrganizationIdAndArchivedAtIsNull(UUID organizationId);

    Page<Project> findByOrganizationId(UUID organizationId, Pageable pageable);

    @Query("""
            select p.slug from Project p
            where p.organization.id = :organizationId and (p.slug = :base or p.slug like concat(:base, '-%'))
            """)
    List<String> findSlugsLike(UUID organizationId, String base);

    @Modifying
    @Query("delete from Project p where p.organization.id = :organizationId")
    void deleteByOrganizationId(UUID organizationId);
}
