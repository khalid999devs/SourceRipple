package io.sourceripple.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.sourceripple.identity.api.dto.RegisterRequest;
import io.sourceripple.identity.domain.Role;
import io.sourceripple.identity.domain.RoleName;
import io.sourceripple.identity.domain.RoleRepository;
import io.sourceripple.identity.domain.User;
import io.sourceripple.identity.domain.UserRepository;
import io.sourceripple.shared.api.ErrorCode;
import io.sourceripple.shared.api.error.ApiException;
import io.sourceripple.shared.api.error.status.BadRequestException;
import io.sourceripple.shared.api.error.status.ConflictException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T09:00:00Z");

    @Mock
    private UserRepository users;

    @Mock
    private RoleRepository roles;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(users, roles, passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void registerStoresOnlyTheBcryptHashAndAssignsTheChosenRole() {
        Role reviewer = mock(Role.class);
        when(roles.findByName(RoleName.REVIEWER)).thenReturn(Optional.of(reviewer));
        when(passwordEncoder.encode("correct-horse-battery")).thenReturn("$2a$12$encoded");
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(request(" Ada@Example.com ", RoleName.REVIEWER));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("$2a$12$encoded");
        assertThat(saved.getValue().getEmail()).isEqualTo("ada@example.com");
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(saved.getValue().getRoles()).containsExactly(reviewer);
    }

    @Test
    void registerRejectsTheAdminAccountType() {
        assertThatThrownBy(() -> authService.register(request("ada@example.com", RoleName.ADMIN)))
                .isInstanceOf(BadRequestException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.ROLE_NOT_ALLOWED);

        verifyNoInteractions(users, passwordEncoder);
    }

    @Test
    void registerRejectsAnEmailThatDiffersOnlyInCase() {
        when(users.existsByEmailIgnoreCase("foo@x.io")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request("Foo@x.io", RoleName.MAINTAINER)))
                .isInstanceOf(ConflictException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.EMAIL_TAKEN);

        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void registerReportsEmailTakenWhenAConcurrentRegistrationWins() {
        when(roles.findByName(RoleName.MAINTAINER)).thenReturn(Optional.of(mock(Role.class)));
        when(passwordEncoder.encode(any())).thenReturn("$2a$12$encoded");
        when(users.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("uq_users_email"));

        assertThatThrownBy(() -> authService.register(request("foo@x.io", RoleName.MAINTAINER)))
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.EMAIL_TAKEN);
    }

    private static RegisterRequest request(String email, RoleName accountType) {
        return new RegisterRequest(email, "correct-horse-battery", "Ada", accountType);
    }
}
