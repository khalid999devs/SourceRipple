package io.sourceripple.workspace.api;

import io.sourceripple.shared.api.PageResponse;
import io.sourceripple.workspace.api.dto.organization.AddMemberRequest;
import io.sourceripple.workspace.api.dto.organization.MemberResponse;
import io.sourceripple.workspace.application.organization.MembershipService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/members")
public class MemberController {

    private final MembershipService memberships;

    public MemberController(MembershipService memberships) {
        this.memberships = memberships;
    }

    @GetMapping
    public PageResponse<MemberResponse> list(
            @PathVariable UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return memberships.list(organizationId, page, size);
    }

    @PostMapping
    public ResponseEntity<MemberResponse> add(
            @PathVariable UUID organizationId, @Valid @RequestBody AddMemberRequest request) {
        MemberResponse member = memberships.add(organizationId, request);
        return ResponseEntity.created(
                        URI.create("/api/v1/organizations/" + organizationId + "/members/" + member.userId()))
                .body(member);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> remove(@PathVariable UUID organizationId, @PathVariable UUID userId) {
        memberships.remove(organizationId, userId);
        return ResponseEntity.noContent().build();
    }
}
