package com.storyforge.service.moderation;

import com.storyforge.domain.community.*;
import com.storyforge.domain.community.CommunityDtos.PageResponse;
import com.storyforge.domain.community.CommunityDtos.UserSummary;
import com.storyforge.domain.moderation.ContentReport;
import com.storyforge.domain.moderation.ContentReport.Status;
import com.storyforge.domain.moderation.ContentReport.TargetType;
import com.storyforge.domain.moderation.ContentReportRepository;
import com.storyforge.domain.moderation.ModerationDtos.*;
import com.storyforge.domain.moderation.UserBlock;
import com.storyforge.domain.moderation.UserBlockRepository;
import com.storyforge.domain.notification.Notification;
import com.storyforge.domain.user.AppUser;
import com.storyforge.domain.user.AppUserRepository;
import com.storyforge.exception.InvalidStateException;
import com.storyforge.exception.NotFoundException;
import com.storyforge.service.auth.AccountPolicy;
import com.storyforge.service.notification.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 신고, 관리자 조치, 사용자 차단. 숨긴 글은 지우지 않고 복원할 수 있습니다.
 * 계정 정지는 쓰기만 막습니다 (AccountGuardInterceptor).
 */
@Service
@Transactional(readOnly = true)
public class ModerationService {

    private static final int EXCERPT = 300;

    private final ContentReportRepository reports;
    private final UserBlockRepository blocks;
    private final WorkRepository works;
    private final PostRepository posts;
    private final CommentRepository comments;
    private final AppUserRepository users;
    private final AccountPolicy policy;
    private final NotificationService notifications;

    public ModerationService(ContentReportRepository reports, UserBlockRepository blocks, WorkRepository works,
                             PostRepository posts, CommentRepository comments, AppUserRepository users,
                             AccountPolicy policy, NotificationService notifications) {
        this.reports = reports;
        this.blocks = blocks;
        this.works = works;
        this.posts = posts;
        this.comments = comments;
        this.users = users;
        this.policy = policy;
        this.notifications = notifications;
    }

    // ───────────── 신고 ─────────────

    /** 같은 대상을 처리 전에 다시 신고하면 새로 만들지 않습니다. 볼 수 없는 대상은 404, 내 것은 409입니다. */
    @Transactional
    public ReportReceipt report(ReportRequest request, Long reporterId) {
        TargetView target = target(request.targetType(), request.targetId());
        boolean admin = policy.isAdminId(reporterId);
        if (!target.exists() || (target.hidden() && !admin)) {
            throw new NotFoundException("신고할 대상을 찾을 수 없습니다.");
        }
        if (reporterId.equals(target.author().id())) {
            throw new InvalidStateException("내 글이나 내 계정은 신고할 수 없습니다.");
        }
        ContentReport existing = reports.findFirstByReporterIdAndTargetTypeAndTargetIdAndStatus(
                reporterId, request.targetType(), request.targetId(), Status.OPEN).orElse(null);
        if (existing != null) {
            return new ReportReceipt(existing.getId(), existing.getStatus());
        }
        ContentReport saved = reports.save(new ContentReport(user(reporterId), request.targetType(), request.targetId(),
                request.reason(), blankToNull(request.detail())));
        return new ReportReceipt(saved.getId(), saved.getStatus());
    }

