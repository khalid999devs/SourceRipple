package io.sourceripple.admin.application;

import io.sourceripple.admin.api.dto.AdminUserResponse;
import io.sourceripple.admin.api.dto.ReplaceRolesRequest;
import io.sourceripple.admin.api.dto.UpdateUserRequest;
import io.sourceripple.identity.application.account.UserAccountService;
import io.sourceripple.shared.api.PageResponse;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class AdminUserService {

    private final UserAccountService accounts;

    public AdminUserService(UserAccountService accounts) {
        this.accounts = accounts;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<AdminUserResponse> list(String query, int page, int size) {
        return PageResponse.from(
                accounts.search(query, PageResponse.request(page, size, Sort.by("createdAt", "id"))),
                AdminUserResponse::from);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public AdminUserResponse update(UUID userId, UpdateUserRequest request) {
        return AdminUserResponse.from(accounts.setEnabled(userId, request.enabled()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public AdminUserResponse replaceRoles(UUID userId, ReplaceRolesRequest request, UUID actingUserId) {
        return AdminUserResponse.from(accounts.replaceRoles(userId, request.roles(), actingUserId));
    }
}
