package com.storyforge.service.community;

import com.storyforge.domain.activity.ActivityType;
import com.storyforge.domain.community.*;
import com.storyforge.domain.community.CommunityDtos.*;
import com.storyforge.domain.user.AppUser;
import com.storyforge.domain.user.AppUserRepository;
import com.storyforge.exception.InvalidStateException;
import com.storyforge.exception.NotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

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

    public EngagementService(WorkService workService, WorkRepository works, PostService postService, PostRepository posts,
                             CommentRepository comments, AppUserRepository users, ActivityService activity,
                             IssueService issues, JdbcTemplate jdbc) {
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
        Post post = postService.load(postId);
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
        return comments.findByWorkIdOrderByCreatedAt(workId).stream()
                .map(c -> view(c, viewerId, work.getAuthor().getId()))
                .toList();
    }

    public List<CommentView> postComments(Long postId, Long viewerId) {
        Post post = postService.load(postId);
        return comments.findByPostIdOrderByCreatedAt(postId).stream()
                .map(c -> view(c, viewerId, post.getAuthor().getId()))
                .toList();
    }

    @Transactional
    public CommentView commentOnWork(Long workId, CommentRequest request, Long userId) {
        Work work = workService.loadVisible(workId, userId);
        checkParent(request.parentId(), workId, null);
        Comment saved = comments.save(new Comment(workId, null, user(userId), request.parentId(), request.content().strip()));
        works.setCommentCount(workId, comments.countByWorkId(workId));
        activity.recordWork(userId, null, work, ActivityType.COMMENT_WRITTEN, "작품에 댓글 작성", 0);
        return view(saved, userId, work.getAuthor().getId());
    }

    @Transactional
    public CommentView commentOnPost(Long postId, CommentRequest request, Long userId) {
        Post post = postService.load(postId);
        checkParent(request.parentId(), null, postId);
        Comment saved = comments.save(new Comment(null, postId, user(userId), request.parentId(), request.content().strip()));
        posts.setCommentCount(postId, comments.countByPostId(postId));
        activity.recordPost(userId, post, ActivityType.COMMENT_WRITTEN, "게시글에 댓글 작성");
        return view(saved, userId, post.getAuthor().getId());
    }

    /** 댓글 수정은 작성자만. 다른 사용자는 404입니다. */
    @Transactional
    public CommentView editComment(Long commentId, CommentEditRequest request, Long userId) {
        Comment comment = load(commentId);
        if (!comment.getAuthor().getId().equals(userId)) {
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

    private void checkParent(Long parentId, Long workId, Long postId) {
        if (parentId == null) {
            return;
        }
        Comment parent = load(parentId);
        if (!Objects.equals(parent.getWorkId(), workId) || !Objects.equals(parent.getPostId(), postId)) {
            throw new NotFoundException("댓글을 찾을 수 없습니다: " + parentId);
        }
        if (parent.getParentId() != null) {
            throw new InvalidStateException("답글에는 다시 답글을 달 수 없습니다.");
        }
    }

    private AppUser user(Long userId) {
        return users.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }

    private static CommentView view(Comment c, Long viewerId, Long ownerId) {
        boolean mine = c.getAuthor().getId().equals(viewerId);
        return new CommentView(c.getId(), c.getParentId(), UserSummary.from(c.getAuthor()), c.getContent(), mine,
                mine || (viewerId != null && viewerId.equals(ownerId)), c.getCreatedAt(), c.getUpdatedAt());
    }
}
