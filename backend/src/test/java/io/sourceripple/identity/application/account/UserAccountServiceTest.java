package io.sourceripple.identity.application.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.sourceripple.identity.domain.Role;
import io.sourceripple.identity.domain.RoleName;
import io.sourceripple.identity.domain.RoleRepository;
import io.sourceripple.identity.domain.User;
import io.sourceripple.identity.domain.UserRepository;
import io.sourceripple.shared.api.ErrorCode;
import io.sourceripple.shared.api.error.status.BadRequestException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID OTHER_ADMIN_ID = UUID.randomUUID();
    private static final String EMAIL = "admin@example.com";

    @Mock
    private UserRepository users;

    @Mock
    private RoleRepository roles;

    @Mock
    private FindByIndexNameSessionRepository<Session> sessions;

    @Mock
    private CurrentSession currentSession;

    private UserAccountService service;
    private User admin;

    @BeforeEach
    void setUp() {
        service = new UserAccountService(users, roles, sessions, currentSession);
        admin = new User(EMAIL, "$2a$12$hash", "Admin", Instant.now());
        admin.addRole(role(RoleName.ADMIN));
    }

    @Test
    void theLastEnabledAdminCannotBeDisabled() {
        when(users.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(users.findEnabledWithRoleForUpdate(RoleName.ADMIN)).thenReturn(List.of(admin));

        assertThatThrownBy(() -> service.setEnabled(ADMIN_ID, false))
                .isInstanceOfSatisfying(
                        BadRequestException.class, ex -> assertThat(ex.code()).isEqualTo(ErrorCode.ROLE_NOT_ALLOWED));

        assertThat(admin.isEnabled()).isTrue();
    }

    @Test
    void anAdminCanBeDisabledWhileAnotherEnabledAdminRemains() {
        when(users.findById(OTHER_ADMIN_ID)).thenReturn(Optional.of(admin));
        when(users.findEnabledWithRoleForUpdate(RoleName.ADMIN)).thenReturn(List.of(admin, mock(User.class)));
        when(sessions.findByPrincipalName(EMAIL)).thenReturn(Map.of());

        UserAccount disabled = service.setEnabled(OTHER_ADMIN_ID, false);

        assertThat(disabled.enabled()).isFalse();
    }

    @Test
    void changingYourOwnRolesKeepsTheCurrentSessionAndEndsTheOthers() {
        Role adminRole = role(RoleName.ADMIN);
        Role maintainerRole = role(RoleName.MAINTAINER);
        when(users.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(roles.findByName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
        when(roles.findByName(RoleName.MAINTAINER)).thenReturn(Optional.of(maintainerRole));
        when(sessions.findByPrincipalName(EMAIL))
                .thenReturn(Map.of("current", mock(Session.class), "stale", mock(Session.class)));
        when(currentSession.id()).thenReturn(Optional.of("current"));

        service.replaceRoles(ADMIN_ID, Set.of(RoleName.ADMIN, RoleName.MAINTAINER), ADMIN_ID);

        verify(sessions).deleteById("stale");
        verify(sessions, never()).deleteById("current");
        verify(currentSession).refreshRoles(Set.of(RoleName.ADMIN, RoleName.MAINTAINER));
    }

    @Test
    void changingSomeoneElsesRolesEndsAllTheirSessions() {
        Role adminRole = role(RoleName.ADMIN);
        when(users.findById(OTHER_ADMIN_ID)).thenReturn(Optional.of(admin));
        when(roles.findByName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
        when(sessions.findByPrincipalName(EMAIL)).thenReturn(Map.of("theirs", mock(Session.class)));

        service.replaceRoles(OTHER_ADMIN_ID, Set.of(RoleName.ADMIN), ADMIN_ID);

        verify(sessions).deleteById("theirs");
        verify(currentSession, never()).refreshRoles(any());
    }

    private static Role role(RoleName name) {
        Role role = mock(Role.class);
        lenient().when(role.getName()).thenReturn(name);
        return role;
    }
}
