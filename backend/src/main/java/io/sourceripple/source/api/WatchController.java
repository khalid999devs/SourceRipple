package io.sourceripple.source.api;

import io.sourceripple.identity.application.access.AuthenticatedUser;
import io.sourceripple.source.api.dto.WatchPreviewRequest;
import io.sourceripple.source.api.dto.WatchPreviewResponse;
import io.sourceripple.source.application.preview.WatchPreviewService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class WatchController {

    private final WatchPreviewService previews;

    public WatchController(WatchPreviewService previews) {
        this.previews = previews;
    }

    @PostMapping("/projects/{projectId}/watches/preview")
    public WatchPreviewResponse preview(
            @PathVariable UUID projectId,
            @Valid @RequestBody WatchPreviewRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return previews.preview(projectId, request, user.getId());
    }
}
