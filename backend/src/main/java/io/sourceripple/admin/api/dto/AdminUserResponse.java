package io.sourceripple.admin.api.dto;

import io.sourceripple.identity.application.account.UserAccount;
import io.sourceripple.identity.domain.RoleName;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        String displayName,
        boolean enabled,
        List<RoleName> roles,
        Instant createdAt,
        Instant lastLoginAt) {

    public static AdminUserResponse from(UserAccount account) {
        return new AdminUserResponse(
                account.id(),
                account.email(),
                account.displayName(),
                account.enabled(),
                account.roles(),
                account.createdAt(),
                account.lastLoginAt());
    }
}
