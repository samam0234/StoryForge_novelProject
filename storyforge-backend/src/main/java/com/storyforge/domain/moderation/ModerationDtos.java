package com.storyforge.domain.moderation;

import com.storyforge.domain.community.CommunityDtos.UserSummary;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ModerationDtos {

    private ModerationDtos() {
    }

    public enum Action { HIDE, RESTORE, DISMISS, SUSPEND, UNSUSPEND }

    public record ReportRequest(@NotNull ContentReport.TargetType targetType, @NotNull Long targetId,
                                @NotNull ContentReport.Reason reason, @Size(max = 500) String detail) {
    }

    public record ReportReceipt(Long id, ContentReport.Status status) {
    }

    /** 관리자가 판단할 때 보는 대상 정보. 링크는 앱 안 경로입니다. */
    public record TargetView(String title, String excerpt, String link, UserSummary author, boolean hidden,
                             boolean authorSuspended, boolean exists) {
    }

    public record ReportView(Long id, ContentReport.TargetType targetType, Long targetId, ContentReport.Reason reason,
                             String detail, ContentReport.Status status, UserSummary reporter, long openReports,
                             TargetView target, String resolutionNote, Instant createdAt, Instant resolvedAt) {
    }

    public record DecideRequest(@NotNull Action action, @Size(max = 300) String note) {
    }
}
