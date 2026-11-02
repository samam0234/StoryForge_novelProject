package com.storyforge.domain.community;

import jakarta.persistence.*;

import java.time.Instant;

/** 게시본 변경 이력 */
@Entity
@Table(name = "work_revision")
public class WorkRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long workId;

    @Column(nullable = false)
    private int revision;

    private String message;

    @Column(nullable = false)
    private int chapterCount;

    @Column(nullable = false)
    private int charCount;

    @Column(nullable = false)
    private Instant createdAt;

    protected WorkRevision() {
    }

    public WorkRevision(Long workId, int revision, String message, int chapterCount, int charCount) {
        this.workId = workId;
        this.revision = revision;
        this.message = message;
        this.chapterCount = chapterCount;
        this.charCount = charCount;
        this.createdAt = Instant.now();
    }

    public int getRevision() { return revision; }
    public String getMessage() { return message; }
    public int getChapterCount() { return chapterCount; }
    public int getCharCount() { return charCount; }
    public Instant getCreatedAt() { return createdAt; }
}
