package io.sourceripple.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.sourceripple.identity.domain.Role;
import io.sourceripple.identity.domain.RoleName;
import io.sourceripple.identity.domain.RoleRepository;
import io.sourceripple.identity.domain.User;
import io.sourceripple.identity.domain.UserRepository;
import io.sourceripple.shared.infrastructure.config.SourceRippleProperties;
import io.sourceripple.support.TestProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminInitializerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-20T09:00:00Z"), ZoneOffset.UTC);

    @Mock
    private UserRepository users;

    @Mock
    private RoleRepository roles;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void createsAnAdminFromConfigurationWhenNoneExists() {
        Role admin = mock(Role.class);
        when(roles.findByName(RoleName.ADMIN)).thenReturn(Optional.of(admin));
        when(passwordEncoder.encode("admin-password-1")).thenReturn("$2a$12$encoded");

        initializer("Admin@Example.com", "admin-password-1").run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("$2a$12$encoded");
        assertThat(saved.getValue().getRoles()).containsExactly(admin);
    }

    @Test
    void createsNothingWhenAnAdminAlreadyExists() {
        when(users.existsByRolesName(RoleName.ADMIN)).thenReturn(true);

        initializer("admin@example.com", "admin-password-1").run(null);

        verify(users, never()).save(any());
    }

    @Test
    void createsNothingWhenTheCredentialsAreMissing() {
        initializer("admin@example.com", "").run(null);

        verify(users, never()).save(any());
    }

    @Test
    void neverPromotesAnExistingAccountWithTheAdminEmail() {
        when(users.existsByEmailIgnoreCase("admin@example.com")).thenReturn(true);

        initializer("admin@example.com", "admin-password-1").run(null);

        verify(users, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"short-pw", "ééééééééééééééééééééééééééééééééééééé"})
    void refusesAPasswordBcryptCannotUse(String password) {
        assertThatThrownBy(() -> initializer("admin@example.com", password).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SOURCERIPPLE_ADMIN_PASSWORD");

        verify(users, never()).save(any());
    }

    private BootstrapAdminInitializer initializer(String email, String password) {
        SourceRippleProperties properties = TestProperties.withAdmin(new SourceRippleProperties.Admin(email, password));
        return new BootstrapAdminInitializer(users, roles, passwordEncoder, properties, CLOCK);
    }
}
