package com.storyforge.domain.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record CreateProjectRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 100) String genre) {
    }

    public record ProjectResponse(
            Long id, String title, String genre, ProjectStatus status,
            int totalChapters, int confirmedChapters,
            Instant createdAt, Instant updatedAt) {
    }

    /** GET /api/v1/projects/{id}/status 응답 */
    public record ProjectStatusResponse(
            Long projectId, ProjectStatus status,
            int totalChapters, int confirmedChapters, Integer nextChapterNumber) {
    }
}
