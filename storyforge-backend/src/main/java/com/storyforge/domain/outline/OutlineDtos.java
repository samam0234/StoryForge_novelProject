package com.storyforge.domain.outline;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class OutlineDtos {

    private OutlineDtos() {
    }

    public record OutlineResponse(Long projectId, List<OutlineChapter> chapters, Instant updatedAt) {

        public static OutlineResponse from(Outline o) {
            return new OutlineResponse(o.getProject().getId(), o.getChapters(), o.getUpdatedAt());
        }
    }

    /** PUT /api/v1/projects/{id}/outline — 순서 재배치 포함 전체 교체 */
    public record UpdateOutlineRequest(@NotNull List<OutlineChapter> chapters) {
    }

    /** POST /api/v1/projects/{id}/outline/generate */
    public record GenerateOutlineRequest(@Min(1) @Max(50) int chapterCount) {
    }
}
