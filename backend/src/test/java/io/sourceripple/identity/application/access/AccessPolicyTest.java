package io.sourceripple.identity.application.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.sourceripple.shared.api.error.status.ForbiddenException;
import io.sourceripple.shared.api.error.status.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AccessPolicyTest {

    private static final UUID ORGANIZATION = UUID.randomUUID();
    private static final UUID PROJECT = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();

    @Mock
    private MembershipLookup memberships;

    @Mock
    private OrganizationResolver projectResolver;

    private AccessPolicy access;

    @BeforeEach
    void setUp() {
        when(projectResolver.resourceType()).thenReturn(ResourceType.PROJECT);
        access = new AccessPolicy(memberships, List.of(projectResolver));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void nonMemberGetsNotFound() {
        signInAs("ROLE_MAINTAINER");
        when(projectResolver.organizationOf(PROJECT)).thenReturn(Optional.of(ORGANIZATION));
        when(memberships.isMember(ORGANIZATION, USER)).thenReturn(false);

        assertThatThrownBy(() -> access.requireProjectMember(PROJECT)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> access.requireProjectWriter(PROJECT)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> access.requireMember(ORGANIZATION)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void unknownResourceGetsTheSameNotFound() {
        signInAs("ROLE_MAINTAINER");
        when(projectResolver.organizationOf(PROJECT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> access.requireProjectMember(PROJECT)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void memberWithoutTheRoleGetsForbidden() {
        signInAs("ROLE_REVIEWER");
        when(projectResolver.organizationOf(PROJECT)).thenReturn(Optional.of(ORGANIZATION));
        when(memberships.isMember(ORGANIZATION, USER)).thenReturn(true);

        assertThatCode(() -> access.requireProjectMember(PROJECT)).doesNotThrowAnyException();
        assertThatThrownBy(() -> access.requireProjectWriter(PROJECT)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void memberWithTheRolePasses() {
        signInAs("ROLE_MAINTAINER");
        when(projectResolver.organizationOf(PROJECT)).thenReturn(Optional.of(ORGANIZATION));
        when(memberships.isMember(ORGANIZATION, USER)).thenReturn(true);

        assertThatCode(() -> access.requireProjectWriter(PROJECT)).doesNotThrowAnyException();
        assertThat(access.canWriteProject(PROJECT)).isTrue();
    }

    @Test
    void onlyTheOwnerPassesOwnerChecks() {
        signInAs("ROLE_MAINTAINER");
        when(memberships.isMember(ORGANIZATION, USER)).thenReturn(true);
        when(memberships.isOwner(ORGANIZATION, USER)).thenReturn(false);

        assertThatThrownBy(() -> access.requireOwner(ORGANIZATION)).isInstanceOf(ForbiddenException.class);

        when(memberships.isOwner(ORGANIZATION, USER)).thenReturn(true);

        assertThatCode(() -> access.requireOwner(ORGANIZATION)).doesNotThrowAnyException();
    }

    private static void signInAs(String authority) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(authority));
        AuthenticatedUser user = new AuthenticatedUser(USER, "user@example.com", null, true, authorities);
        SecurityContextHolder.getContext()
                .setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, authorities));
    }
}