    public PageResponse<ReportView> list(Status status, int page, int size, Long adminId) {
        policy.requireAdmin(adminId);
        Page<ContentReport> result = reports.findByStatusOrderByCreatedAtDesc(status,
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50)));
        return new PageResponse<>(result.stream().map(this::view).toList(), result.getTotalElements(),
                result.getNumber(), result.getSize());
    }

    @Transactional
    public ReportView decide(Long reportId, DecideRequest request, Long adminId) {
        policy.requireAdmin(adminId);
        ContentReport report = reports.findById(Objects.requireNonNull(reportId))
                .orElseThrow(() -> new NotFoundException("신고를 찾을 수 없습니다: " + reportId));
        TargetType type = report.getTargetType();
        Long id = report.getTargetId();
        String note = blankToNull(request.note());
        TargetView target = target(type, id);
        Long authorId = target.author() == null ? null : target.author().id();
        switch (request.action()) {
            case HIDE -> {
                requireContent(type);
                setHidden(type, id, true);
                closeOpen(type, id, Status.RESOLVED, adminId, note);
                notifyAuthor(authorId, "운영 정책에 따라 내 글이 숨겨졌습니다: " + target.title() + reasonSuffix(note), target.link());
            }
            case RESTORE -> {
                requireContent(type);
                setHidden(type, id, false);
                notifyAuthor(authorId, "숨겨졌던 글이 복원되었습니다: " + target.title(), target.link());
            }
            case DISMISS -> closeOpen(type, id, Status.DISMISSED, adminId, note);
            case SUSPEND -> {
                AppUser author = user(authorId);
                if (policy.isAdmin(author)) {
                    throw new InvalidStateException("관리자 계정은 정지할 수 없습니다.");
                }
                author.suspend(note);
                closeOpen(type, id, Status.RESOLVED, adminId, note);
                notifyAuthor(authorId, "계정 이용이 정지되었습니다." + reasonSuffix(note), null);
            }
            case UNSUSPEND -> {
                user(authorId).unsuspend();
                notifyAuthor(authorId, "계정 이용 정지가 풀렸습니다.", null);
            }
        }
        return view(report);
    }

    private void closeOpen(TargetType type, Long id, Status status, Long adminId, String note) {
        for (ContentReport r : reports.findByTargetTypeAndTargetIdAndStatus(type, id, Status.OPEN)) {
            r.close(status, adminId, note);
            // 신고자에게는 조치 결과만 알립니다. 누가 신고했는지는 작성자에게 알리지 않습니다.
            notifications.send(r.getReporter().getId(), Notification.Type.MODERATION,
                    status == Status.RESOLVED ? "신고하신 내용을 처리했습니다." : "신고하신 내용을 검토했으며 조치하지 않았습니다.", null);
        }
    }

    private void notifyAuthor(Long authorId, String message, String link) {
        if (authorId != null) {
            notifications.send(authorId, Notification.Type.MODERATION, message, link);
        }
    }

    private static String reasonSuffix(String note) {
        return note == null ? "" : " (사유: " + note + ")";
    }

    private static void requireContent(TargetType type) {
        if (type == TargetType.USER) {
            throw new InvalidStateException("계정은 숨길 수 없습니다. 정지를 사용하세요.");
        }
    }

    private void setHidden(TargetType type, Long id, boolean hidden) {
        switch (type) {
            case WORK -> works.findById(Objects.requireNonNull(id)).ifPresent(w -> w.setHidden(hidden));
            case POST -> posts.findById(Objects.requireNonNull(id)).ifPresent(p -> p.setHidden(hidden));
            case COMMENT -> comments.findById(Objects.requireNonNull(id)).ifPresent(c -> c.setHidden(hidden));
            case USER -> throw new InvalidStateException("계정은 숨길 수 없습니다.");
        }
    }

    private ReportView view(ContentReport r) {
        return new ReportView(r.getId(), r.getTargetType(), r.getTargetId(), r.getReason(), r.getDetail(), r.getStatus(),
                UserSummary.from(r.getReporter()),
                reports.countByTargetTypeAndTargetIdAndStatus(r.getTargetType(), r.getTargetId(), Status.OPEN),
                target(r.getTargetType(), r.getTargetId()), r.getResolutionNote(), r.getCreatedAt(), r.getResolvedAt());
    }

    /** 신고 대상을 읽습니다. 지워졌으면 exists=false입니다. */
    private TargetView target(TargetType type, Long id) {
        return switch (type) {
            case WORK -> works.findWithAuthor(Objects.requireNonNull(id))
                    .map(w -> targetView(w.getTitle(), w.getSynopsis(), "/works/" + w.getId(), w.getAuthor(), w.isHidden()))
                    .orElse(missing());
            case POST -> posts.findWithAuthor(Objects.requireNonNull(id))
                    .map(p -> targetView(p.getTitle(), p.getContent(), "/posts/" + p.getId(), p.getAuthor(), p.isHidden()))
                    .orElse(missing());
            case COMMENT -> comments.findWithAuthor(Objects.requireNonNull(id))
                    .map(c -> targetView("댓글", c.getContent(),
                            c.getWorkId() != null ? "/works/" + c.getWorkId() : "/posts/" + c.getPostId(),
                            c.getAuthor(), c.isHidden()))
                    .orElse(missing());
            case USER -> users.findById(Objects.requireNonNull(id))
                    .map(u -> targetView(u.getDisplayName(), u.getBio(), "/users/" + u.getId(), u, false))
                    .orElse(missing());
        };
    }

    private static TargetView targetView(String title, String text, String link, AppUser author, boolean hidden) {
        String excerpt = text == null || text.length() <= EXCERPT ? text : text.substring(0, EXCERPT) + "…";
        return new TargetView(title, excerpt, link, UserSummary.from(author), hidden, author.isSuspended(), true);
    }

    private static TargetView missing() {
        return new TargetView("(삭제됨)", null, null, null, false, false, false);
    }

    // ───────────── 차단 ─────────────

    @Transactional
    public void block(Long blocker, Long blocked) {
        if (blocker.equals(blocked)) {
            throw new InvalidStateException("자기 자신을 차단할 수 없습니다.");
        }
        user(blocked);
        blocks.block(blocker, blocked);
    }

    @Transactional
    public void unblock(Long blocker, Long blocked) {
        blocks.unblock(blocker, blocked);
    }

    public List<UserSummary> blockedUsers(Long blocker) {
        List<Long> ids = blocks.findByBlockerIdOrderByCreatedAtDesc(blocker).stream().map(UserBlock::getBlockedId).toList();
        return users.findAllById(ids).stream().map(UserSummary::from).toList();
    }

    private AppUser user(Long id) {
        if (id == null) {
            throw new NotFoundException("사용자를 찾을 수 없습니다.");
        }
        return users.findById(id).orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다: " + id));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
