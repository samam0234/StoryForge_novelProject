package com.storyforge.service.community;

import com.storyforge.domain.activity.ActivityType;
import com.storyforge.domain.community.*;
import com.storyforge.domain.community.CommunityDtos.*;
import com.storyforge.domain.moderation.UserBlockRepository;
import com.storyforge.domain.notification.Notification;
import com.storyforge.domain.user.AppUser;
import com.storyforge.domain.user.AppUserRepository;
import com.storyforge.exception.InvalidStateException;
import com.storyforge.exception.NotFoundException;
import com.storyforge.service.notification.NotificationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 추천과 댓글 */
@Service
@Transactional(readOnly = true)
public class EngagementService {

    private final WorkService workService;
    private final WorkRepository works;
    private final PostService postService;
    private final PostRepository posts;
    private final CommentRepository comments;
    private final AppUserRepository users;
    private final ActivityService activity;
    private final IssueService issues;
    private final JdbcTemplate jdbc;
    private final UserBlockRepository blocks;
    private final NotificationService notifications;

    public EngagementService(WorkService workService, WorkRepository works, PostService postService, PostRepository posts,
                             CommentRepository comments, AppUserRepository users, ActivityService activity,
                             IssueService issues, JdbcTemplate jdbc,
                             UserBlockRepository blocks, NotificationService notifications) {
        this.blocks = blocks;
        this.notifications = notifications;
        this.workService = workService;
        this.works = works;
        this.postService = postService;
        this.posts = posts;
        this.comments = comments;
        this.users = users;
        this.activity = activity;
        this.issues = issues;
        this.jdbc = jdbc;
    }

    // ───────────── 추천 ─────────────

    /** 같은 사용자가 두 번 눌러도 한 번만 셉니다. 자기 작품은 추천할 수 없습니다 (달성 기록 조작 방지). */
    @Transactional
    public LikeResponse likeWork(Long workId, Long userId) {
        Work work = workService.loadVisible(workId, userId);
        if (work.getAuthor().getId().equals(userId)) {
            throw new InvalidStateException("내 작품은 추천할 수 없습니다.");
        }
        int inserted = jdbc.update("INSERT INTO work_like (work_id, user_id, created_at) VALUES (?, ?, now()) ON CONFLICT DO NOTHING",
                workId, userId);
        if (inserted > 0) {
            works.addLikes(workId, 1);
            int total = works.likeCount(workId);
            Long daily = jdbc.queryForObject(
                    "SELECT count(*) FROM work_like WHERE work_id = ? AND created_at > now() - interval '24 hours'", Long.class, workId);
            issues.reached(workId, IssueEvent.Metric.TOTAL_LIKES, total);
            issues.reached(workId, IssueEvent.Metric.DAILY_LIKES, daily == null ? 0 : daily);
            activity.touchAchievements(work.getAuthor().getId());
        }
        return new LikeResponse(true, works.likeCount(workId));
    }

    @Transactional
    public LikeResponse unlikeWork(Long workId, Long userId) {
        workService.loadVisible(workId, userId);
        if (jdbc.update("DELETE FROM work_like WHERE work_id = ? AND user_id = ?", workId, userId) > 0) {
            works.addLikes(workId, -1);
        }
        return new LikeResponse(false, works.likeCount(workId));
    }

    @Transactional
    public LikeResponse likePost(Long postId, Long userId) {
        Post post = postService.loadVisible(postId, userId);
        if (post.getAuthor().getId().equals(userId)) {
            throw new InvalidStateException("내 글은 추천할 수 없습니다.");
        }
        if (jdbc.update("INSERT INTO post_like (post_id, user_id, created_at) VALUES (?, ?, now()) ON CONFLICT DO NOTHING", postId, userId) > 0) {
            posts.addLikes(postId, 1);
        }
        return new LikeResponse(true, posts.likeCount(postId));
    }

    @Transactional
    public LikeResponse unlikePost(Long postId, Long userId) {
        postService.load(postId);
        if (jdbc.update("DELETE FROM post_like WHERE post_id = ? AND user_id = ?", postId, userId) > 0) {
            posts.addLikes(postId, -1);
        }
        return new LikeResponse(false, posts.likeCount(postId));
    }

    // ───────────── 댓글 ─────────────

    public List<CommentView> workComments(Long workId, Long viewerId) {
        Work work = workService.loadVisible(workId, viewerId);
        return visible(comments.findByWorkIdOrderByCreatedAt(workId), viewerId, work.getAuthor().getId());
    }

    public List<CommentView> postComments(Long postId, Long viewerId) {
        Post post = postService.loadVisible(postId, viewerId);
        return visible(comments.findByPostIdOrderByCreatedAt(postId), viewerId, post.getAuthor().getId());
    }

    @Transactional
    public CommentView commentOnWork(Long workId, CommentRequest request, Long userId) {
        Work work = workService.loadVisible(workId, userId);
        requireNotBlocked(work.getAuthor().getId(), userId);
        Comment parent = checkParent(request.parentId(), workId, null);
        Comment saved = comments.save(new Comment(workId, null, user(userId), request.parentId(), request.content().strip()));
        works.setCommentCount(workId, comments.countByWorkId(workId));
        activity.recordWork(userId, null, work, ActivityType.COMMENT_WRITTEN, "작품에 댓글 작성", 0);
        notifyComment(saved, parent, work.getAuthor().getId(), "「" + work.getTitle() + "」", "/works/" + workId);
        return view(saved, userId, work.getAuthor().getId());
    }

