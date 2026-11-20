package io.sourceripple.identity.application;

import io.sourceripple.identity.api.dto.MeResponse;
import io.sourceripple.identity.api.dto.RegisterRequest;
import io.sourceripple.identity.api.dto.UserResponse;
import io.sourceripple.identity.domain.Role;
import io.sourceripple.identity.domain.RoleName;
import io.sourceripple.identity.domain.RoleRepository;
import io.sourceripple.identity.domain.User;
import io.sourceripple.identity.domain.UserRepository;
import io.sourceripple.shared.api.ErrorCode;
import io.sourceripple.shared.api.error.BadRequestException;
import io.sourceripple.shared.api.error.ConflictException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AuthService(UserRepository users, RoleRepository roles, PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request.accountType() == RoleName.ADMIN) {
            throw new BadRequestException(ErrorCode.ROLE_NOT_ALLOWED, "The ADMIN role cannot be self-assigned.");
        }
        String email = EmailAddresses.normalize(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw emailTaken();
        }
        User user = new User(
                email, passwordEncoder.encode(request.password()), request.displayName().trim(), clock.instant());
        user.addRole(role(request.accountType()));
        try {
            return UserResponse.from(users.saveAndFlush(user));
        } catch (DataIntegrityViolationException raceWithAnotherRegistration) {
            throw emailTaken();
        }
    }

    @Transactional
    public MeResponse recordLogin(UUID userId) {
        User user = users.findById(userId).orElseThrow();
        user.recordLogin(clock.instant());
        return MeResponse.from(user);
    }

    @Transactional(readOnly = true)
    public MeResponse currentUser(UUID userId) {
        return MeResponse.from(users.findById(userId).orElseThrow());
    }

    private Role role(RoleName name) {
        return roles.findByName(name).orElseThrow();
    }

    private static ConflictException emailTaken() {
        return new ConflictException(ErrorCode.EMAIL_TAKEN, "An account with this email already exists.");
    }
}
