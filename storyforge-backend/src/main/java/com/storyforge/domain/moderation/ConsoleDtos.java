package com.storyforge.domain.moderation;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ConsoleDtos {
    private ConsoleDtos() { }

    public record Overview(long users, long suspendedUsers, long projects, long works, long openReports,
                           Map<String, Long> pipeline, Instant capturedAt) { }

    public record UserView(Long id, String email, String displayName, boolean admin, boolean suspended,
                           String suspendedReason, Instant createdAt) { }

    public record UserDecision(boolean suspended, @NotBlank @Size(max = 300) String note) { }

    public record BatchDecision(@NotEmpty @Size(max = 50) List<@NotNull @Positive Long> reportIds,
                                @NotNull ModerationDtos.Action action,
                                @NotBlank @Size(max = 300) String note) { }

    public record BatchResult(int requested, int processed) { }

    public record AuditView(long id, long actorId, String actorName, String action, String targetType,
                            long targetId, Long reportId, String note, Instant createdAt) { }
}
