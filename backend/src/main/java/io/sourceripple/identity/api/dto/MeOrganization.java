package io.sourceripple.identity.api.dto;

import java.util.UUID;

public record MeOrganization(UUID id, String name, boolean owner) {}
