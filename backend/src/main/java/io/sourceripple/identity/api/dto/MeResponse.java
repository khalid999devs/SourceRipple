package io.sourceripple.identity.api.dto;

import io.sourceripple.identity.domain.Role;
import io.sourceripple.identity.domain.RoleName;
import io.sourceripple.identity.domain.User;
import java.util.List;

public record MeResponse(UserResponse user, List<RoleName> roles) {

    public static MeResponse from(User user) {
        List<RoleName> roleNames =
                user.getRoles().stream().map(Role::getName).sorted().toList();
        return new MeResponse(UserResponse.from(user), roleNames);
    }
}
