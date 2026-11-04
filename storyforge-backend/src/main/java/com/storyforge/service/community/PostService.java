package com.storyforge.service.community;

import com.storyforge.domain.activity.ActivityType;
import com.storyforge.domain.community.CommunityDtos.*;
import com.storyforge.domain.community.Post;
import com.storyforge.domain.community.PostRepository;
import com.storyforge.domain.user.AppUser;
import com.storyforge.domain.user.AppUserRepository;
import com.storyforge.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 창작 라운지 게시글 */
@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository posts;
    private final AppUserRepository users;
    private final ActivityService activity;
    private final JdbcTemplate jdbc;
    private final BlockQuery blocks;
    private final ContentPolicy contentPolicy;

    public PostService(PostRepository posts, AppUserRepository users, ActivityService activity, JdbcTemplate jdbc,
                       BlockQuery blocks, ContentPolicy contentPolicy) {
        this.posts = posts;
        this.users = users;
        this.activity = activity;
        this.jdbc = jdbc;
        this.blocks = blocks;
        this.contentPolicy = contentPolicy;
    }

    public PageResponse<PostCard> list(Post.Category category, String q, int page, int size, Long viewerId) {
        Set<Post.Category> categories = category == null ? EnumSet.allOf(Post.Category.class) : EnumSet.of(category);
        Collection<Long> excluded = blocks.excludedAuthorIds(viewerId);
        Page<Post> result = posts.search(categories, WorkService.likePattern(q), excluded.isEmpty(),
                excluded.isEmpty() ? List.of(-1L) : excluded,
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50), Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResponse<>(result.stream().map(PostCard::from).toList(), result.getTotalElements(),
                result.getNumber(), result.getSize());
    }

    public Post load(Long postId) {
        return posts.findWithAuthor(Objects.requireNonNull(postId))
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다: " + postId));
    }

    @Transactional
    public PostDetail detail(Long postId, Long viewerId) {
        Post post = load(postId);
        if ((post.getHiddenAt() != null && !post.getAuthor().getId().equals(viewerId))
                || blocks.separated(viewerId, post.getAuthor().getId())) {
            throw new NotFoundException("게시글을 찾을 수 없습니다: " + postId);
        }
        boolean owned = post.getAuthor().getId().equals(viewerId);
        if (!owned) {
            posts.addView(postId);
        }
        boolean liked = viewerId != null && Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM post_like WHERE post_id = ? AND user_id = ?)", Boolean.class, postId, viewerId));
        return new PostDetail(PostCard.from(post), post.getContent(), owned, liked, post.getUpdatedAt());
    }

    @Transactional
    public PostCard create(PostRequest request, Long userId) {
        AppUser author = users.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        contentPolicy.check(request.title(), request.content());
        Post post = posts.save(new Post(author, request.category(), request.title().strip(), request.content().strip()));
        activity.recordPost(userId, post, ActivityType.POST_WRITTEN, "라운지에 글 작성");
        return PostCard.from(post);
    }

    @Transactional
    public PostCard update(Long postId, PostRequest request, Long userId) {
        Post post = loadOwned(postId, userId);
        contentPolicy.check(request.title(), request.content());
        post.update(request.category(), request.title().strip(), request.content().strip());
        return PostCard.from(post);
    }

    @Transactional
    public void delete(Long postId, Long userId) {
        posts.delete(Objects.requireNonNull(loadOwned(postId, userId)));
    }

    private Post loadOwned(Long postId, Long userId) {
        Post post = load(postId);
        if (!post.getAuthor().getId().equals(userId)) {
            throw new NotFoundException("게시글을 찾을 수 없습니다: " + postId);
        }
        return post;
    }
}
