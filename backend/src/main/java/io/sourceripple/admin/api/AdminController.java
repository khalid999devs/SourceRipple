package io.sourceripple.admin.api;

import io.sourceripple.admin.api.dto.AdminUserResponse;
import io.sourceripple.admin.api.dto.ReplaceRolesRequest;
import io.sourceripple.admin.api.dto.UpdateUserRequest;
import io.sourceripple.admin.application.AdminUserService;
import io.sourceripple.identity.application.access.AuthenticatedUser;
import io.sourceripple.shared.api.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminUserService adminUsers;

    public AdminController(AdminUserService adminUsers) {
        this.adminUsers = adminUsers;
    }

    @GetMapping("/users")
    public PageResponse<AdminUserResponse> listUsers(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminUsers.list(q, page, size);
    }

    @PatchMapping("/users/{userId}")
    public AdminUserResponse updateUser(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRequest request) {
        return adminUsers.update(userId, request);
    }

    @PutMapping("/users/{userId}/roles")
    public AdminUserResponse replaceRoles(
            @PathVariable UUID userId,
            @Valid @RequestBody ReplaceRolesRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        return adminUsers.replaceRoles(userId, request, admin.getId());
    }
}
