package com.storyforge.domain.community;

import com.storyforge.domain.bible.BibleDtos.BibleSnapshot;
import com.storyforge.domain.outline.OutlineChapter;
import com.storyforge.domain.user.AppUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/** 커뮤니티 API 계약. 다른 사람에게 보이는 사용자 정보에는 이메일을 넣지 않습니다. */
public final class CommunityDtos {

    private CommunityDtos() {
    }

    public record UserSummary(Long id, String displayName, String avatarColor) {

        public static UserSummary from(AppUser u) {
            return new UserSummary(u.getId(), u.getDisplayName(), u.getAvatarColor());
        }
    }

    public record PageResponse<T>(List<T> items, long total, int page, int size) {
    }

    public record WorkCard(
            Long id, Work.Kind kind, Work.PackageType packageType, String title, String synopsis, String genre,
            List<String> tags, UserSummary author, Work.Visibility visibility, Work.ClonePolicy clonePolicy,
            int revision, int chapterCount, int charCount, int likeCount, int commentCount, int cloneCount,
            int shareCount, long viewCount, Long parentWorkId, String parentTitle,
            boolean hidden, Instant publishedAt, Instant updatedAt) {

        public static WorkCard from(Work w) {
            return new WorkCard(w.getId(), w.getKind(), w.getPackageType(), w.getTitle(), w.getSynopsis(), w.getGenre(),
                    splitTags(w.getTags()), UserSummary.from(w.getAuthor()), w.getVisibility(), w.getClonePolicy(),
                    w.getRevision(), w.getChapterCount(), w.getCharCount(), w.getLikeCount(), w.getCommentCount(),
                    w.getCloneCount(), w.getShareCount(), w.getViewCount(), w.getParentWorkId(), w.getParentTitle(),
                    w.isHidden(), w.getPublishedAt(), w.getUpdatedAt());
        }

        private static List<String> splitTags(String tags) {
            return tags == null || tags.isBlank() ? List.of()
                    : Arrays.stream(tags.split(",")).map(t -> t.strip()).filter(t -> !t.isEmpty()).toList();
        }
    }

    public record ChapterEntry(int number, String title, int chars) {
    }

    public record RevisionEntry(int revision, String message, int chapterCount, int charCount, Instant createdAt) {

        public static RevisionEntry from(WorkRevision r) {
            return new RevisionEntry(r.getRevision(), r.getMessage(), r.getChapterCount(), r.getCharCount(), r.getCreatedAt());
        }
    }

    public record PackageItemView(Long id, int position, PackageItem.Role role, String note,
                                  PackageItem.Status status, WorkCard work) {

        public static PackageItemView from(PackageItem i) {
            return new PackageItemView(i.getId(), i.getPosition(), i.getRole(), i.getNote(), i.getStatus(),
                    WorkCard.from(i.getMember()));
        }
    }

    public record PackageLinkView(Long id, Long fromItemId, Long toItemId, PackageLink.Type type, String label) {

        public static PackageLinkView from(PackageLink l) {
            return new PackageLinkView(l.getId(), l.getFromItemId(), l.getToItemId(), l.getLinkType(), l.getLabel());
        }
    }

    public record CharacterAppearance(Long workId, String workTitle, String description) {
    }

    /** 통합 인물 사전 항목. 둘 이상의 작품에 나오면 crossover입니다. */
    public record PackageCharacter(String name, boolean crossover, List<CharacterAppearance> appearances) {
    }

    public record PackageView(String worldNote, List<PackageItemView> items, List<PackageLinkView> links,
                              List<PackageCharacter> characters) {
    }

    public record WorkDetail(
            WorkCard card, boolean owned, boolean liked, boolean downloadable, Long projectId,
            List<WorkClone.Mode> cloneModes, List<ChapterEntry> chapters, BibleSnapshot bible,
            List<OutlineChapter> outline, List<RevisionEntry> revisions, PackageView packageView,
            List<WorkCard> inPackages) {
    }

    public record WorkChapterResponse(int number, String title, String content, Integer previous, Integer next) {
    }

    /** POST /projects/{id}/publish */
    public record PublishRequest(
            @NotNull Work.Kind kind,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String synopsis,
            @Size(max = 100) String genre,
            @Size(max = 300) String tags,
            @NotNull Work.Visibility visibility,
            @NotNull Work.ClonePolicy clonePolicy,
            boolean downloadable) {
    }

