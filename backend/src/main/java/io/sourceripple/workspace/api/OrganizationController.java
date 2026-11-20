package io.sourceripple.workspace.api;

import io.sourceripple.identity.application.access.AuthenticatedUser;
import io.sourceripple.shared.api.PageResponse;
import io.sourceripple.workspace.api.dto.organization.CreateOrganizationRequest;
import io.sourceripple.workspace.api.dto.organization.OrganizationResponse;
import io.sourceripple.workspace.api.dto.organization.RenameOrganizationRequest;
import io.sourceripple.workspace.application.organization.OrganizationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizations;

    public OrganizationController(OrganizationService organizations) {
        this.organizations = organizations;
    }

    @PostMapping
    public ResponseEntity<OrganizationResponse> create(
            @Valid @RequestBody CreateOrganizationRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        OrganizationResponse created = organizations.create(request, user.getId());
        return ResponseEntity.created(URI.create("/api/v1/organizations/" + created.id()))
                .body(created);
    }

    @GetMapping
    public PageResponse<OrganizationResponse> listMine(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return organizations.listMine(user.getId(), page, size);
    }

    @GetMapping("/{organizationId}")
    public OrganizationResponse get(
            @PathVariable UUID organizationId, @AuthenticationPrincipal AuthenticatedUser user) {
        return organizations.get(organizationId, user.getId());
    }

    @PatchMapping("/{organizationId}")
    public OrganizationResponse rename(
            @PathVariable UUID organizationId,
            @Valid @RequestBody RenameOrganizationRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return organizations.rename(organizationId, request, user.getId());
    }

    @DeleteMapping("/{organizationId}")
    public ResponseEntity<Void> delete(@PathVariable UUID organizationId) {
        organizations.delete(organizationId);
        return ResponseEntity.noContent().build();
    }
}
