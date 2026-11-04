package com.storyforge.service.community;

import com.storyforge.domain.community.Comment;
import com.storyforge.domain.community.CommentRepository;
import com.storyforge.domain.community.Post;
import com.storyforge.domain.community.PostRepository;
import com.storyforge.domain.community.Work;
import com.storyforge.domain.community.WorkRepository;
import com.storyforge.domain.moderation.ContentReport;
import com.storyforge.domain.moderation.ContentReportRepository;
import com.storyforge.domain.moderation.ModerationDtos.BlockRequest;
import com.storyforge.domain.moderation.ModerationDtos.ReportRequest;
import com.storyforge.domain.moderation.ModerationDtos.ReportView;
import com.storyforge.domain.moderation.UserBlock;
import com.storyforge.domain.moderation.UserBlockKey;
import com.storyforge.domain.moderation.UserBlockRepository;
import com.storyforge.domain.user.AppUser;
import com.storyforge.domain.user.AppUserRepository;
import com.storyforge.domain.user.Role;
import com.storyforge.exception.InvalidStateException;
import com.storyforge.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/** 신고, 차단, 관리자의 숨김. 관리자가 아니면 관리 API는 없는 것처럼 404입니다. */
@Service
@Transactional(readOnly = true)
public class ModerationService {

    private final AppUserRepository users;
    private final ContentReportRepository reports;
    private final UserBlockRepository blocks;
    private final PostRepository posts;
    private final WorkRepository works;
    private final CommentRepository comments;
    private final BlockQuery blockQuery;
    private final WorkService workService;

    public ModerationService(AppUserRepository users, ContentReportRepository reports, UserBlockRepository blocks,
                             PostRepository posts, WorkRepository works, CommentRepository comments,
                             BlockQuery blockQuery, WorkService workService) {
        this.users = users;
        this.reports = reports;
        this.blocks = blocks;
        this.posts = posts;
        this.works = works;
        this.comments = comments;
        this.blockQuery = blockQuery;
        this.workService = workService;
    }

    @Transactional
    public ReportView report(ReportRequest request, Long reporterId) {
        AppUser reporter = user(reporterId);
        requireVisibleTarget(request.targetType(), request.targetId(), reporterId);
        if (reports.findByReporterIdAndTargetTypeAndTargetIdAndStatus(
                reporterId, request.targetType(), request.targetId(), ContentReport.Status.OPEN).isPresent()) {
            throw new InvalidStateException("이미 신고한 대상입니다.");
        }
        String detail = request.detail() == null || request.detail().isBlank() ? null : request.detail().strip();
        ContentReport saved = reports.save(new ContentReport(reporter, request.targetType(), request.targetId(), request.reason(), detail));
        return ReportView.from(saved);
    }

    public List<ReportView> openReports(Long userId) {
        requireAdmin(userId);
        return reports.findByStatusOrderByCreatedAtDesc(ContentReport.Status.OPEN).stream().map(ReportView::from).toList();
    }

    @Transactional
    public ReportView dismiss(Long reportId, Long userId) {
        AppUser admin = requireAdmin(userId);
        ContentReport report = open(reportId);
        report.resolve(ContentReport.Status.DISMISSED, admin);
        return ReportView.from(report);
    }

    /** 사용자 신고는 계정을 지우지 않습니다. 글·작품·댓글만 공개 목록에서 뺍니다. */
    @Transactional
    public ReportView hide(Long reportId, Long userId) {
        AppUser admin = requireAdmin(userId);
        ContentReport report = open(reportId);
        switch (report.getTargetType()) {
            case POST -> hidePost(report.getTargetId());
            case WORK -> hideWork(report.getTargetId());
            case COMMENT -> hideComment(report.getTargetId());
            case USER -> throw new InvalidStateException("사용자 신고는 숨기지 않습니다. 글이나 작품을 숨기세요.");
        }
        report.resolve(ContentReport.Status.HIDDEN, admin);
        return ReportView.from(report);
    }

    @Transactional
    public void block(BlockRequest request, Long userId) {
        if (userId.equals(request.userId())) {
            throw new InvalidStateException("자신을 차단할 수 없습니다.");
        }
        user(request.userId());
        if (!blocks.existsById(new UserBlockKey(userId, request.userId()))) {
            blocks.save(new UserBlock(userId, request.userId()));
        }
    }

    @Transactional
    public void unblock(Long blockedId, Long userId) {
        blocks.deleteById(new UserBlockKey(userId, blockedId));
    }

    private void requireVisibleTarget(ContentReport.TargetType type, Long targetId, Long reporterId) {
        switch (type) {
            case POST -> {
                Post post = posts.findWithAuthor(targetId).orElseThrow(() -> missing("게시글"));
                if (post.getHiddenAt() != null || blockQuery.separated(reporterId, post.getAuthor().getId())) {
                    throw missing("게시글");
                }
                if (post.getAuthor().getId().equals(reporterId)) {
                    throw new InvalidStateException("자신의 글은 신고할 수 없습니다.");
                }
            }
            case WORK -> {
                Work work = workService.loadVisible(targetId, reporterId);
                if (work.getAuthor().getId().equals(reporterId)) {
                    throw new InvalidStateException("자신의 작품은 신고할 수 없습니다.");
                }
            }
            case COMMENT -> {
                Comment comment = comments.findWithAuthor(targetId).orElseThrow(() -> missing("댓글"));
                if (comment.getHiddenAt() != null || blockQuery.separated(reporterId, comment.getAuthor().getId())) {
                    throw missing("댓글");
                }
                if (comment.getAuthor().getId().equals(reporterId)) {
                    throw new InvalidStateException("자신의 댓글은 신고할 수 없습니다.");
                }
            }
            case USER -> {
                if (reporterId.equals(targetId)) {
                    throw new InvalidStateException("자신을 신고할 수 없습니다.");
                }
                user(targetId);
            }
        }
    }

    private void hidePost(Long postId) {
        Post post = posts.findWithAuthor(postId).orElseThrow(() -> missing("게시글"));
        post.hide();
    }

    private void hideWork(Long workId) {
        Work work = works.findWithAuthor(workId).orElseThrow(() -> missing("작품"));
        work.hide();
    }

    private void hideComment(Long commentId) {
        Comment comment = comments.findWithAuthor(commentId).orElseThrow(() -> missing("댓글"));
        comment.hide();
        if (comment.getWorkId() != null) {
            works.setCommentCount(comment.getWorkId(), comments.countByWorkIdAndHiddenAtIsNull(comment.getWorkId()));
        }
        if (comment.getPostId() != null) {
            posts.setCommentCount(comment.getPostId(), comments.countByPostIdAndHiddenAtIsNull(comment.getPostId()));
        }
    }

    private ContentReport open(Long reportId) {
        ContentReport report = reports.findById(Objects.requireNonNull(reportId))
                .orElseThrow(() -> new NotFoundException("신고를 찾을 수 없습니다."));
        if (report.getStatus() != ContentReport.Status.OPEN) {
            throw new InvalidStateException("이미 처리한 신고입니다.");
        }
        return report;
    }

    private AppUser requireAdmin(Long userId) {
        AppUser user = user(userId);
        if (user.getRole() != Role.ADMIN) {
            throw new NotFoundException("요청을 찾을 수 없습니다.");
        }
        return user;
    }

    private AppUser user(Long userId) {
        return users.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }

    private static NotFoundException missing(String name) {
        return new NotFoundException(name + "을 찾을 수 없습니다.");
    }
}
