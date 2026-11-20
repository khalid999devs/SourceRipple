package io.sourceripple.workspace.api;

import io.sourceripple.shared.api.PageResponse;
import io.sourceripple.workspace.api.dto.project.CreateProjectRequest;
import io.sourceripple.workspace.api.dto.project.ProjectResponse;
import io.sourceripple.workspace.api.dto.project.ProjectSummaryResponse;
import io.sourceripple.workspace.api.dto.project.UpdateProjectRequest;
import io.sourceripple.workspace.application.project.ProjectService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1")
public class ProjectController {

    private final ProjectService projects;

    public ProjectController(ProjectService projects) {
        this.projects = projects;
    }

    @PostMapping("/organizations/{organizationId}/projects")
    public ResponseEntity<ProjectResponse> create(
            @PathVariable UUID organizationId, @Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse created = projects.create(organizationId, request);
        return ResponseEntity.created(URI.create("/api/v1/projects/" + created.id())).body(created);
    }

    @GetMapping("/organizations/{organizationId}/projects")
    public PageResponse<ProjectResponse> list(
            @PathVariable UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return projects.list(organizationId, page, size);
    }

    @GetMapping("/projects/{projectId}")
    public ProjectResponse get(@PathVariable UUID projectId) {
        return projects.get(projectId);
    }

    @PatchMapping("/projects/{projectId}")
    public ProjectResponse update(@PathVariable UUID projectId, @Valid @RequestBody UpdateProjectRequest request) {
        return projects.update(projectId, request);
    }

    @DeleteMapping("/projects/{projectId}")
    public ResponseEntity<Void> archive(@PathVariable UUID projectId) {
        projects.archive(projectId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/projects/{projectId}/summary")
    public ProjectSummaryResponse summary(@PathVariable UUID projectId) {
        return projects.summary(projectId);
    }
}
