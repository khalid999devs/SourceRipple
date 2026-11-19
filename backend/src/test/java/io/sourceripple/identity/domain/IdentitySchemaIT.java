package io.sourceripple.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.sourceripple.TestcontainersConfiguration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class IdentitySchemaIT {

    @Autowired
    private UserRepository users;

    @Autowired
    private RoleRepository roles;

    @Test
    void seedsExactlyTheThreeRoles() {
        assertThat(roles.findAll())
                .extracting(Role::getName)
                .containsExactlyInAnyOrder(RoleName.ADMIN, RoleName.MAINTAINER, RoleName.REVIEWER);
    }

    @Test
    void storesUserWithRolesAndFindsItByEmailInAnyCase() {
        String email = uniqueEmail();
        User user = new User(email, "$2a$10$hash", "Ada", Instant.parse("2026-11-20T10:00:00Z"));
        user.addRole(roles.findByName(RoleName.MAINTAINER).orElseThrow());
        users.saveAndFlush(user);

        User found = users.findByEmailIgnoreCase(email.toUpperCase()).orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.isEnabled()).isTrue();
        assertThat(found.getRoles()).extracting(Role::getName).containsExactly(RoleName.MAINTAINER);
    }

    @Test
    void rejectsADuplicateEmail() {
        String email = uniqueEmail();
        users.saveAndFlush(new User(email, "$2a$10$hash", "First", Instant.now()));

        assertThatThrownBy(() -> users.saveAndFlush(new User(email, "$2a$10$hash", "Second", Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsAnEmailThatIsNotLowerCased() {
        assertThatThrownBy(() -> users.saveAndFlush(
                        new User("Mixed." + uniqueEmail(), "$2a$10$hash", "Mixed", Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
