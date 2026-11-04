package com.storyforge.domain.moderation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ModerationDtos {

    private ModerationDtos() {
    }

    public record ReportRequest(
            @NotNull ContentReport.TargetType targetType,
            @NotNull Long targetId,
            @NotNull ContentReport.Reason reason,
            @Size(max = 1000) String detail) {
    }

    public record ReportView(
            Long id, Long reporterId, String reporterName, ContentReport.TargetType targetType, Long targetId,
            ContentReport.Reason reason, String detail, ContentReport.Status status, Instant createdAt) {

        public static ReportView from(ContentReport report) {
            return new ReportView(report.getId(), report.getReporter().getId(), report.getReporter().getDisplayName(),
                    report.getTargetType(), report.getTargetId(), report.getReason(), report.getDetail(),
                    report.getStatus(), report.getCreatedAt());
        }
    }

    public record BlockRequest(@NotNull Long userId) {
    }

    public record LlmNotice(String provider, boolean external, String notice) {
    }
}
