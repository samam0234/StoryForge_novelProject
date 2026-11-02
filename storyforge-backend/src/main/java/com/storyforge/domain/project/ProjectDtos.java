package com.storyforge.domain.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record CreateProjectRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 100) String genre,
            Project.Form form) {
    }

    /** PUT /api/v1/projects/{id} — 프로젝트 설정 (기획서 §9.6-5) */
    public record UpdateProjectRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 100) String genre,
            @Pattern(regexp = "claude-opus-5-5|claude-sonnet-5-5|claude-haiku-5-5") String model,
            @Size(max = 2000) String defaultTone,
            @NotBlank @Pattern(regexp = "md|txt|docx|pdf|html|epub|odt|rtf") String exportFormat) {
    }

    public record ProjectResponse(
            Long id, String title, String genre, ProjectStatus status,
            String model, String defaultTone, String exportFormat,
            int totalChapters, int confirmedChapters,
            Project.Form form, Long workId, Long clonedFromWorkId, String clonedFromTitle, String cloneMode,
            long manualChars, long agentChars,
            Instant createdAt, Instant updatedAt) {
    }

    /** GET /api/v1/projects/{id}/status 응답 */
    public record ProjectStatusResponse(
            Long projectId, ProjectStatus status,
            int totalChapters, int confirmedChapters, Integer nextChapterNumber) {
    }
}
