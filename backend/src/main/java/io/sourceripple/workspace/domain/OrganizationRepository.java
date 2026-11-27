package io.sourceripple.workspace.domain;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    boolean existsByIdAndMembersId(UUID organizationId, UUID userId);

    boolean existsByIdAndOwnerId(UUID organizationId, UUID userId);

    long countByOwnerId(UUID ownerId);

    Page<Organization> findByMembersId(UUID userId, Pageable pageable);

    List<Organization> findByMembersIdOrderByNameAsc(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Organization o where o.id = :organizationId")
    Optional<Organization> findByIdForUpdate(UUID organizationId);

    @Query(
            value = """
                    select u.id as userId, u.email as email, u.display_name as displayName, m.added_at as addedAt
                    from organization_members m join users u on u.id = m.user_id
                    where m.organization_id = :organizationId
                    order by m.added_at, u.email
                    """,
            countQuery = "select count(*) from organization_members where organization_id = :organizationId",
            nativeQuery = true)
    Page<MemberRow> findMembers(UUID organizationId, Pageable pageable);

    @Query(
            value = """
                    select u.id as userId, u.email as email, u.display_name as displayName, m.added_at as addedAt
                    from organization_members m join users u on u.id = m.user_id
                    where m.organization_id = :organizationId and m.user_id = :userId
                    """,
            nativeQuery = true)
    Optional<MemberRow> findMember(UUID organizationId, UUID userId);
}
