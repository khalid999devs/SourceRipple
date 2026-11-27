package io.sourceripple.identity.infrastructure.security;

import io.sourceripple.identity.application.LoginThrottle;
import io.sourceripple.identity.application.access.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.stereotype.Component;

@Component
public class SessionAuthenticator {

    private final LoginThrottle loginThrottle;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfAuthenticationStrategy csrfRotation;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

    public SessionAuthenticator(
            LoginThrottle loginThrottle,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository) {
        this.loginThrottle = loginThrottle;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.csrfRotation = new CsrfAuthenticationStrategy(csrfTokenRepository);
        this.csrfRotation.setRequestHandler(new CsrfTokenRequestAttributeHandler());
    }

    public AuthenticatedUser login(
            String email, String password, HttpServletRequest request, HttpServletResponse response) {
        loginThrottle.check(request.getRemoteAddr(), email);
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, password));

        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        contextHolder.setContext(context);
        // Spring Security does not persist the context for a custom login endpoint on its own.
        securityContextRepository.saveContext(context, request, response);

        rotateCsrfToken(authentication, request, response);
        return (AuthenticatedUser) authentication.getPrincipal();
    }

    private void rotateCsrfToken(
            Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        csrfRotation.onAuthentication(authentication, request, response);
        CsrfToken fresh = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        // Reading the new token writes its cookie into the login response, so the SPA can use it right away.
        if (fresh != null) {
            fresh.getToken();
        }
    }
}
