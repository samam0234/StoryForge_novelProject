package com.storyforge.domain.chapter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ChapterDtos {

    private ChapterDtos() {
    }

    public record ContinuityReportDto(List<ContinuityIssue> issues, int score, Instant createdAt) {

        public static ContinuityReportDto from(ContinuityReport r) {
            return r == null ? null : new ContinuityReportDto(r.getIssues(), r.getScore(), r.getCreatedAt());
        }
    }

    public record ChapterResponse(
            Long id, Long projectId, int number, String title, String content,
            ChapterStatus status, String summary, Map<String, Object> generationContext,
            Instant createdAt, ContinuityReportDto continuityReport) {

        public static ChapterResponse from(Chapter c, ContinuityReport report) {
            return new ChapterResponse(c.getId(), c.getProject().getId(), c.getNumber(), c.getTitle(),
                    c.getContent(), c.getStatus(), c.getSummary(), c.getGenerationContext(),
                    c.getCreatedAt(), ContinuityReportDto.from(report));
        }
    }

    /** PUT /api/v1/chapters/{id} — 검토 중 사용자 수정 */
    public record UpdateChapterRequest(String title, String content) {
    }
}
