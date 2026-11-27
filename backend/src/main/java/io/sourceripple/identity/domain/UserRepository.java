package io.sourceripple.identity.domain;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :userId")
    Optional<User> findByIdForUpdate(UUID userId);

    boolean existsByRolesName(RoleName name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u join u.roles r where r.name = :role and u.enabled = true")
    List<User> findEnabledWithRoleForUpdate(RoleName role);

    @Query("""
            select u from User u
            where lower(u.email) like lower(concat('%', :query, '%'))
               or lower(u.displayName) like lower(concat('%', :query, '%'))
            """)
    Page<User> search(String query, Pageable pageable);
}