    @Transactional
    public CommentView commentOnPost(Long postId, CommentRequest request, Long userId) {
        Post post = postService.loadVisible(postId, userId);
        requireNotBlocked(post.getAuthor().getId(), userId);
        Comment parent = checkParent(request.parentId(), null, postId);
        Comment saved = comments.save(new Comment(null, postId, user(userId), request.parentId(), request.content().strip()));
        posts.setCommentCount(postId, comments.countByPostId(postId));
        activity.recordPost(userId, post, ActivityType.COMMENT_WRITTEN, "게시글에 댓글 작성");
        notifyComment(saved, parent, post.getAuthor().getId(), "「" + post.getTitle() + "」", "/posts/" + postId);
        return view(saved, userId, post.getAuthor().getId());
    }

    /** 댓글 수정은 작성자만. 다른 사용자는 404입니다. */
    @Transactional
    public CommentView editComment(Long commentId, CommentEditRequest request, Long userId) {
        Comment comment = load(commentId);
        if (!comment.getAuthor().getId().equals(userId) || comment.isHidden()) {
            throw new NotFoundException("댓글을 찾을 수 없습니다: " + commentId);
        }
        comment.edit(request.content().strip());
        return view(comment, userId, targetOwner(comment));
    }

    /** 삭제는 작성자 또는 작품·게시글 주인. 답글도 함께 지워집니다. */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = load(commentId);
        if (!comment.getAuthor().getId().equals(userId) && !Objects.equals(targetOwner(comment), userId)) {
            throw new NotFoundException("댓글을 찾을 수 없습니다: " + commentId);
        }
        Long workId = comment.getWorkId();
        Long postId = comment.getPostId();
        comments.delete(comment);
        comments.flush();
        if (workId != null) {
            works.setCommentCount(workId, comments.countByWorkId(workId));
        } else {
            posts.setCommentCount(postId, comments.countByPostId(postId));
        }
    }

    private Comment load(Long commentId) {
        return comments.findWithAuthor(Objects.requireNonNull(commentId))
                .orElseThrow(() -> new NotFoundException("댓글을 찾을 수 없습니다: " + commentId));
    }

    private Long targetOwner(Comment c) {
        if (c.getWorkId() != null) {
            return works.findWithAuthor(c.getWorkId()).map(w -> w.getAuthor().getId()).orElse(null);
        }
        return posts.findWithAuthor(c.getPostId()).map(p -> p.getAuthor().getId()).orElse(null);
    }

    /** 내가 차단한 사람의 댓글은 나에게 보이지 않습니다. 그 답글도 함께 빠집니다. */
    private List<CommentView> visible(List<Comment> list, Long viewerId, Long ownerId) {
        Set<Long> blocked = viewerId == null ? Set.of() : new HashSet<>(blocks.blockedIds(viewerId));
        Set<Long> removed = new HashSet<>();
        List<CommentView> out = new ArrayList<>();
        for (Comment c : list) {
            if (blocked.contains(c.getAuthor().getId()) || (c.getParentId() != null && removed.contains(c.getParentId()))) {
                removed.add(c.getId());
            } else {
                out.add(view(c, viewerId, ownerId));
            }
        }
        return out;
    }

    private void requireNotBlocked(Long ownerId, Long userId) {
        if (blocks.existsByBlockerIdAndBlockedId(ownerId, userId)) {
            throw new InvalidStateException("작성자가 댓글을 받지 않습니다.");
        }
    }

    /** 글 주인과 원댓글 작성자에게 알립니다. 댓글 내용은 넣지 않습니다. */
    private void notifyComment(Comment saved, Comment parent, Long ownerId, String title, String link) {
        Long actor = saved.getAuthor().getId();
        String name = saved.getAuthor().getDisplayName();
        notifications.notify(ownerId, actor, Notification.Type.COMMENT, name + "님이 " + title + "에 댓글을 남겼습니다.", link);
        if (parent != null && !parent.getAuthor().getId().equals(ownerId)) {
            notifications.notify(parent.getAuthor().getId(), actor, Notification.Type.REPLY, name + "님이 내 댓글에 답글을 남겼습니다.", link);
        }
    }

    private Comment checkParent(Long parentId, Long workId, Long postId) {
        if (parentId == null) {
            return null;
        }
        Comment parent = load(parentId);
        if (!Objects.equals(parent.getWorkId(), workId) || !Objects.equals(parent.getPostId(), postId)) {
            throw new NotFoundException("댓글을 찾을 수 없습니다: " + parentId);
        }
        if (parent.getParentId() != null) {
            throw new InvalidStateException("답글에는 다시 답글을 달 수 없습니다.");
        }
        return parent;
    }

    private AppUser user(Long userId) {
        return users.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }

    private static CommentView view(Comment c, Long viewerId, Long ownerId) {
        boolean mine = c.getAuthor().getId().equals(viewerId);
        // 숨긴 댓글은 내용을 내려보내지 않습니다. 답글 흐름이 끊기지 않게 자리만 남깁니다.
        String content = c.isHidden() ? "관리자가 숨긴 댓글입니다." : c.getContent();
        return new CommentView(c.getId(), c.getParentId(), UserSummary.from(c.getAuthor()), content, c.isHidden(),
                mine && !c.isHidden(), !c.isHidden() && (mine || (viewerId != null && viewerId.equals(ownerId))),
                c.getCreatedAt(), c.getUpdatedAt());
    }
}
