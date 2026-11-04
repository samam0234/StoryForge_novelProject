package com.storyforge.domain.community;

import com.storyforge.domain.user.AppUser;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.Instant;

/** 창작 라운지 일반 게시글 */
@Entity
@Table(name = "post")
@DynamicUpdate
public class Post {

    public enum Category { FREE, TIP, QUESTION, RECRUIT, SHOWCASE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false)
    private int likeCount;

    @Column(nullable = false)
    private int commentCount;

    @Column(nullable = false)
    private long viewCount;

    private Instant hiddenAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Post() {
    }

    public Post(AppUser author, Category category, String title, String content) {
        this.author = author;
        this.category = category;
        this.title = title;
        this.content = content;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(Category category, String title, String content) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.updatedAt = Instant.now();
    }

    public void setHidden(boolean hidden) {
        this.hiddenAt = hidden ? Instant.now() : null;
    }

    public boolean isHidden() {
        return hiddenAt != null;
    }

    public Long getId() { return id; }
    public AppUser getAuthor() { return author; }
    public Category getCategory() { return category; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public int getLikeCount() { return likeCount; }
    public int getCommentCount() { return commentCount; }
    public long getViewCount() { return viewCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
