package com.storyforge.controller;

import com.storyforge.domain.project.ProjectDtos.CreateProjectRequest;
import com.storyforge.domain.project.ProjectDtos.ProjectResponse;
import com.storyforge.domain.project.ProjectDtos.ProjectStatusResponse;
import com.storyforge.service.auth.CurrentUser;
import com.storyforge.service.project.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final CurrentUser currentUser;

    public ProjectController(ProjectService projectService, CurrentUser currentUser) {
        this.projectService = projectService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody CreateProjectRequest request) {
        return projectService.create(request, currentUser.id());
    }

    @GetMapping
    public List<ProjectResponse> list() {
        return projectService.list(currentUser.id());
    }

    @GetMapping("/{projectId}")
    public ProjectResponse get(@PathVariable Long projectId) {
        return projectService.get(projectId);
    }

    @GetMapping("/{projectId}/status")
    public ProjectStatusResponse status(@PathVariable Long projectId) {
        return projectService.status(projectId);
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long projectId) {
        projectService.delete(projectId);
    }
}