    /** PUT /works/{id} */
    public record UpdateWorkRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String synopsis,
            @Size(max = 100) String genre,
            @Size(max = 300) String tags,
            @NotNull Work.Visibility visibility,
            @NotNull Work.ClonePolicy clonePolicy,
            boolean downloadable,
            Work.PackageType packageType,
            @Size(max = 20000) String worldNote) {
    }

    public record UpdateWorkChapterRequest(
            @Size(max = 200) String title,
            @NotNull String content,
            @Size(max = 300) String message) {
    }

    public record SyncWorkRequest(@Size(max = 300) String message) {
    }

    public record LikeResponse(boolean liked, int likeCount) {
    }

    // ───────────── Clone ─────────────

    public record CloneRequest(@NotNull WorkClone.Mode mode, @Size(max = 200) String title) {
    }

    public record CloneResponse(Long projectId, WorkClone.Mode mode, int revision) {
    }

    public record NetworkNode(Long workId, Long parentId, String title, UserSummary author, int revision,
                              int likeCount, int cloneCount, boolean current) {
    }

    public record CloneEntry(UserSummary user, WorkClone.Mode mode, int revision, Instant createdAt, Long derivedWorkId) {
    }

    public record CloneNetwork(Long rootId, List<NetworkNode> nodes, long world, long plot, long full,
                               List<CloneEntry> clones) {
    }

    public record UpstreamChapter(int number, String upstreamTitle, String localTitle, String state, int changedChars,
                                  String upstreamContent, String localContent) {
    }

    public record UpstreamCompare(Long workId, String title, WorkClone.Mode mode, int clonedRevision, int currentRevision,
                                  boolean available, List<UpstreamChapter> chapters) {
    }

    // ───────────── 공유 ─────────────

    public record ShareRequest(@NotNull WorkShare.Channel channel, @Size(max = 200) String target) {
    }

    public record ShareEntry(Long id, UserSummary user, WorkShare.Channel channel, String target, int visitCount,
                             Instant createdAt) {

        public static ShareEntry from(WorkShare s) {
            return new ShareEntry(s.getId(), UserSummary.from(s.getUser()), s.getChannel(), s.getTarget(),
                    s.getVisitCount(), s.getCreatedAt());
        }
    }

    public record ShareCreated(ShareEntry share, String token) {
    }

    public record ShareGraph(int total, int visits, List<ShareEntry> items) {
    }

    // ───────────── 패키지포지 ─────────────

    public record CreatePackageRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String synopsis,
            @Size(max = 100) String genre,
            @Size(max = 300) String tags,
            @NotNull Work.PackageType packageType,
            @Size(max = 20000) String worldNote,
            @NotNull Work.Visibility visibility,
            @NotEmpty List<Long> memberWorkIds) {
    }

    public record AddItemRequest(@NotNull Long memberWorkId, @NotNull PackageItem.Role role, @Size(max = 500) String note) {
    }

    public record UpdateItemRequest(@NotNull PackageItem.Role role, @Size(max = 500) String note) {
    }

    public record ReorderRequest(@NotEmpty List<Long> itemIds) {
    }

    public record AddLinkRequest(@NotNull Long fromItemId, @NotNull Long toItemId, @NotNull PackageLink.Type type,
                                 @Size(max = 200) String label) {
    }

    public record PackageRequest(Long itemId, Long packageId, String packageTitle, UserSummary requester, WorkCard work,
                                 Instant createdAt) {
    }

    public record DecideRequest(boolean approve) {
    }

    // ───────────── 게시글·댓글 ─────────────

    public record PostRequest(@NotNull Post.Category category, @NotBlank @Size(max = 200) String title,
                              @NotBlank @Size(max = 20000) String content) {
    }

    public record PostCard(Long id, Post.Category category, String title, UserSummary author, int likeCount,
                           int commentCount, long viewCount, Instant createdAt) {

        public static PostCard from(Post p) {
            return new PostCard(p.getId(), p.getCategory(), p.getTitle(), UserSummary.from(p.getAuthor()),
                    p.getLikeCount(), p.getCommentCount(), p.getViewCount(), p.getCreatedAt());
        }
    }

    public record PostDetail(PostCard card, String content, boolean owned, boolean liked, boolean hidden, Instant updatedAt) {
    }

    public record CommentRequest(@NotBlank @Size(max = 2000) String content, Long parentId) {
    }

    public record CommentEditRequest(@NotBlank @Size(max = 2000) String content) {
    }

    public record CommentView(Long id, Long parentId, UserSummary author, String content, boolean hidden, boolean canEdit,
                              boolean canDelete, Instant createdAt, Instant updatedAt) {
    }

    // ───────────── 이슈 ─────────────

    public record Celebration(Long id, IssueEvent.Metric metric, int threshold, String message, WorkCard work,
                              Instant createdAt) {
    }

    public record IssueBoard(List<WorkCard> trending, List<Celebration> celebrations, List<WorkCard> fresh) {
    }

    // ───────────── 프로필·계정 ─────────────

    public record ProfileStats(long works, long likesReceived, long clonesReceived, long posts, long comments,
                               long achievements) {
    }

    public record Profile(UserSummary user, String bio, String website, Instant joinedAt, boolean self,
                          boolean blockedByMe, boolean suspended, ProfileStats stats) {
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 100) String displayName,
            @Size(max = 500) String bio,
            @Size(max = 300) @Pattern(regexp = "^$|^https?://\\S+$", message = "http 또는 https 주소여야 합니다") String website,
            @Pattern(regexp = "^$|^#[0-9a-fA-F]{6}$", message = "#RRGGBB 형식이어야 합니다") String avatarColor) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }

    public record DeleteAccountRequest(@NotBlank String password) {
    }
}
