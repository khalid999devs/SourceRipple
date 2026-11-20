package io.sourceripple.identity.application.account;

import io.sourceripple.identity.domain.Role;
import io.sourceripple.identity.domain.RoleName;
import io.sourceripple.identity.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserAccount(
        UUID id,
        String email,
        String displayName,
        boolean enabled,
        List<RoleName> roles,
        Instant createdAt,
        Instant lastLoginAt) {

    static UserAccount from(User user) {
        return new UserAccount(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.isEnabled(),
                user.getRoles().stream().map(Role::getName).sorted().toList(),
                user.getCreatedAt(),
                user.getLastLoginAt());
    }
}
