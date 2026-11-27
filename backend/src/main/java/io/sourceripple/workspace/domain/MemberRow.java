package io.sourceripple.workspace.domain;

import java.time.Instant;
import java.util.UUID;

public interface MemberRow {

    UUID getUserId();

    String getEmail();

    String getDisplayName();

    Instant getAddedAt();
}
