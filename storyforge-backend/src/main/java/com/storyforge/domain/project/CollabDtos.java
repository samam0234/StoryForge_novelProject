package com.storyforge.domain.project;

import com.storyforge.domain.community.CommunityDtos.UserSummary;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** 공동 창작 API. 응답에 이메일은 넣지 않습니다. */
public final class CollabDtos {

    private CollabDtos() {
    }

    public record InviteRequest(@NotBlank @Email @Size(max = 200) String email) {
    }

    public record CollaboratorView(Long id, Long projectId, String projectTitle, UserSummary user,
                                   String status, Instant createdAt) {
    }

    public record CommitRequest(
            @NotBlank @Size(max = 500) String message,
            @NotNull @Pattern(regexp = "BIBLE|OUTLINE|CHAPTER") String target,
            Integer chapterNumber,
            Integer startOffset,
            Integer endOffset,
            @Size(max = 2000) String quote) {
    }

    public record CommitView(Long id, Long projectId, UserSummary author, Long parentId, String message,
                             String target, Integer chapterNumber, Integer startOffset, Integer endOffset,
                             String quote, Instant createdAt) {
    }
}
