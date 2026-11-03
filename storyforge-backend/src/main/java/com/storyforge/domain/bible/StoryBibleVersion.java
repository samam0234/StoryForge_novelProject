package com.storyforge.domain.bible;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/** Story Bible 버전 이력 (기획서 §6.1) */
@Entity
@Table(name = "story_bible_version")
public class StoryBibleVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bible_id", nullable = false)
    private StoryBible bible;

    @Column(nullable = false)
    private int version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private BibleDtos.BibleSnapshot snapshot;

    @Column(nullable = false)
    private Instant createdAt;

    protected StoryBibleVersion() {
    }

    public StoryBibleVersion(StoryBible bible, BibleDtos.BibleSnapshot snapshot) {
        this.bible = bible;
        this.version = bible.getVersion();
        this.snapshot = snapshot;
        this.createdAt = Instant.now();
    }

    public int getVersion() { return version; }
    public BibleDtos.BibleSnapshot getSnapshot() { return snapshot; }
    public Instant getCreatedAt() { return createdAt; }
}
