package io.sourceripple.workspace.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.sourceripple.TestcontainersConfiguration;
import io.sourceripple.identity.domain.User;
import io.sourceripple.identity.domain.UserRepository;
import jakarta.persistence.EntityManager;
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
class WorkspaceSchemaIT {

    @Autowired
    private UserRepository users;

    @Autowired
    private OrganizationRepository organizations;

    @Autowired
    private ProjectRepository projects;

    @Autowired
    private EntityManager entityManager;

    @Test
    void storesOrganizationMembersThroughTheJoinTable() {
        User owner = newUser();
        User reviewer = newUser();
        Organization organization = new Organization(owner, "Acme", Instant.now());
        organization.addMember(owner);
        organization.addMember(reviewer);
        organizations.saveAndFlush(organization);
        entityManager.clear();

        Organization found = organizations.findById(organization.getId()).orElseThrow();

        assertThat(found.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(found.getMembers())
                .extracting(User::getId)
                .containsExactlyInAnyOrder(owner.getId(), reviewer.getId());
    }

    @Test
    void allowsTheSameSlugInDifferentOrganizations() {
        Organization first = newOrganization();
        Organization second = newOrganization();

        projects.saveAndFlush(new Project(first, "Shop", "shop", null, Instant.now()));
        projects.saveAndFlush(new Project(second, "Shop", "shop", null, Instant.now()));

        assertThat(projects.count()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void rejectsADuplicateSlugWithinOneOrganization() {
        Organization organization = newOrganization();
        projects.saveAndFlush(new Project(organization, "Shop", "shop", null, Instant.now()));

        assertThatThrownBy(() -> projects.saveAndFlush(new Project(organization, "Shop", "shop", null, Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Organization newOrganization() {
        return organizations.saveAndFlush(new Organization(newUser(), "Org", Instant.now()));
    }

    private User newUser() {
        return users.saveAndFlush(
                new User("user-" + UUID.randomUUID() + "@example.com", "$2a$10$hash", "Member", Instant.now()));
    }
}
