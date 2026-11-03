package com.storyforge.domain.project;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * 참여자끼리 남기는 작업 커밋. 메시지와 인용 구간은 이 프로젝트 안에서만 보입니다.
 */
@Entity
@Table(name = "work_commit")
public class WorkCommit {

    public enum Target { BIBLE, OUTLINE, CHAPTER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long authorId;

    private Long parentId;

    @Column(nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Target target;

    private Integer chapterNumber;

    private Integer startOffset;

    private Integer endOffset;

    @Column(columnDefinition = "text")
    private String quote;

    @Column(nullable = false)
    private Instant createdAt;

    protected WorkCommit() {
    }

    public WorkCommit(Long projectId, Long authorId, Long parentId, String message, Target target,
                      Integer chapterNumber, Integer startOffset, Integer endOffset, String quote) {
        this.projectId = projectId;
        this.authorId = authorId;
        this.parentId = parentId;
        this.message = message;
        this.target = target;
        this.chapterNumber = chapterNumber;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.quote = quote;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public Long getAuthorId() { return authorId; }
    public Long getParentId() { return parentId; }
    public String getMessage() { return message; }
    public Target getTarget() { return target; }
    public Integer getChapterNumber() { return chapterNumber; }
    public Integer getStartOffset() { return startOffset; }
    public Integer getEndOffset() { return endOffset; }
    public String getQuote() { return quote; }
    public Instant getCreatedAt() { return createdAt; }
}
