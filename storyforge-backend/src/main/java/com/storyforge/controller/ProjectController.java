package com.storyforge.controller;

import com.storyforge.domain.project.ProjectDtos.CreateProjectRequest;
import com.storyforge.domain.project.ProjectDtos.ProjectResponse;
import com.storyforge.domain.project.ProjectDtos.ProjectStatusResponse;
import com.storyforge.domain.project.ProjectDtos.UpdateProjectRequest;
import com.storyforge.service.auth.CurrentUser;
import com.storyforge.service.project.ExportService;
import com.storyforge.service.project.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ExportService exportService;
    private final CurrentUser currentUser;

    public ProjectController(ProjectService projectService, ExportService exportService, CurrentUser currentUser) {
        this.projectService = projectService;
        this.exportService = exportService;
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

    @PutMapping("/{projectId}")
    public ProjectResponse update(@PathVariable Long projectId, @Valid @RequestBody UpdateProjectRequest request) {
        return projectService.update(projectId, request);
    }

    /** 승인된 챕터 내보내기 (기획서 §6.4). format을 생략하면 프로젝트 설정값을 씁니다. */
    @GetMapping("/{projectId}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long projectId, @RequestParam(required = false) String format) {
        ExportService.ExportFile file = exportService.export(projectId, format);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.filename(), StandardCharsets.UTF_8).build().toString())
                .body(file.bytes());
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
