package io.sourceripple.workspace.api.dto.organization;

import io.sourceripple.workspace.domain.MemberRow;
import java.time.Instant;
import java.util.UUID;

public record MemberResponse(UUID userId, String email, String displayName, boolean owner, Instant addedAt) {

    public static MemberResponse from(MemberRow row, UUID ownerId) {
        return new MemberResponse(
                row.getUserId(), row.getEmail(), row.getDisplayName(), row.getUserId().equals(ownerId), row.getAddedAt());
    }
}
