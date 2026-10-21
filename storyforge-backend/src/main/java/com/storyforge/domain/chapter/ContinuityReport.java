package com.storyforge.domain.chapter;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "continuity_report")
public class ContinuityReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<ContinuityIssue> issues;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private Instant createdAt;

    protected ContinuityReport() {
    }

    public ContinuityReport(Chapter chapter, List<ContinuityIssue> issues, int score) {
        this.chapter = chapter;
        this.issues = issues;
        this.score = score;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Chapter getChapter() { return chapter; }
    public List<ContinuityIssue> getIssues() { return issues; }
    public int getScore() { return score; }
    public Instant getCreatedAt() { return createdAt; }
}
