package io.sourceripple.identity.application.account;

import io.sourceripple.identity.domain.RoleName;
import java.util.Optional;
import java.util.Set;

/** The session of the request being handled, if there is one. */
public interface CurrentSession {

    Optional<String> id();

    void refreshRoles(Set<RoleName> roles);
}
