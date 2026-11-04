package com.storyforge.controller;

import com.storyforge.domain.community.CommunityDtos.*;
import com.storyforge.domain.community.Post;
import com.storyforge.service.auth.CurrentUser;
import com.storyforge.service.community.EngagementService;
import com.storyforge.service.community.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 창작 라운지 게시글 */
@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService posts;
    private final EngagementService engagement;
    private final CurrentUser currentUser;

    public PostController(PostService posts, EngagementService engagement, CurrentUser currentUser) {
        this.posts = posts;
        this.engagement = engagement;
        this.currentUser = currentUser;
    }

    @GetMapping
    public PageResponse<PostCard> list(@RequestParam(required = false) Post.Category category,
                                       @RequestParam(required = false) String q,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return posts.list(category, q, page, size, currentUser.idOrNull());
    }

    @GetMapping("/{postId}")
    public PostDetail detail(@PathVariable Long postId) {
        return posts.detail(postId, currentUser.idOrNull());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostCard create(@Valid @RequestBody PostRequest request) {
        return posts.create(request, currentUser.id());
    }

    @PutMapping("/{postId}")
    public PostCard update(@PathVariable Long postId, @Valid @RequestBody PostRequest request) {
        return posts.update(postId, request, currentUser.id());
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long postId) {
        posts.delete(postId, currentUser.id());
    }

    @PutMapping("/{postId}/like")
    public LikeResponse like(@PathVariable Long postId) {
        return engagement.likePost(postId, currentUser.id());
    }

    @DeleteMapping("/{postId}/like")
    public LikeResponse unlike(@PathVariable Long postId) {
        return engagement.unlikePost(postId, currentUser.id());
    }

    @GetMapping("/{postId}/comments")
    public List<CommentView> comments(@PathVariable Long postId) {
        return engagement.postComments(postId, currentUser.idOrNull());
    }

    @PostMapping("/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentView comment(@PathVariable Long postId, @Valid @RequestBody CommentRequest request) {
        return engagement.commentOnPost(postId, request, currentUser.id());
    }
}
