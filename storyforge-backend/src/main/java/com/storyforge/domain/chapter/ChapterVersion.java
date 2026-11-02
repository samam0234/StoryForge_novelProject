package com.storyforge.domain.chapter;

import jakarta.persistence.*;

import java.time.Instant;

/** 챕터 생성·수정 이력 (기획서 §6.4 생성 히스토리) */
@Entity
@Table(name = "chapter_version")
public class ChapterVersion {

    public enum Source { GENERATED, EDITED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(nullable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source;

    private String title;

    @Column(columnDefinition = "text")
    private String content;

    @Column(nullable = false)
    private Instant createdAt;

    protected ChapterVersion() {
    }

    public ChapterVersion(Chapter chapter, int version, Source source) {
        this.chapter = chapter;
        this.version = version;
        this.source = source;
        this.title = chapter.getTitle();
        this.content = chapter.getContent();
        this.createdAt = Instant.now();
    }

    public int getVersion() { return version; }
    public Source getSource() { return source; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
