package io.sourceripple.identity.api.dto;

import io.sourceripple.identity.domain.User;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String email, String displayName, Instant createdAt, Instant lastLoginAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt(), user.getLastLoginAt());
    }
}
