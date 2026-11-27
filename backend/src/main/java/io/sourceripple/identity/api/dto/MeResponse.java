package io.sourceripple.identity.api.dto;

import io.sourceripple.identity.domain.RoleName;
import io.sourceripple.identity.domain.User;
import java.util.List;

public record MeResponse(UserResponse user, List<RoleName> roles, List<MeOrganization> organizations) {

    public static MeResponse from(User user) {
        return new MeResponse(UserResponse.from(user), user.roleNames(), List.of());
    }

    public MeResponse withOrganizations(List<MeOrganization> memberOf) {
        return new MeResponse(user, roles, List.copyOf(memberOf));
    }
}
