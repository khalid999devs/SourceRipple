package io.sourceripple.identity.api;

import io.sourceripple.identity.api.dto.LoginRequest;
import io.sourceripple.identity.api.dto.MeOrganization;
import io.sourceripple.identity.api.dto.MeResponse;
import io.sourceripple.identity.api.dto.RegisterRequest;
import io.sourceripple.identity.api.dto.UserResponse;
import io.sourceripple.identity.application.AuthService;
import io.sourceripple.identity.application.access.AuthenticatedUser;
import io.sourceripple.identity.infrastructure.security.SessionAuthenticator;
import io.sourceripple.workspace.application.organization.OrganizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionAuthenticator sessionAuthenticator;
    private final OrganizationService organizations;

    public AuthController(
            AuthService authService, SessionAuthenticator sessionAuthenticator, OrganizationService organizations) {
        this.authService = authService;
        this.sessionAuthenticator = sessionAuthenticator;
        this.organizations = organizations;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.created(URI.create("/api/v1/auth/me")).body(user);
    }

    @PostMapping("/login")
    public MeResponse login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        AuthenticatedUser user =
                sessionAuthenticator.login(request.email(), request.password(), httpRequest, httpResponse);
        return withOrganizations(authService.recordLogin(user.getId()));
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return withOrganizations(authService.currentUser(user.getId()));
    }

    private MeResponse withOrganizations(MeResponse me) {
        return me.withOrganizations(organizations.allOf(me.user().id()).stream()
                .map(organization -> new MeOrganization(organization.id(), organization.name(), organization.owner()))
                .toList());
    }
}
