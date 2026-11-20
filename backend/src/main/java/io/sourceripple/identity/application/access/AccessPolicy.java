package io.sourceripple.identity.application.access;

import io.sourceripple.identity.domain.RoleName;
import io.sourceripple.shared.api.error.status.ForbiddenException;
import io.sourceripple.shared.api.error.status.NotFoundException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("access")
public class AccessPolicy {

    private final MembershipLookup memberships;
    private final Map<ResourceType, OrganizationResolver> resolvers = new EnumMap<>(ResourceType.class);

    public AccessPolicy(MembershipLookup memberships, List<OrganizationResolver> resolvers) {
        this.memberships = memberships;
        resolvers.forEach(resolver -> this.resolvers.put(resolver.resourceType(), resolver));
    }

    public boolean canReadOrganization(UUID organizationId) {
        requireMember(organizationId, currentUser());
        return true;
    }

    public boolean requireOwner(UUID organizationId) {
        AuthenticatedUser user = currentUser();
        requireMember(organizationId, user);
        if (!memberships.isOwner(organizationId, user.getId())) {
            throw new ForbiddenException("Only the organization owner can do this.");
        }
        return true;
    }

    public boolean canReadProject(UUID projectId) {
        requireMember(resolve(ResourceType.PROJECT, projectId), currentUser());
        return true;
    }

    public boolean canWriteProject(UUID projectId) {
        AuthenticatedUser user = currentUser();
        requireMember(resolve(ResourceType.PROJECT, projectId), user);
        requireRole(user, RoleName.MAINTAINER);
        return true;
    }

    public UUID resolve(ResourceType type, UUID resourceId) {
        OrganizationResolver resolver = resolvers.get(type);
        if (resolver == null || resourceId == null) {
            throw notFound();
        }
        return resolver.organizationOf(resourceId).orElseThrow(AccessPolicy::notFound);
    }

    private void requireMember(UUID organizationId, AuthenticatedUser user) {
        if (organizationId == null || !memberships.isMember(organizationId, user.getId())) {
            throw notFound();
        }
    }

    private static void requireRole(AuthenticatedUser user, RoleName role) {
        if (!user.hasRole(role)) {
            throw new ForbiddenException("This action requires the " + role + " role.");
        }
    }

    private static AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new InsufficientAuthenticationException("Authentication is required.");
        }
        return user;
    }

    private static NotFoundException notFound() {
        // Non-members get the same 404 as a missing resource so existence never leaks.
        return new NotFoundException("The resource was not found.");
    }
}
