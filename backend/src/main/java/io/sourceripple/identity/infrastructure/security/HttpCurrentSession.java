package io.sourceripple.identity.infrastructure.security;

import io.sourceripple.identity.application.access.AuthenticatedUser;
import io.sourceripple.identity.application.account.CurrentSession;
import io.sourceripple.identity.domain.RoleName;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
class HttpCurrentSession implements CurrentSession {

    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

    HttpCurrentSession(SecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
    }

    @Override
    public Optional<String> id() {
        return request().map(attributes -> attributes.getRequest().getSession(false)).map(HttpSession::getId);
    }

    @Override
    public void refreshRoles(Set<RoleName> roles) {
        Optional<ServletRequestAttributes> request = request();
        Authentication current = contextHolder.getContext().getAuthentication();
        if (request.isEmpty() || current == null || !(current.getPrincipal() instanceof AuthenticatedUser user)) {
            return;
        }

        List<GrantedAuthority> authorities = AuthenticatedUser.authoritiesOf(roles);
        AuthenticatedUser refreshed = user.withAuthorities(authorities);
        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(refreshed, null, authorities));
        contextHolder.setContext(context);
        securityContextRepository.saveContext(context, request.get().getRequest(), request.get().getResponse());
    }

    private static Optional<ServletRequestAttributes> request() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? Optional.of(attributes)
                : Optional.empty();
    }
}
