package io.sourceripple.identity.application.account;

import io.sourceripple.identity.domain.Role;
import io.sourceripple.identity.domain.RoleName;
import io.sourceripple.identity.domain.RoleRepository;
import io.sourceripple.identity.domain.User;
import io.sourceripple.identity.domain.UserRepository;
import io.sourceripple.shared.api.ErrorCode;
import io.sourceripple.shared.api.error.status.BadRequestException;
import io.sourceripple.shared.api.error.status.NotFoundException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserAccountService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    public UserAccountService(
            UserRepository users,
            RoleRepository roles,
            FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.users = users;
        this.roles = roles;
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public Page<UserAccount> search(String query, Pageable pageable) {
        Page<User> page = StringUtils.hasText(query)
                ? users.findByEmailContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
                        query.trim(), query.trim(), pageable)
                : users.findAll(pageable);
        return page.map(UserAccount::from);
    }

    @Transactional
    public UserAccount setEnabled(UUID userId, boolean enabled) {
        User user = find(userId);
        if (!enabled && user.isEnabled() && isAdmin(user)) {
            requireAnotherEnabledAdmin();
        }
        user.setEnabled(enabled);
        if (!enabled) {
            endSessions(user);
        }
        return UserAccount.from(user);
    }

    @Transactional
    public UserAccount replaceRoles(UUID userId, Set<RoleName> roleNames, UUID actingUserId) {
        User user = find(userId);
        boolean removesAdmin = isAdmin(user) && !roleNames.contains(RoleName.ADMIN);
        if (removesAdmin && userId.equals(actingUserId)) {
            throw new BadRequestException(ErrorCode.ROLE_NOT_ALLOWED, "You cannot remove your own ADMIN role.");
        }
        if (removesAdmin && user.isEnabled()) {
            requireAnotherEnabledAdmin();
        }
        Set<Role> newRoles = roleNames.stream()
                .map(name -> roles.findByName(name).orElseThrow())
                .collect(Collectors.toSet());
        user.replaceRoles(newRoles);
        // Authorities are cached in the session, so stale sessions would keep the old roles.
        endSessions(user);
        return UserAccount.from(user);
    }

    private User find(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException("The user was not found."));
    }

    private void requireAnotherEnabledAdmin() {
        if (users.countByEnabledTrueAndRolesName(RoleName.ADMIN) <= 1) {
            throw new BadRequestException(ErrorCode.ROLE_NOT_ALLOWED, "At least one enabled ADMIN must remain.");
        }
    }

    private void endSessions(User user) {
        sessions.findByPrincipalName(user.getEmail()).keySet().forEach(sessions::deleteById);
    }

    private static boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN);
    }
}
