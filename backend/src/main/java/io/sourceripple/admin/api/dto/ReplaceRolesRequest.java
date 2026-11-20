package io.sourceripple.admin.api.dto;

import io.sourceripple.identity.domain.RoleName;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record ReplaceRolesRequest(@NotEmpty Set<@NotNull RoleName> roles) {}
