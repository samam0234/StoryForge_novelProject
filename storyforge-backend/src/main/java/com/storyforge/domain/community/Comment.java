package com.storyforge.domain.community;

import com.storyforge.domain.user.AppUser;
import jakarta.persistence.*;

import java.time.Instant;

/** 작품 또는 게시글의 댓글. 답글은 한 단계까지 parentId로 잇습니다. */
@Entity
@Table(name = "comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long workId;

    private Long postId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;

    private Long parentId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant hiddenAt;

    protected Comment() {
    }

    public Comment(Long workId, Long postId, AppUser author, Long parentId, String content) {
        this.workId = workId;
        this.postId = postId;
        this.author = author;
        this.parentId = parentId;
        this.content = content;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void edit(String content) {
        this.content = content;
        this.updatedAt = Instant.now();
    }

    public void hide() {
        this.hiddenAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getWorkId() { return workId; }
    public Long getPostId() { return postId; }
    public AppUser getAuthor() { return author; }
    public Long getParentId() { return parentId; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getHiddenAt() { return hiddenAt; }
}
